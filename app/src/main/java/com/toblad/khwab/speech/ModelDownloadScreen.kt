package com.toblad.khwab.speech

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInBounce
import androidx.compose.animation.core.EaseInOutCubic
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

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

    // ── Single infinite transition driving all logo animations ───────────────
    val anim = rememberInfiniteTransition(label = "logo_anim")

    // ── BOUNCE: parabolic arc — rises high, hangs, drops fast ────────────────
    // Cycle: rise(480ms) → hang(80ms) → fall+bounce(400ms) → rest(840ms)
    val bounceY by anim.animateFloat(
        initialValue = 0f,
        targetValue  = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1800
                0f   at 0    using FastOutSlowInEasing
                -72f at 480  using LinearEasing
                -76f at 560  using LinearEasing
                0f   at 960  using EaseInBounce
                0f   at 1800 using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "bounce_y"
    )

    // ── SQUISH / STRETCH scale Y ──────────────────────────────────────────────
    val scaleY by anim.animateFloat(
        initialValue = 1f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1800
                1.00f at 0    using FastOutSlowInEasing
                0.72f at 120  using FastOutSlowInEasing
                1.35f at 480  using FastOutSlowInEasing
                1.00f at 700  using LinearEasing
                0.60f at 960  using FastOutSlowInEasing
                1.12f at 1100 using FastOutSlowInEasing
                1.00f at 1300 using FastOutSlowInEasing
                1.00f at 1800 using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "scale_y"
    )

    // ── SQUISH scale X (inverse of Y for rubbery feel) ───────────────────────
    val scaleX by anim.animateFloat(
        initialValue = 1f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1800
                1.00f at 0
                1.28f at 120
                0.78f at 480
                1.00f at 700
                1.45f at 960
                0.88f at 1100
                1.00f at 1300
                1.00f at 1800
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "scale_x"
    )

    // ── TILT: wobble left and right mid-air ───────────────────────────────────
    val tiltDeg by anim.animateFloat(
        initialValue = 0f,
        targetValue  = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1800
                0f   at 0
                -14f at 280
                18f  at 560
                -8f  at 760
                4f   at 880
                0f   at 960
                0f   at 1800
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "tilt"
    )

    // ── FULL SPIN: one complete 360° rotation at the apex ─────────────────────
    val spinDeg by anim.animateFloat(
        initialValue = 0f,
        targetValue  = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1800
                0f   at 0
                0f   at 350  using EaseInOutCubic
                360f at 750  using EaseInOutCubic   // full spin at apex
                360f at 1800
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    // ── Y-AXIS FLIP: perspective-warp scaleX during apex ─────────────────────
    val flipPhase by anim.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1800
                0f at 0
                0f at 400
                1f at 700
                1f at 1800
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "flip"
    )
    val flipCos = cos(flipPhase * PI.toFloat())

    // ── COLOR CYCLE: brand palette rotation ───────────────────────────────────
    val colorCycle by anim.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "color_cycle"
    )

    // ── SHADOW: shrinks when logo is high, expands on landing ────────────────
    val shadowScale by anim.animateFloat(
        initialValue = 1f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1800
                1.0f at 0
                0.4f at 120
                0.15f at 560
                0.15f at 600
                0.9f at 960
                1.0f at 1300
                1.0f at 1800
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "shadow_scale"
    )
    val shadowAlpha by anim.animateFloat(
        initialValue = 0.45f,
        targetValue  = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1800
                0.45f at 0
                0.10f at 560
                0.10f at 600
                0.45f at 960
                0.45f at 1800
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "shadow_alpha"
    )

    // ── PARTICLES: continuous drift ───────────────────────────────────────────
    val particlePhase by anim.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_phase"
    )

    // ── LANDING BURST ─────────────────────────────────────────────────────────
    val burstPhase by anim.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1800
                0f at 0
                0f at 960
                1f at 1200
                1f at 1800
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "burst"
    )

    // ── PULSE RING: expands outward from the K continuously ───────────────────
    val pulsePhase by anim.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    // ── TRAIL GHOST: faint echoes of the K left behind mid-air ───────────────
    // Offset behind the current bounceY position
    val trailOffset by anim.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "trail"
    )

    // ── SHIMMER sweep on progress bar ─────────────────────────────────────────
    val shimmerOffset by anim.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    // ── BACKGROUND GLOW pulse ─────────────────────────────────────────────────
    val bgGlowAlpha by anim.animateFloat(
        initialValue = 0.20f,
        targetValue  = 0.52f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_glow"
    )

    val logoColor = lerpBrandColor(colorCycle)

    // Effective 3D perspective-flip scaleX: combine rubber scaleX with flip warp
    val effectiveScaleX = scaleX * flipCos

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // ── Full-screen radial glow layer ─────────────────────────────────
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx     = size.width / 2f
                val cy     = size.height * 0.28f
                val radius = size.width * 0.80f
                drawCircle(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            logoColor.copy(alpha = bgGlowAlpha),
                            logoColor.copy(alpha = bgGlowAlpha * 0.25f),
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

                    // ── Animated K logo area ─────────────────────────────────
                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier.size(width = 180.dp, height = 240.dp)
                    ) {
                        // Floating particles (behind logo)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawFloatingParticles(
                                particlePhase = particlePhase,
                                accentColor   = logoColor
                            )
                        }

                        // Pulse ring (expands outward from K's resting position)
                        Canvas(
                            modifier = Modifier
                                .size(160.dp)
                                .align(Alignment.BottomCenter)
                        ) {
                            drawPulseRing(phase = pulsePhase, color = logoColor)
                        }

                        // Landing burst (at ground level)
                        Canvas(
                            modifier = Modifier
                                .size(width = 180.dp, height = 60.dp)
                                .align(Alignment.BottomCenter)
                        ) {
                            drawLandingBurst(phase = burstPhase, color = logoColor)
                        }

                        // Ground shadow ellipse
                        Canvas(
                            modifier = Modifier
                                .size(width = 120.dp, height = 24.dp)
                                .align(Alignment.BottomCenter)
                        ) {
                            drawOval(
                                color   = logoColor.copy(alpha = shadowAlpha),
                                topLeft = Offset(
                                    x = center.x - (size.width / 2f) * shadowScale,
                                    y = center.y - (size.height / 2f) * 0.4f
                                ),
                                size = Size(
                                    width  = size.width  * shadowScale,
                                    height = size.height * 0.4f
                                )
                            )
                        }

                        // Trail ghosts — two faint echoes of the K, offset upward
                        Canvas(
                            modifier = Modifier
                                .size(120.dp)
                                .align(Alignment.TopCenter)
                        ) {
                            // Only draw trails while airborne
                            val airborne = bounceY < -8f
                            if (airborne) {
                                // Ghost 1: 20px above current position, 25% opacity
                                translate(top = bounceY + 20f) {
                                    scale(scaleX = effectiveScaleX * 0.9f, scaleY = scaleY * 0.9f) {
                                        rotate(degrees = tiltDeg + spinDeg) {
                                            drawKLetter(
                                                color   = logoColor.copy(alpha = 0.22f),
                                                flipCos = flipCos
                                            )
                                        }
                                    }
                                }
                                // Ghost 2: 42px above, 12% opacity
                                translate(top = bounceY + 42f) {
                                    scale(scaleX = effectiveScaleX * 0.78f, scaleY = scaleY * 0.78f) {
                                        rotate(degrees = tiltDeg + spinDeg) {
                                            drawKLetter(
                                                color   = logoColor.copy(alpha = 0.10f),
                                                flipCos = flipCos
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // The main "K" — bouncing + spinning + flipping + tilting
                        Canvas(
                            modifier = Modifier
                                .size(120.dp)
                                .align(Alignment.TopCenter)
                        ) {
                            translate(top = bounceY) {
                                scale(scaleX = effectiveScaleX, scaleY = scaleY) {
                                    rotate(degrees = tiltDeg + spinDeg) {
                                        drawKLetter(
                                            color   = logoColor,
                                            flipCos = flipCos
                                        )
                                    }
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
                            val percent  = (s as? ModelDownloadState.Downloading)?.percent ?: 0
                            val fileName = (s as? ModelDownloadState.Downloading)?.currentFile ?: ""

                            val displayPercent by animateIntAsState(
                                targetValue   = percent,
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

                            GlowProgressBar(
                                progress = percent / 100f,
                                shimmer  = shimmerOffset,
                                color    = logoColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp)
                            )

                            Text(
                                text  = "$displayPercent%",
                                color = logoColor,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            AnimatedVisibility(
                                visible = fileName.isNotBlank(),
                                enter   = fadeIn(animationSpec = tween(durationMillis = 400))
                            ) {
                                Text(
                                    text  = fileName,
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
                                text  = s.message,
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = viewModel::retry,
                                shape   = RoundedCornerShape(14.dp)
                            ) { Text("Retry") }
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
            }
        }
    }
}

// ── Build the K letter paths (vertical bar + upper arm + lower arm) ───────────
private fun buildKPaths(w: Float, h: Float, stroke: Float): Triple<Path, Path, Path> {
    val barLeft   = w * 0.12f
    val barRight  = barLeft + stroke
    val topY      = h * 0.08f
    val botY      = h * 0.92f
    val midY      = h * 0.50f
    val armStartX = barRight - stroke * 0.05f   // arms overlap the bar slightly
    val tipX      = w * 0.90f

    val bar = Path().apply {
        addRect(
            androidx.compose.ui.geometry.Rect(barLeft, topY, barRight, botY)
        )
    }

    // Upper diagonal arm: trapezoid from mid-spine to top-right
    val upper = Path().apply {
        moveTo(armStartX, midY - stroke * 0.25f)
        lineTo(armStartX, midY + stroke * 0.75f)
        lineTo(tipX,      topY + stroke)
        lineTo(tipX,      topY)
        close()
    }

    // Lower diagonal arm: trapezoid from mid-spine to bottom-right
    val lower = Path().apply {
        moveTo(armStartX, midY - stroke * 0.75f)
        lineTo(armStartX, midY + stroke * 0.25f)
        lineTo(tipX,      botY)
        lineTo(tipX,      botY - stroke)
        close()
    }

    return Triple(bar, upper, lower)
}

// ── Draw the bold 3D "K" letter using only DrawScope primitives ───────────────
private fun DrawScope.drawKLetter(color: Color, flipCos: Float) {
    val w      = size.width
    val h      = size.height
    val stroke = w * 0.19f

    val depthSign   = if (flipCos >= 0f) 1f else -1f
    val depthOffset = w * 0.055f * depthSign

    val depthColor = Color(
        red   = (color.red   * 0.45f).coerceIn(0f, 1f),
        green = (color.green * 0.45f).coerceIn(0f, 1f),
        blue  = (color.blue  * 0.45f).coerceIn(0f, 1f),
        alpha = color.alpha * 0.75f
    )

    val (bar, upper, lower) = buildKPaths(w, h, stroke)

    // ── Depth / shadow layer ──────────────────────────────────────────────────
    translate(left = depthOffset, top = depthOffset) {
        drawPath(bar,   color = depthColor, style = Fill)
        drawPath(upper, color = depthColor, style = Fill)
        drawPath(lower, color = depthColor, style = Fill)
    }

    // ── Outer glow: draw oversized blurred version via drawIntoCanvas ─────────
    drawIntoCanvas { canvas ->
        val glowPaint = Paint().apply { isAntiAlias = true }
        glowPaint.asFrameworkPaint().apply {
            maskFilter = android.graphics.BlurMaskFilter(
                w * 0.13f,
                android.graphics.BlurMaskFilter.Blur.NORMAL
            )
            this.color = color.copy(alpha = color.alpha * 0.70f).toArgb()
        }
        // Draw bar rect as glow
        canvas.drawRect(
            left   = w * 0.12f,
            top    = h * 0.08f,
            right  = w * 0.12f + stroke,
            bottom = h * 0.92f,
            paint  = glowPaint
        )
    }

    // ── Main bright K ─────────────────────────────────────────────────────────
    drawPath(bar,   color = color, style = Fill)
    drawPath(upper, color = color, style = Fill)
    drawPath(lower, color = color, style = Fill)

    // ── Top-left shine highlight ───────────────────────────────────────────────
    val shineColor = Color.White.copy(alpha = color.alpha * 0.40f)
    val (shineBar, shineUpper, _) = buildKPaths(w * 0.22f / stroke + w * 0.12f, h, stroke * 0.22f)
    // Simpler: just draw a narrow bright strip on the left edge of the bar
    drawRect(
        color   = shineColor,
        topLeft = Offset(w * 0.12f, h * 0.08f),
        size    = Size(stroke * 0.22f, h * 0.44f)
    )
    // Thin shine on top edge of upper arm
    drawPath(
        path  = Path().apply {
            val armStartX = w * 0.12f + stroke
            val tipX      = w * 0.90f
            val midY      = h * 0.50f
            moveTo(armStartX, midY - stroke * 0.25f)
            lineTo(tipX,      h * 0.08f)
            lineTo(tipX,      h * 0.08f + stroke * 0.22f)
            lineTo(armStartX, midY - stroke * 0.25f + stroke * 0.22f)
            close()
        },
        color = shineColor,
        style = Fill
    )
}

// ── Expanding pulse ring centered on the K ────────────────────────────────────
// phase 0→1: ring expands from r=0 to r=max and fades out
private fun DrawScope.drawPulseRing(phase: Float, color: Color) {
    val cx        = size.width  / 2f
    val cy        = size.height / 2f
    val maxRadius = size.width  * 0.46f
    val radius    = maxRadius * phase
    val alpha     = (1f - phase).coerceIn(0f, 1f) * 0.70f

    if (radius > 0f) {
        drawCircle(
            color       = color.copy(alpha = alpha),
            radius      = radius,
            center      = Offset(cx, cy),
            style       = Stroke(width = 3.5f * (1f - phase * 0.6f))
        )
        // Second inner ring, slightly offset in phase
        val r2     = maxRadius * ((phase + 0.3f) % 1f)
        val alpha2 = (1f - ((phase + 0.3f) % 1f)).coerceIn(0f, 1f) * 0.40f
        drawCircle(
            color  = color.copy(alpha = alpha2),
            radius = r2,
            center = Offset(cx, cy),
            style  = Stroke(width = 2f)
        )
    }
}

// ── 10 particles drifting upward around the logo ──────────────────────────────
private fun DrawScope.drawFloatingParticles(particlePhase: Float, accentColor: Color) {
    val cx = size.width  / 2f
    val cy = size.height / 2f

    val particles = listOf(
        Triple(-62f, 5f, 0),
        Triple( 58f, 4f, 1),
        Triple(-38f, 6f, 2),
        Triple( 44f, 3f, 3),
        Triple(-70f, 4f, 4),
        Triple( 22f, 5f, 5),
        Triple(-22f, 3f, 6),
        Triple( 68f, 6f, 7),
        Triple(-48f, 4f, 8),
        Triple( 36f, 5f, 9),
    )

    val palette = listOf(
        KhwabBlue,
        KhwabViolet,
        KhwabGreen,
        KhwabYellow,
        accentColor,
        KhwabBlue.copy(alpha   = 0.7f),
        KhwabViolet.copy(alpha = 0.7f),
        KhwabGreen.copy(alpha  = 0.7f),
        accentColor.copy(alpha = 0.6f),
        KhwabYellow.copy(alpha = 0.7f),
    )

    particles.forEachIndexed { i, (xOff, radius, colorIdx) ->
        val phase = (particlePhase + i / 10f) % 1f
        val y     = cy + 20f - phase * (cy + size.height * 0.85f)
        val alpha = when {
            phase < 0.15f -> phase / 0.15f
            phase > 0.72f -> (1f - phase) / 0.28f
            else          -> 1f
        }
        drawCircle(
            color  = palette[colorIdx].copy(alpha = alpha.coerceIn(0f, 1f)),
            radius = radius,
            center = Offset(cx + xOff, y)
        )
    }
}

// ── Radial landing-burst: 8 short spikes that expand outward on landing ───────
private fun DrawScope.drawLandingBurst(phase: Float, color: Color) {
    if (phase <= 0f || phase >= 1f) return

    val cx        = size.width  / 2f
    val cy        = size.height * 0.25f
    val maxRadius = size.width  * 0.45f
    val spikes    = 8
    val alpha     = (1f - phase).coerceIn(0f, 1f) * 0.85f

    for (i in 0 until spikes) {
        val angle  = (i * (2f * PI / spikes)).toFloat()
        val r      = maxRadius * phase
        val perpAngle = angle + PI.toFloat() / 2f
        val baseW  = 6f * (1f - phase * 0.8f)

        val tipX   = cx + cos(angle) * r
        val tipY   = cy + sin(angle) * r
        val baseX1 = cx + cos(perpAngle) * baseW
        val baseY1 = cy + sin(perpAngle) * baseW
        val baseX2 = cx - cos(perpAngle) * baseW
        val baseY2 = cy - sin(perpAngle) * baseW

        drawPath(
            path = Path().apply {
                moveTo(tipX, tipY)
                lineTo(baseX1, baseY1)
                lineTo(baseX2, baseY2)
                close()
            },
            color = color.copy(alpha = alpha),
            style = Fill
        )
    }

    val dotR = 8f * (1f - phase)
    if (dotR > 0f) {
        drawCircle(
            color  = color.copy(alpha = alpha),
            radius = dotR,
            center = Offset(cx, cy)
        )
    }
}

// ── Glowing gradient progress bar with animated shimmer ──────────────────────
@Composable
private fun GlowProgressBar(
    progress: Float,
    shimmer:  Float,
    color:    Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue   = progress,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "prog_anim"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val r = h / 2f

        drawRoundRect(
            color        = Color(0xFF1A2540),
            size         = size,
            cornerRadius = CornerRadius(r)
        )

        if (animatedProgress > 0f) {
            val fillW = w * animatedProgress.coerceIn(0f, 1f)

            drawIntoCanvas { canvas ->
                val glowPaint = Paint().apply { isAntiAlias = true }
                glowPaint.asFrameworkPaint().apply {
                    maskFilter = android.graphics.BlurMaskFilter(
                        h * 3f,
                        android.graphics.BlurMaskFilter.Blur.NORMAL
                    )
                    setColor(color.copy(alpha = 0.55f).toArgb())
                }
                canvas.drawRoundRect(0f, 0f, fillW, h, r, r, glowPaint)
            }

            drawRoundRect(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(KhwabBlue, color, KhwabViolet),
                    startX = 0f,
                    endX   = fillW
                ),
                size         = Size(fillW, h),
                cornerRadius = CornerRadius(r)
            )

            val stripeW = fillW * 0.38f
            val stripeX = (fillW + stripeW) * shimmer - stripeW
            drawRoundRect(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.50f),
                        Color.Transparent
                    ),
                    startX = stripeX,
                    endX   = stripeX + stripeW
                ),
                size         = Size(fillW, h),
                cornerRadius = CornerRadius(r)
            )

            drawRoundRect(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.28f),
                        Color.White.copy(alpha = 0.08f)
                    ),
                    startX = 0f,
                    endX   = fillW
                ),
                topLeft      = Offset(0f, 0f),
                size         = Size(fillW, h * 0.42f),
                cornerRadius = CornerRadius(r)
            )
        }
    }
}

// ── Smoothly cycle through KhwabBlue → KhwabViolet → KhwabGreen → KhwabYellow
private fun lerpBrandColor(t: Float): Color {
    val colors = listOf(KhwabBlue, KhwabViolet, KhwabGreen, KhwabYellow, KhwabBlue)
    val scaled = t * (colors.size - 1)
    val idx    = scaled.toInt().coerceIn(0, colors.size - 2)
    val frac   = scaled - idx
    val a      = colors[idx]
    val b      = colors[idx + 1]
    return Color(
        red   = a.red   + (b.red   - a.red)   * frac,
        green = a.green + (b.green - a.green) * frac,
        blue  = a.blue  + (b.blue  - a.blue)  * frac,
        alpha = 1f
    )
}
