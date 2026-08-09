package com.toblad.khwab.speech

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInBounce
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.EaseOutExpo
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full-screen download screen shown on first launch when the Whisper
 * model files are not yet present on-device.
 *
 * When download completes a multi-stage "dream comes to reality" cinematic
 * plays before [onReady] is called to reveal the home screen.
 *
 * Sequence (total ~3 200 ms):
 *   0 –  700 ms  K launches upward, K grows, glow floods the screen
 *   700 – 1 400 ms  K reaches apex — radial starburst explodes outward
 *   1 400 – 2 200 ms  burst fades, screen washes to pure white
 *   2 200 – 3 200 ms  white dissolves → onReady() called
 */
@Composable
fun ModelDownloadScreen(
    onReady: () -> Unit,
    viewModel: ModelDownloadViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // ── Dream-to-reality transition animatables ───────────────────────────────
    // dreamActive: true while the cinematic is playing
    var dreamActive by remember { mutableStateOf(false) }

    // K soars up and grows
    val dreamKOffsetY  = remember { Animatable(0f) }
    val dreamKScale    = remember { Animatable(1f) }
    // Radial starburst radius (0 → full screen diagonal)
    val dreamBurst     = remember { Animatable(0f) }
    // Glow bloom alpha that floods the screen
    val dreamGlowAlpha = remember { Animatable(0f) }
    // Final white-out overlay (0 → 1)
    val dreamWhite     = remember { Animatable(0f) }
    // Content fade out (1 → 0) during whiteout
    val dreamContentAlpha = remember { Animatable(1f) }

    LaunchedEffect(state) {
        val done = state is ModelDownloadState.Ready || state is ModelDownloadState.Completed
        if (done && !dreamActive) {
            dreamActive = true

            // ── Stage 1 (0–700 ms): K launches up, glow floods ───────────────
            launch { dreamKOffsetY.animateTo(-420f, tween(700, easing = EaseOutExpo)) }
            launch { dreamKScale.animateTo(3.5f,    tween(700, easing = EaseOutCubic)) }
            launch { dreamGlowAlpha.animateTo(0.85f, tween(700, easing = FastOutSlowInEasing)) }
            delay(500)

            // ── Stage 2 (500–1 400 ms): starburst explodes ───────────────────
            launch { dreamBurst.animateTo(1f, tween(900, easing = EaseOutCubic)) }
            delay(600)

            // ── Stage 3 (1 100–2 200 ms): content fades, white washes in ─────
            launch { dreamContentAlpha.animateTo(0f, tween(700, easing = FastOutSlowInEasing)) }
            launch { dreamWhite.animateTo(1f, tween(900, easing = EaseOutCubic)) }
            delay(700)

            // ── Stage 4 (1 800–3 200 ms): white dissolves and we hand off ────
            launch { dreamWhite.animateTo(0f, tween(1000, easing = EaseOutCubic)) }
            delay(1000)

            onReady()
        }
    }

    LaunchedEffect(Unit) {
        if (state is ModelDownloadState.Idle) {
            viewModel.startDownload()
        }
    }

    val colors = MaterialTheme.colorScheme

    // ── Infinite transition — idle bouncing logo ──────────────────────────────
    val anim = rememberInfiniteTransition(label = "logo_anim")

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

    val spinDeg by anim.animateFloat(
        initialValue = 0f,
        targetValue  = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1800
                0f   at 0
                0f   at 350  using EaseInOutCubic
                360f at 750  using EaseInOutCubic
                360f at 1800
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

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

    val colorCycle by anim.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "color_cycle"
    )

    val shadowScale by anim.animateFloat(
        initialValue = 1f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1800
                1.0f  at 0
                0.4f  at 120
                0.15f at 560
                0.15f at 600
                0.9f  at 960
                1.0f  at 1300
                1.0f  at 1800
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

    val particlePhase by anim.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_phase"
    )

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

    val pulsePhase by anim.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    val shimmerOffset by anim.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val bgGlowAlpha by anim.animateFloat(
        initialValue = 0.20f,
        targetValue  = 0.52f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_glow"
    )

    val logoColor   = lerpBrandColor(colorCycle)
    val effectiveScaleX = scaleX * flipCos

    // During dream: K uses dream animatables instead of idle bounce
    val activeKOffsetY = if (dreamActive) dreamKOffsetY.value else bounceY
    val activeKScaleX  = if (dreamActive) dreamKScale.value * effectiveScaleX else effectiveScaleX
    val activeKScaleY  = if (dreamActive) dreamKScale.value * scaleY else scaleY

    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = colors.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            // ── Idle radial glow layer (fades out as dream activates) ─────────
            if (!dreamActive) {
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
            }

            // ── Dream Stage 1: flooding glow bloom ───────────────────────────
            if (dreamActive) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx     = size.width / 2f
                    val cy     = size.height * 0.35f
                    val radius = size.width * 1.6f
                    drawCircle(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                logoColor.copy(alpha = dreamGlowAlpha.value),
                                KhwabViolet.copy(alpha = dreamGlowAlpha.value * 0.7f),
                                KhwabBlue.copy(alpha = dreamGlowAlpha.value * 0.4f),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = radius
                        ),
                        radius = radius,
                        center = Offset(cx, cy)
                    )
                }
            }

            // ── Dream Stage 2: starburst expanding rays ───────────────────────
            if (dreamActive && dreamBurst.value > 0f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawDreamStarburst(
                        phase      = dreamBurst.value,
                        color      = logoColor,
                        glowAlpha  = dreamGlowAlpha.value
                    )
                }
            }

            // ── Main content (fades out during dream stage 3) ─────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        // fade content out during whiteout stage
                        .then(
                            if (dreamActive)
                                Modifier.then(Modifier) // alpha applied per-canvas below
                            else Modifier
                        )
                ) {
                    // ── Animated K logo area ─────────────────────────────────
                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier.size(width = 180.dp, height = 240.dp)
                    ) {
                        if (!dreamActive) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawFloatingParticles(
                                    particlePhase = particlePhase,
                                    accentColor   = logoColor
                                )
                            }

                            Canvas(
                                modifier = Modifier
                                    .size(160.dp)
                                    .align(Alignment.BottomCenter)
                            ) {
                                drawPulseRing(phase = pulsePhase, color = logoColor)
                            }

                            Canvas(
                                modifier = Modifier
                                    .size(width = 180.dp, height = 60.dp)
                                    .align(Alignment.BottomCenter)
                            ) {
                                drawLandingBurst(phase = burstPhase, color = logoColor)
                            }

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
                        }

                        // Trail ghosts while airborne (idle only)
                        if (!dreamActive) {
                            Canvas(
                                modifier = Modifier
                                    .size(120.dp)
                                    .align(Alignment.TopCenter)
                            ) {
                                val airborne = bounceY < -8f
                                if (airborne) {
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
                        }

                        // The K — both idle and dream use the same canvas, different values
                        Canvas(
                            modifier = Modifier
                                .size(120.dp)
                                .align(Alignment.TopCenter)
                        ) {
                            translate(top = activeKOffsetY) {
                                scale(scaleX = activeKScaleX, scaleY = activeKScaleY) {
                                    rotate(degrees = tiltDeg + spinDeg) {
                                        drawKLetter(
                                            color   = logoColor.copy(
                                                alpha = dreamContentAlpha.value
                                            ),
                                            flipCos = flipCos
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Text and progress — fade out during dream
                    val contentA = dreamContentAlpha.value

                    Text(
                        text = "Khwab",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.onBackground.copy(alpha = contentA)
                    )

                    Text(
                        text = "Your intelligent voice companion",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant.copy(alpha = contentA),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Setting up Khwab",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onBackground.copy(alpha = contentA)
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
                                color = colors.onSurfaceVariant.copy(alpha = contentA),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            GlowProgressBar(
                                progress = percent / 100f,
                                shimmer  = shimmerOffset,
                                color    = logoColor,
                                alpha    = contentA,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp)
                            )

                            Text(
                                text  = "$displayPercent%",
                                color = logoColor.copy(alpha = contentA),
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
                                    color = colors.onSurfaceVariant.copy(alpha = contentA),
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        is ModelDownloadState.Failed -> {
                            Text(
                                text = "Download failed",
                                color = colors.error.copy(alpha = contentA),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text  = s.message,
                                color = colors.onSurfaceVariant.copy(alpha = contentA),
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
                            // "Ready!" briefly visible before dream sequence begins
                            Text(
                                text = "Your dream is ready ✨",
                                color = logoColor.copy(alpha = contentA),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            // ── Dream Stage 3: full-screen white-out veil ────────────────────
            if (dreamActive && dreamWhite.value > 0f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        color = Color.White.copy(alpha = dreamWhite.value),
                        size  = size
                    )
                }
            }
        }
    }
}

// ── Dream starburst: 24 long light-rays shooting outward from screen centre ───
// phase 0→1: rays grow from 0 to full diagonal; alpha fades as they expand
private fun DrawScope.drawDreamStarburst(
    phase:     Float,
    color:     Color,
    glowAlpha: Float
) {
    val cx    = size.width  / 2f
    val cy    = size.height * 0.38f   // slightly above centre — where K was
    val diag  = kotlin.math.sqrt((size.width * size.width + size.height * size.height).toDouble()).toFloat()
    val rays  = 24
    val alpha = ((1f - phase * 0.7f) * glowAlpha).coerceIn(0f, 1f)

    for (i in 0 until rays) {
        val angle    = (i * (2f * PI / rays)).toFloat()
        val rayLen   = diag * phase
        val baseW    = (14f * (1f - phase * 0.6f)).coerceAtLeast(2f)
        val perpAngle = angle + PI.toFloat() / 2f

        val tipX   = cx + cos(angle) * rayLen
        val tipY   = cy + sin(angle) * rayLen
        val baseX1 = cx + cos(perpAngle) * baseW
        val baseY1 = cy + sin(perpAngle) * baseW
        val baseX2 = cx - cos(perpAngle) * baseW
        val baseY2 = cy - sin(perpAngle) * baseW

        // Alternate between brand colours for a rainbow-dream effect
        val rayColor = when (i % 4) {
            0    -> KhwabBlue
            1    -> KhwabViolet
            2    -> color
            else -> KhwabGreen
        }.copy(alpha = alpha)

        drawPath(
            path = Path().apply {
                moveTo(tipX, tipY)
                lineTo(baseX1, baseY1)
                lineTo(baseX2, baseY2)
                close()
            },
            color = rayColor,
            style = Fill
        )
    }

    // Central bright core that shrinks as rays expand
    val coreR = (80f * (1f - phase)).coerceAtLeast(0f)
    if (coreR > 0f) {
        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = glowAlpha),
                    color.copy(alpha = glowAlpha * 0.6f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = coreR
            ),
            radius = coreR,
            center = Offset(cx, cy)
        )
    }

    // Concentric ring wave that races outward behind the rays
    val ringR = diag * 0.55f * phase
    if (ringR > 0f) {
        drawCircle(
            color  = Color.White.copy(alpha = (alpha * 0.55f).coerceIn(0f, 1f)),
            radius = ringR,
            center = Offset(cx, cy),
            style  = Stroke(width = 8f * (1f - phase * 0.8f))
        )
    }
}

// ── Build the K letter paths (vertical bar + upper arm + lower arm) ───────────
private fun buildKPaths(w: Float, h: Float, stroke: Float): Triple<Path, Path, Path> {
    val barLeft   = w * 0.12f
    val barRight  = barLeft + stroke
    val topY      = h * 0.08f
    val botY      = h * 0.92f
    val midY      = h * 0.50f
    val armStartX = barRight - stroke * 0.05f
    val tipX      = w * 0.90f

    val bar = Path().apply {
        addRect(androidx.compose.ui.geometry.Rect(barLeft, topY, barRight, botY))
    }
    val upper = Path().apply {
        moveTo(armStartX, midY - stroke * 0.25f)
        lineTo(armStartX, midY + stroke * 0.75f)
        lineTo(tipX,      topY + stroke)
        lineTo(tipX,      topY)
        close()
    }
    val lower = Path().apply {
        moveTo(armStartX, midY - stroke * 0.75f)
        lineTo(armStartX, midY + stroke * 0.25f)
        lineTo(tipX,      botY)
        lineTo(tipX,      botY - stroke)
        close()
    }
    return Triple(bar, upper, lower)
}

// ── Draw the bold 3D "K" letter ───────────────────────────────────────────────
//
// 3D layers (back → front):
//   1. Deep shadow  — large offset, near-black, gives the illusion of depth
//   2. Side faces   — stepped offsets in a darkened hue, fill the "extrusion" body
//   3. Outer glow   — blurred bloom around the front face
//   4. Front face   — full brand colour
//   5. Shine strip  — white highlight on the left/top edge, simulates a light source
private fun DrawScope.drawKLetter(color: Color, flipCos: Float) {
    val w      = size.width
    val h      = size.height
    val stroke = w * 0.19f

    // Depth direction follows the flip so it looks consistent during spin.
    val depthSign = if (flipCos >= 0f) 1f else -1f

    // ── Layer 1: deep cast shadow ─────────────────────────────────────────────
    // A large offset dark fill that peeks out from behind every face.
    val shadowOffset = w * 0.11f * depthSign
    val shadowColor  = Color(0f, 0f, 0f, color.alpha * 0.55f)
    val (sBar, sUpper, sLower) = buildKPaths(w, h, stroke)
    translate(left = shadowOffset, top = shadowOffset * 0.7f) {
        drawPath(sBar,   color = shadowColor, style = Fill)
        drawPath(sUpper, color = shadowColor, style = Fill)
        drawPath(sLower, color = shadowColor, style = Fill)
    }

    // ── Layer 2: extruded side faces (4 stepped slices) ───────────────────────
    // Each slice is a slightly darkened version of the brand colour, stacked
    // between the shadow and the front face to simulate a thick 3-D body.
    val steps = 4
    for (i in steps downTo 1) {
        val frac        = i / steps.toFloat()
        val sliceOffset = w * 0.085f * frac * depthSign
        val brightness  = 0.30f + 0.25f * (1f - frac)   // 0.30 → 0.55 darkest→lighter
        val sliceColor  = Color(
            red   = (color.red   * brightness).coerceIn(0f, 1f),
            green = (color.green * brightness).coerceIn(0f, 1f),
            blue  = (color.blue  * brightness).coerceIn(0f, 1f),
            alpha = color.alpha
        )
        val (fBar, fUpper, fLower) = buildKPaths(w, h, stroke)
        translate(left = sliceOffset, top = sliceOffset * 0.7f) {
            drawPath(fBar,   color = sliceColor, style = Fill)
            drawPath(fUpper, color = sliceColor, style = Fill)
            drawPath(fLower, color = sliceColor, style = Fill)
        }
    }

    // ── Layer 3: outer glow bloom ─────────────────────────────────────────────
    drawIntoCanvas { canvas ->
        val glowPaint = Paint().apply { isAntiAlias = true }
        glowPaint.asFrameworkPaint().apply {
            maskFilter = android.graphics.BlurMaskFilter(
                w * 0.18f, android.graphics.BlurMaskFilter.Blur.NORMAL
            )
            this.color = color.copy(alpha = color.alpha * 0.75f).toArgb()
        }
        canvas.drawRect(w * 0.12f, h * 0.08f, w * 0.12f + stroke, h * 0.92f, glowPaint)
    }

    // ── Layer 4: front face (full brand colour) ───────────────────────────────
    val (bar, upper, lower) = buildKPaths(w, h, stroke)
    drawPath(bar,   color = color, style = Fill)
    drawPath(upper, color = color, style = Fill)
    drawPath(lower, color = color, style = Fill)

    // ── Layer 5: shine highlight (top-left light source) ─────────────────────
    val shineColor = Color.White.copy(alpha = color.alpha * 0.55f)
    // Vertical bar: left-edge shine strip
    drawRect(
        color   = shineColor,
        topLeft = Offset(w * 0.12f, h * 0.08f),
        size    = Size(stroke * 0.20f, h * 0.84f)
    )
    // Upper arm: top-edge shine strip
    drawPath(
        path = Path().apply {
            val asx  = w * 0.12f + stroke
            val tipX = w * 0.90f
            val midY = h * 0.50f
            moveTo(asx,  midY - stroke * 0.25f)
            lineTo(tipX, h * 0.08f)
            lineTo(tipX, h * 0.08f + stroke * 0.20f)
            lineTo(asx,  midY - stroke * 0.25f + stroke * 0.20f)
            close()
        },
        color = shineColor,
        style = Fill
    )
}

// ── Expanding dual pulse rings ────────────────────────────────────────────────
private fun DrawScope.drawPulseRing(phase: Float, color: Color) {
    val cx        = size.width  / 2f
    val cy        = size.height / 2f
    val maxRadius = size.width  * 0.46f
    val radius    = maxRadius * phase
    val alpha     = (1f - phase).coerceIn(0f, 1f) * 0.70f

    if (radius > 0f) {
        drawCircle(
            color  = color.copy(alpha = alpha),
            radius = radius,
            center = Offset(cx, cy),
            style  = Stroke(width = 3.5f * (1f - phase * 0.6f))
        )
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
        Triple(-62f, 5f, 0), Triple( 58f, 4f, 1), Triple(-38f, 6f, 2),
        Triple( 44f, 3f, 3), Triple(-70f, 4f, 4), Triple( 22f, 5f, 5),
        Triple(-22f, 3f, 6), Triple( 68f, 6f, 7), Triple(-48f, 4f, 8),
        Triple( 36f, 5f, 9),
    )
    val palette = listOf(
        KhwabBlue, KhwabViolet, KhwabGreen, KhwabYellow, accentColor,
        KhwabBlue.copy(alpha = 0.7f), KhwabViolet.copy(alpha = 0.7f),
        KhwabGreen.copy(alpha = 0.7f), accentColor.copy(alpha = 0.6f),
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

// ── Radial landing-burst ──────────────────────────────────────────────────────
private fun DrawScope.drawLandingBurst(phase: Float, color: Color) {
    if (phase <= 0f || phase >= 1f) return
    val cx    = size.width  / 2f
    val cy    = size.height * 0.25f
    val maxR  = size.width  * 0.45f
    val alpha = (1f - phase).coerceIn(0f, 1f) * 0.85f
    for (i in 0 until 8) {
        val angle     = (i * (2f * PI / 8)).toFloat()
        val r         = maxR * phase
        val perpAngle = angle + PI.toFloat() / 2f
        val baseW     = 6f * (1f - phase * 0.8f)
        drawPath(
            path = Path().apply {
                moveTo(cx + cos(angle) * r,      cy + sin(angle) * r)
                lineTo(cx + cos(perpAngle) * baseW, cy + sin(perpAngle) * baseW)
                lineTo(cx - cos(perpAngle) * baseW, cy - sin(perpAngle) * baseW)
                close()
            },
            color = color.copy(alpha = alpha), style = Fill
        )
    }
    val dotR = 8f * (1f - phase)
    if (dotR > 0f) drawCircle(color = color.copy(alpha = alpha), radius = dotR, center = Offset(cx, cy))
}

// ── Glowing gradient progress bar ────────────────────────────────────────────
@Composable
private fun GlowProgressBar(
    progress: Float,
    shimmer:  Float,
    color:    Color,
    alpha:    Float = 1f,
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
            color        = Color(0xFF1A2540).copy(alpha = alpha),
            size         = size,
            cornerRadius = CornerRadius(r)
        )

        if (animatedProgress > 0f) {
            val fillW = w * animatedProgress.coerceIn(0f, 1f)

            drawIntoCanvas { canvas ->
                val glowPaint = Paint().apply { isAntiAlias = true }
                glowPaint.asFrameworkPaint().apply {
                    maskFilter = android.graphics.BlurMaskFilter(
                        h * 3f, android.graphics.BlurMaskFilter.Blur.NORMAL
                    )
                    setColor(color.copy(alpha = 0.55f * alpha).toArgb())
                }
                canvas.drawRoundRect(0f, 0f, fillW, h, r, r, glowPaint)
            }

            drawRoundRect(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(
                        KhwabBlue.copy(alpha = alpha),
                        color.copy(alpha = alpha),
                        KhwabViolet.copy(alpha = alpha)
                    ),
                    startX = 0f, endX = fillW
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
                        Color.White.copy(alpha = 0.50f * alpha),
                        Color.Transparent
                    ),
                    startX = stripeX, endX = stripeX + stripeW
                ),
                size         = Size(fillW, h),
                cornerRadius = CornerRadius(r)
            )

            drawRoundRect(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.28f * alpha),
                        Color.White.copy(alpha = 0.08f * alpha)
                    ),
                    startX = 0f, endX = fillW
                ),
                topLeft      = Offset(0f, 0f),
                size         = Size(fillW, h * 0.42f),
                cornerRadius = CornerRadius(r)
            )
        }
    }
}

// ── Brand colour lerp ─────────────────────────────────────────────────────────
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
