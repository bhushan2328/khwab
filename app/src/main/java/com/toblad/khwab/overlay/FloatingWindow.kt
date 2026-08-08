package com.toblad.khwab.overlay

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.toblad.khwab.R
import com.toblad.khwab.state.AssistantState
import kotlin.math.abs

class FloatingWindow(
    private val context: Context,
    private val onMicTap: (() -> Unit)? = null
) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var floatingView: View? = null
    private var micButton: ImageView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var activeAnimator: AnimatorSet? = null
    private var activeSingleAnimator: android.animation.Animator? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    // ── background resource per state ──────────────────────────────────────
    private fun backgroundFor(state: AssistantState): Int = when (state) {
        AssistantState.LISTENING  -> R.drawable.bg_mic_listening
        AssistantState.THINKING   -> R.drawable.bg_mic_thinking
        AssistantState.EXECUTING  -> R.drawable.bg_mic_executing
        AssistantState.SPEAKING   -> R.drawable.bg_mic_speaking
        AssistantState.ERROR      -> R.drawable.bg_mic_error
        else                      -> R.drawable.bg_mic_idle
    }

    // ── show / hide ────────────────────────────────────────────────────────
    fun show() {
        if (floatingView != null) return

        floatingView = LayoutInflater.from(context).inflate(R.layout.floating_mic, null)
        micButton = floatingView!!.findViewById(R.id.micButton)

        val screenWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.width()
        } else {
            context.resources.displayMetrics.widthPixels
        }
        val screenHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.height()
        } else {
            context.resources.displayMetrics.heightPixels
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // Density-aware starting position: upper-right zone
            x = (screenWidth * 0.80).toInt()
            y = (screenHeight * 0.30).toInt()
        }

        attachDragListener()
        windowManager.addView(floatingView, layoutParams)
    }

    fun hide() {
        stopAnimation()
        floatingView?.let {
            windowManager.removeView(it)
            floatingView = null
            micButton = null
            layoutParams = null
        }
    }

    // ── public state API ───────────────────────────────────────────────────
    fun setState(state: AssistantState) {
        mainHandler.post {
            val btn = micButton ?: return@post
            stopAnimation()
            btn.setBackgroundResource(backgroundFor(state))
            // reset transforms left over from prior animations
            btn.translationX = 0f
            btn.translationY = 0f
            btn.rotation    = 0f
            btn.scaleX      = 1f
            btn.scaleY      = 1f
            startAnimationFor(state, btn)
        }
    }

    // ── per-state animation ────────────────────────────────────────────────
    private fun startAnimationFor(state: AssistantState, target: View) {
        when (state) {
            AssistantState.LISTENING -> {
                // gentle pulse — scale in/out
                val anim = AnimatorInflater
                    .loadAnimator(context, R.animator.mic_pulse) as AnimatorSet
                anim.setTarget(target)
                anim.start()
                activeAnimator = anim
            }
            AssistantState.THINKING -> {
                // continuous rotation
                val anim = AnimatorInflater
                    .loadAnimator(context, R.animator.mic_spin)
                anim.setTarget(target)
                anim.start()
                activeSingleAnimator = anim
            }
            AssistantState.EXECUTING -> {
                // bounce up/down
                val anim = AnimatorInflater
                    .loadAnimator(context, R.animator.mic_bounce) as AnimatorSet
                anim.setTarget(target)
                anim.start()
                activeAnimator = anim
            }
            AssistantState.SPEAKING -> {
                // fast side-to-side wiggle
                val anim = AnimatorInflater
                    .loadAnimator(context, R.animator.mic_wiggle) as AnimatorSet
                anim.setTarget(target)
                anim.start()
                activeAnimator = anim
            }
            AssistantState.ERROR -> {
                // one-shot hard shake, then back to idle
                val anim = AnimatorInflater
                    .loadAnimator(context, R.animator.mic_shake) as AnimatorSet
                anim.setTarget(target)
                anim.start()
                activeAnimator = anim
                // after shake completes (~600 ms) revert to idle automatically
                mainHandler.postDelayed({ setState(AssistantState.READY) }, 700)
            }
            else -> { /* STOPPED / READY — no animation, just idle colour */ }
        }
    }

    private fun stopAnimation() {
        activeAnimator?.cancel()
        activeAnimator = null
        activeSingleAnimator?.cancel()
        activeSingleAnimator = null
    }

    // ── drag + edge-snap ───────────────────────────────────────────────────
    @SuppressLint("ClickableViewAccessibility")
    private fun attachDragListener() {
        val view = floatingView ?: return
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        view.setOnTouchListener { v, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX      = params.x
                    initialY      = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging    = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (!isDragging && (abs(dx) > 8 || abs(dy) > 8)) isDragging = true
                    if (isDragging) {
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        snapToEdge(view, params)
                    } else {
                        v.performClick()
                        onMicTap?.invoke()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun snapToEdge(view: View, params: WindowManager.LayoutParams) {
        val screenWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.width()
        } else {
            context.resources.displayMetrics.widthPixels
        }

        val buttonWidth = view.width.takeIf { it > 0 } ?: 164  // 64dp approx
        val margin = 24
        val targetX = if (params.x + buttonWidth / 2 < screenWidth / 2) margin
                      else screenWidth - buttonWidth - margin

        // fix #12: decelerate easing + longer duration for natural snap feel
        ValueAnimator.ofInt(params.x, targetX).apply {
            duration = 320
            interpolator = android.view.animation.DecelerateInterpolator(2f)
            addUpdateListener { anim ->
                params.x = anim.animatedValue as Int
                runCatching { windowManager.updateViewLayout(view, params) }
            }
            start()
        }
    }
}
