package com.toblad.khwab.speech

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toblad.khwab.ui.theme.KhwabBlue
import com.toblad.khwab.ui.theme.KhwabGreen
import com.toblad.khwab.ui.theme.KhwabViolet
import com.toblad.khwab.ui.theme.KhwabYellow

/**
 * Full-screen download screen shown on first launch when the Whisper
 * model files are not yet present on-device.
 *
 * [onReady] is called when the models are available (either already
 * downloaded or just completed) and the app can proceed normally.
 */
@Composable
fun ModelDownloadScreen(
    onReady: () -> Unit,
    viewModel: ModelDownloadViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state is ModelDownloadState.Ready || state is ModelDownloadState.Completed) {
            onReady()
        }
    }

    LaunchedEffect(Unit) {
        if (state is ModelDownloadState.Idle) {
            viewModel.startDownload()
        }
    }

    val colors = MaterialTheme.colorScheme

    // ── Infinite transition driving ALL logo animations ──────────────────────
    val anim = rememberInfiniteTransition(label = "logo_anim")

    // Bounce: translateY offset — goes up then comes back with overshoot feel
    val bounceY by anim.animateFloat(
        initialValue = 0f,
        targetValue = -36f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0f        at 0    using FastOutSlowInEasing
                -36f      at 400  using FastOutSlowInEasing
                -36f      at 500  using LinearEasing
                0f        at 900  using FastOutSlowInEasing
                0f        at 1200 using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "bounce_y"
    )

    // Pop scale: squish on land, stretch on launch
    val popScale by anim.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                1.0f  at 0    using FastOutSlowInEasing   // resting
                0.85f at 100  using FastOutSlowInEasing   // squish before launch
                1.2f  at 400  using FastOutSlowInEasing   // stretch at peak
                1.0f  at 700  using FastOutSlowInEasing   // normal mid-air
                0.8f  at 900  using FastOutSlowInEasing   // squish on land
                1.05f at 1000 using FastOutSlowInEasing   // small bounce back
                1.0f  at 1200 using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "pop_scale"
    )

    // Horizontal squash (inverse of vertical for 3D feel)
    val scaleX by anim.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                1.0f  at 0
                1.15f at 100    // wider when squished down
                0.9f  at 400    // narrower when stretched up
                1.0f  at 700
                1.2f  at 900    // wide splat on land
                0.95f at 1000
                1.0f  at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "scale_x"
    )

    // Tilt left/right (subtle wiggle in the air)
    val tiltDeg by anim.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0f   at 0
                -8f  at 300
                8f   at 600
                -4f  at 800
                0f   at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "tilt"
    )

    // Color shift cycling through brand palette
    val colorCycle by anim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "color_cycle"
    )

    // Shadow scale — small when logo is up high, big when on ground
    val shadowScale by anim.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                1.0f  at 0
                0.5f  at 100
                0.3f  at 400
                0.3f  at 500
                0.8f  at 900
                1.0f  at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "shadow_scale"
    )

    // Shadow alpha
    val shadowAlpha by anim.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.4f at 0
                0.15f at 400
                0.15f at 500
                0.4f at 900
                0.4f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "shadow_alpha"
    )

    // Particle phase 0→1 drives all 8 particles (each offset by 1/8 phase)
    val particlePhase by anim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_phase"
    )

    // Shimmer sweep: 0→1 drives a highlight stripe across the progress bar
    val shimmerOffset by anim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    // Interpolate brand color from cycle value
    val logoColor = lerpBrandColor(colorCycle)

    // Background glow pulse: alpha oscillates in sync with bounce
    val bgGlowAlpha by anim.animateFloat(
        initialValue = 0.25f,
        targetValue  = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_glow"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // ── Full-screen radial glow layer ─────────────────────────────
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height * 0.28f   // upper third, behind the logo
                val radius = size.width * 0.75f
                drawCircle(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            logoColor.copy(alpha = bgGlowAlpha),
                            logoColor.copy(alpha = bgGlowAlpha * 0.3f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = radius
                    ),
                    radius = radius,
                    center = Offset(cx, cy)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {

                // ── Animated 3D Logo + Particles ─────────────────────────────
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier.size(width = 160.dp, height = 180.dp)
                ) {
                    // Floating particles layer (behind the K)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawFloatingParticles(
                            particlePhase = particlePhase,
                            accentColor   = logoColor
                        )
                    }

                    // Ground shadow ellipse
                    Canvas(
                        modifier = Modifier
                            .size(width = 100.dp, height = 20.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        drawOval(
                            color = logoColor.copy(alpha = shadowAlpha),
                            topLeft = Offset(
                                x = center.x - (size.width / 2f) * shadowScale,
                                y = center.y - (size.height / 2f) * 0.5f
                            ),
                            size = androidx.compose.ui.geometry.Size(
                                width = size.width * shadowScale,
                                height = size.height * 0.5f
                            )
                        )
                    }

                    // The 3D "K" letter on Canvas
                    Canvas(
                        modifier = Modifier
                            .size(100.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        translate(top = bounceY) {
                            scale(scaleX = scaleX, scaleY = popScale) {
                                drawAnimatedK(
                                    color = logoColor,
                                    tiltDeg = tiltDeg
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Khwab",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.onBackground
                )

                Text(
                    text = "Your intelligent voice companion",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Setting up Khwab",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground
                )

                when (val s = state) {

                    is ModelDownloadState.Idle,
                    is ModelDownloadState.Downloading -> {
                        val percent = (s as? ModelDownloadState.Downloading)?.percent ?: 0
                        val fileName = (s as? ModelDownloadState.Downloading)?.currentFile ?: ""

                        // Smoothly ticking integer counter
                        val displayPercent by animateIntAsState(
                            targetValue = percent,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                            label = "pct_counter"
                        )

                        Text(
                            text = "Downloading speech recognition model…\nThis happens once and requires Wi-Fi.",
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Stylish glowing gradient bar with shimmer
                        GlowProgressBar(
                            progress  = percent / 100f,
                            shimmer   = shimmerOffset,
                            modifier  = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                        )

                        // Animated percentage counter
                        Text(
                            text = "$displayPercent%",
                            color = logoColor,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        // File name fades in when it becomes available
                        AnimatedVisibility(
                            visible = fileName.isNotBlank(),
                            enter   = fadeIn(animationSpec = tween(durationMillis = 400))
                        ) {
                            Text(
                                text = fileName,
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    is ModelDownloadState.Failed -> {
                        Text(
                            text = "Download failed",
                            color = colors.error,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = s.message,
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = viewModel::retry,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Retry")
                        }
                    }

                    is ModelDownloadState.Ready,
                    is ModelDownloadState.Completed -> {
                        Text(
                            text = "Ready!",
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
            } // inner padding Box
        }
    }
}

// ── Draw a bold 3D "K" using Canvas paths ────────────────────────────────────
private fun DrawScope.drawAnimatedK(color: Color, tiltDeg: Float) {
    val w = size.width
    val h = size.height
    val stroke = w * 0.18f

    // 3D depth layer (offset slightly bottom-right, darker)
    val depthColor = color.copy(
        red   = (color.red   * 0.5f).coerceIn(0f, 1f),
        green = (color.green * 0.5f).coerceIn(0f, 1f),
        blue  = (color.blue  * 0.5f).coerceIn(0f, 1f),
        alpha = 0.7f
    )

    // Shadow/depth layer offset
    val depthOffset = w * 0.05f

    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            isAntiAlias = true
        }
        val frameworkPaint = paint.asFrameworkPaint()

        // ── Glow / bloom effect ──────────────────────────────────────────────
        frameworkPaint.color = android.graphics.Color.TRANSPARENT
        frameworkPaint.setShadowLayer(w * 0.15f, 0f, 0f, color.copy(alpha = 0.8f).toArgb())

        // Draw vertical bar of K (depth layer)
        frameworkPaint.color = depthColor.toArgb()
        frameworkPaint.setShadowLayer(0f, 0f, 0f, android.graphics.Color.TRANSPARENT)
        canvas.drawRect(
            left   = w * 0.10f + depthOffset,
            top    = h * 0.08f + depthOffset,
            right  = w * 0.10f + stroke + depthOffset,
            bottom = h * 0.92f + depthOffset,
            paint  = paint
        )
        // Upper arm depth
        canvas.drawRect(
            left   = w * 0.28f + depthOffset,
            top    = h * 0.08f + depthOffset,
            right  = w * 0.90f + depthOffset,
            bottom = h * 0.08f + stroke + depthOffset,
            paint  = paint
        )
        // Lower arm depth
        canvas.drawRect(
            left   = w * 0.28f + depthOffset,
            top    = h * 0.92f - stroke + depthOffset,
            right  = w * 0.90f + depthOffset,
            bottom = h * 0.92f + depthOffset,
            paint  = paint
        )
        // Center diagonal connector depth
        canvas.drawRect(
            left   = w * 0.28f + depthOffset,
            top    = h * 0.44f + depthOffset,
            right  = w * 0.28f + stroke + depthOffset,
            bottom = h * 0.56f + depthOffset,
            paint  = paint
        )

        // ── Main bright K ────────────────────────────────────────────────────
        frameworkPaint.color = color.toArgb()
        frameworkPaint.setShadowLayer(w * 0.12f, 0f, 0f, color.copy(alpha = 0.9f).toArgb())

        // Vertical bar
        canvas.drawRect(
            left   = w * 0.10f,
            top    = h * 0.08f,
            right  = w * 0.10f + stroke,
            bottom = h * 0.92f,
            paint  = paint
        )
        // Upper arm
        canvas.drawRect(
            left   = w * 0.28f,
            top    = h * 0.08f,
            right  = w * 0.90f,
            bottom = h * 0.08f + stroke,
            paint  = paint
        )
        // Lower arm
        canvas.drawRect(
            left   = w * 0.28f,
            top    = h * 0.92f - stroke,
            right  = w * 0.90f,
            bottom = h * 0.92f,
            paint  = paint
        )
        // Center diagonal connector
        canvas.drawRect(
            left   = w * 0.28f,
            top    = h * 0.44f,
            right  = w * 0.28f + stroke,
            bottom = h * 0.56f,
            paint  = paint
        )

        // ── Highlight stripe (top-left bright edge for 3D illusion) ─────────
        frameworkPaint.color = Color.White.copy(alpha = 0.35f).toArgb()
        frameworkPaint.setShadowLayer(0f, 0f, 0f, android.graphics.Color.TRANSPARENT)

        // Highlight on vertical bar left edge
        canvas.drawRect(
            left   = w * 0.10f,
            top    = h * 0.08f,
            right  = w * 0.10f + stroke * 0.25f,
            bottom = h * 0.50f,
            paint  = paint
        )
        // Highlight on upper arm top edge
        canvas.drawRect(
            left   = w * 0.28f,
            top    = h * 0.08f,
            right  = w * 0.65f,
            bottom = h * 0.08f + stroke * 0.25f,
            paint  = paint
        )
    }
}

// ── 8 particles drifting upward around the logo ──────────────────────────────
// particlePhase: 0→1 looping, each particle is staggered by 1/8th
private fun DrawScope.drawFloatingParticles(particlePhase: Float, accentColor: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f

    // Each particle: (x-offset from center, radius, color index, phase-offset)
    val particles = listOf(
        Triple(-55f,  5f, 0),
        Triple( 55f,  4f, 1),
        Triple(-35f,  6f, 2),
        Triple( 40f,  3f, 3),
        Triple(-60f,  4f, 4),
        Triple( 20f,  5f, 5),
        Triple(-20f,  3f, 6),
        Triple( 60f,  6f, 7),
    )

    val palette = listOf(
        KhwabBlue,
        KhwabViolet,
        KhwabGreen,
        KhwabYellow,
        accentColor,
        KhwabBlue.copy(alpha = 0.7f),
        KhwabViolet.copy(alpha = 0.7f),
        KhwabGreen.copy(alpha = 0.7f),
    )

    particles.forEachIndexed { i, (xOff, radius, colorIdx) ->
        val phase = (particlePhase + i / 8f) % 1f   // stagger each by 1/8
        // travel from cy+20 upward to cy-size.height
        val y = cy + 20f - phase * (cy + size.height * 0.8f)
        val alpha = when {
            phase < 0.2f -> phase / 0.2f            // fade in
            phase > 0.7f -> (1f - phase) / 0.3f     // fade out
            else         -> 1f
        }
        drawCircle(
            color  = palette[colorIdx].copy(alpha = alpha.coerceIn(0f, 1f)),
            radius = radius,
            center = Offset(cx + xOff, y)
        )
    }
}




// ── Glowing gradient progress bar with sweeping shimmer ──────────────────────
@Composable
private fun GlowProgressBar(
    progress: Float,
    shimmer: Float,
    modifier: Modifier = Modifier
) {
    // Smooth the raw progress value so it animates rather than jumps
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "prog_anim"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val r = h / 2f

        // ── Track (dark background pill) ────────────────────────────────────
        drawRoundRect(
            color      = Color(0xFF1A2540),
            size       = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r)
        )

        if (animatedProgress > 0f) {
            val fillW = w * animatedProgress.coerceIn(0f, 1f)

            // ── Glow halo underneath fill ────────────────────────────────────
            drawIntoCanvas { canvas ->
                val glowPaint = Paint().apply { isAntiAlias = true }
                glowPaint.asFrameworkPaint().apply {
                    color = android.graphics.Color.TRANSPARENT
                    maskFilter = android.graphics.BlurMaskFilter(
                        h * 2.5f,
                        android.graphics.BlurMaskFilter.Blur.NORMAL
                    )
                    setColor(KhwabBlue.copy(alpha = 0.55f).toArgb())
                }
                canvas.drawRoundRect(
                    left   = 0f,
                    top    = 0f,
                    right  = fillW,
                    bottom = h,
                    radiusX = r,
                    radiusY = r,
                    paint  = glowPaint
                )
            }

            // ── Gradient fill pill ───────────────────────────────────────────
            drawRoundRect(
                brush  = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(KhwabBlue, KhwabViolet),
                    startX = 0f,
                    endX   = fillW
                ),
                size   = androidx.compose.ui.geometry.Size(fillW, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r)
            )

            // ── Shimmer highlight stripe sweeping across filled area ─────────
            val stripeW = fillW * 0.35f
            val stripeX = (fillW + stripeW) * shimmer - stripeW
            drawRoundRect(
                brush  = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.45f),
                        Color.Transparent
                    ),
                    startX = stripeX,
                    endX   = stripeX + stripeW
                ),
                size   = androidx.compose.ui.geometry.Size(fillW, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r)
            )

            // ── Bright top edge highlight (3D capsule look) ─────────────────
            drawRoundRect(
                brush  = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.10f)
                    ),
                    startX = 0f,
                    endX   = fillW
                ),
                topLeft = Offset(0f, 0f),
                size    = androidx.compose.ui.geometry.Size(fillW, h * 0.45f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r)
            )
        }
    }
}


// ── Smoothly cycle through KhwabBlue → KhwabViolet → KhwabGreen ─────────────
private fun lerpBrandColor(t: Float): Color {
    val colors = listOf(KhwabBlue, KhwabViolet, KhwabGreen, KhwabYellow, KhwabBlue)
    val scaled = t * (colors.size - 1)
    val idx = scaled.toInt().coerceIn(0, colors.size - 2)
    val frac = scaled - idx
    val a = colors[idx]
    val b = colors[idx + 1]
    return Color(
        red   = a.red   + (b.red   - a.red)   * frac,
        green = a.green + (b.green - a.green) * frac,
        blue  = a.blue  + (b.blue  - a.blue)  * frac,
        alpha = 1f
    )
}
