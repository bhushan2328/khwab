package com.toblad.khwab.overlay

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import com.toblad.khwab.service.VoiceService
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

    // Semi-transparent dismiss zone shown at the bottom during drag.
    private var dismissZoneView: View? = null
    private var dismissZoneParams: WindowManager.LayoutParams? = null
    private var dismissZoneVisible = false

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

        val screenWidth = screenWidth()
        val screenHeight = screenHeight()

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
            x = (screenWidth * 0.80).toInt()
            y = (screenHeight * 0.30).toInt()
        }

        try {
            windowManager.addView(floatingView, layoutParams)
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindow", "Failed to add floating view", e)
            floatingView = null
            micButton = null
            layoutParams = null
            return
        }
        attachDragListener()
    }

    fun hide() {
        hideDismissZone()
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
                val anim = AnimatorInflater
                    .loadAnimator(context, R.animator.mic_pulse) as AnimatorSet
                anim.setTarget(target)
                anim.start()
                activeAnimator = anim
            }
            AssistantState.THINKING -> {
                val anim = AnimatorInflater
                    .loadAnimator(context, R.animator.mic_spin)
                anim.setTarget(target)
                anim.start()
                activeSingleAnimator = anim
            }
            AssistantState.EXECUTING -> {
                val anim = AnimatorInflater
                    .loadAnimator(context, R.animator.mic_bounce) as AnimatorSet
                anim.setTarget(target)
                anim.start()
                activeAnimator = anim
            }
            AssistantState.SPEAKING -> {
                val anim = AnimatorInflater
                    .loadAnimator(context, R.animator.mic_wiggle) as AnimatorSet
                anim.setTarget(target)
                anim.start()
                activeAnimator = anim
            }
            AssistantState.ERROR -> {
                val anim = AnimatorInflater
                    .loadAnimator(context, R.animator.mic_shake) as AnimatorSet
                anim.setTarget(target)
                anim.start()
                activeAnimator = anim
                mainHandler.postDelayed({ setState(AssistantState.READY) }, 700)
            }
            else -> { /* STOPPED / READY — no animation */ }
        }
    }

    private fun stopAnimation() {
        activeAnimator?.cancel()
        activeAnimator = null
        activeSingleAnimator?.cancel()
        activeSingleAnimator = null
    }

    // ── drag + edge-snap + dismiss zone ───────────────────────────────────
    @SuppressLint("ClickableViewAccessibility")
    private fun attachDragListener() {
        // Attach to micButton (the child ImageView), not the FrameLayout wrapper.
        // The FrameLayout is WRAP_CONTENT so its hit area equals the child; attaching
        // to the parent would silently drop all events because the child was previously
        // marked clickable=true and consumed them first. The child is now
        // clickable=false in XML so every touch reaches this listener directly.
        val btn  = micButton  ?: return
        val root = floatingView ?: return
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        btn.setOnTouchListener { _, event ->
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
                    if (!isDragging && (abs(dx) > 8 || abs(dy) > 8)) {
                        isDragging = true
                        showDismissZone()
                    }
                    if (isDragging) {
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager.updateViewLayout(root, params)

                        // Highlight dismiss zone when button is dragged near the bottom.
                        val nearBottom = event.rawY > screenHeight() * 0.80f
                        dismissZoneView?.alpha = if (nearBottom) 0.85f else 0.45f
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    hideDismissZone()
                    if (isDragging) {
                        // If released over the bottom dismiss zone → stop service
                        if (event.rawY > screenHeight() * 0.82f) {
                            context.stopService(Intent(context, VoiceService::class.java))
                        } else {
                            snapToEdge(root, params)
                        }
                    } else {
                        onMicTap?.invoke()
                    }
                    true
                }
                else -> false
            }
        }
    }

    // ── dismiss zone overlay ───────────────────────────────────────────────

    private fun showDismissZone() {
        if (dismissZoneVisible) return
        dismissZoneVisible = true

        val zoneView = View(context).apply {
            setBackgroundColor(0xBB_CC0000.toInt())  // translucent red strip
            alpha = 0.45f
        }

        val height = (screenHeight() * 0.12f).toInt()
        val zoneParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            height,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
        }

        dismissZoneView   = zoneView
        dismissZoneParams = zoneParams
        windowManager.addView(zoneView, zoneParams)
    }

    private fun hideDismissZone() {
        dismissZoneView?.let {
            runCatching { windowManager.removeView(it) }
            dismissZoneView   = null
            dismissZoneParams = null
            dismissZoneVisible = false
        }
    }

    // ── edge snap ─────────────────────────────────────────────────────────
    private fun snapToEdge(view: View, params: WindowManager.LayoutParams) {
        val sw = screenWidth()
        val buttonWidth = view.width.takeIf { it > 0 } ?: 164
        val margin = 24
        val targetX = if (params.x + buttonWidth / 2 < sw / 2) margin
                      else sw - buttonWidth - margin

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

    // ── helpers ───────────────────────────────────────────────────────────
    private fun screenWidth() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        windowManager.currentWindowMetrics.bounds.width()
    else context.resources.displayMetrics.widthPixels

    private fun screenHeight() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        windowManager.currentWindowMetrics.bounds.height()
    else context.resources.displayMetrics.heightPixels
}
