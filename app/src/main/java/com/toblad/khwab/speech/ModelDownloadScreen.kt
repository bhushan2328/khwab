package com.toblad.khwab.speech

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
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
    // Cycle: 0→peak(400ms)→hang(500ms)→land(900ms)→rest(1400ms)
    val bounceY by anim.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1800
                0f   at 0    using FastOutSlowInEasing
                -68f at 480  using LinearEasing        // peak — high arc
                -72f at 560  using LinearEasing        // brief hang at apex
                0f   at 960  using EaseInBounce        // fast drop + micro bounce
                0f   at 1800 using LinearEasing        // rest on ground
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "bounce_y"
    )

    // ── SQUISH / STRETCH scale Y ──────────────────────────────────────────────
    val scaleY by anim.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1800
                1.00f at 0    using FastOutSlowInEasing
                0.75f at 120  using FastOutSlowInEasing  // squish before launch
                1.30f at 480  using FastOutSlowInEasing  // tall stretch at apex
                1.00f at 700  using LinearEasing
                0.65f at 960  using FastOutSlowInEasing  // splat on landing
                1.10f at 1100 using FastOutSlowInEasing  // rebound
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
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1800
                1.00f at 0
                1.25f at 120  // wide when squished down before launch
                0.80f at 480  // narrow when stretched tall
                1.00f at 700
                1.40f at 960  // wide splat on landing
                0.90f at 1100
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
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1800
                0f   at 0
                -12f at 300   // lean left on takeoff
                15f  at 600   // swing right at apex
                -6f  at 800
                0f   at 960
                0f   at 1800
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "tilt"
    )

    // ── 3D FLIP: a Y-axis perspective rotation that happens at the apex ───────
    // 0→1 drives half a flip (0°→180°) which we map to a scaleX distortion
    val flipPhase by anim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1800
                0f   at 0
                0f   at 400
                1f   at 700   // full flip completes before descent
                1f   at 1800
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "flip"
    )
    // cos of flip angle: starts 1, goes to -1 then back — gives perspective warp
    val flipCos = cos(flipPhase * PI.toFloat())  // 1→-1 then back

    // ── COLOR CYCLE: brand palette rotation ───────────────────────────────────
    val colorCycle by anim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "color_cycle"
    )

    // ── SHADOW: shrinks when logo is high, expands on landing ────────────────
    val shadowScale by anim.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1800
                1.0f at 0
                0.4f at 120
                0.2f at 560
                0.2f at 600
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
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1800
                0.45f at 0
                0.12f at 560
                0.12f at 600
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
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_phase"
    )

    // ── LANDING BURST: a radial spike burst that fires on each landing ────────
    // 0→1 in 400ms after landing (at t=960ms), then stays at 1 until reset
    val burstPhase by anim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1800
                0f at 0
                0f at 960    // landing moment
                1f at 1200   // burst fully expanded over 240ms
                1f at 1800
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "burst"
    )

    // ── SHIMMER sweep on progress bar ─────────────────────────────────────────
    val shimmerOffset by anim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    // ── BACKGROUND GLOW pulse ─────────────────────────────────────────────────
    val bgGlowAlpha by anim.animateFloat(
        initialValue = 0.20f,
        targetValue  = 0.50f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_glow"
    )

    val logoColor = lerpBrandColor(colorCycle)

    // Effective 3D perspective-flip scaleX: combine actual scaleX with flip warp
    // flipCos ranges 1→-1→1; abs gives a "pinch" effect at mid-flip (0°)
    // The sign change (negative cos) flips the rendered appearance of the letter
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
                val cx = size.width / 2f
                val cy = size.height * 0.28f
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

                    // ── Animated 3D Logo + Particles + Landing Burst ─────────
                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier.size(width = 180.dp, height = 220.dp)
                    ) {
                        // Floating particles (behind logo)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawFloatingParticles(
                                particlePhase = particlePhase,
                                accentColor   = logoColor
                            )
                        }

                        // Landing burst (behind logo, at ground level)
                        Canvas(
                            modifier = Modifier
                                .size(width = 180.dp, height = 60.dp)
                                .align(Alignment.BottomCenter)
                        ) {
                            drawLandingBurst(
                                phase      = burstPhase,
                                color      = logoColor
                            )
                        }

                        // Ground shadow ellipse
                        Canvas(
                            modifier = Modifier
                                .size(width = 120.dp, height = 24.dp)
                                .align(Alignment.BottomCenter)
                        ) {
                            drawOval(
                                color = logoColor.copy(alpha = shadowAlpha),
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

                        // The 3D "K" letter on Canvas — bouncing + flipping + tilting
                        Canvas(
                            modifier = Modifier
                                .size(120.dp)
                                .align(Alignment.TopCenter)
                        ) {
                            translate(top = bounceY) {
                                // Y-axis perspective flip via scaleX sign
                                scale(scaleX = effectiveScaleX, scaleY = scaleY) {
                                    rotate(degrees = tiltDeg) {
                                        drawAnimatedK(
                                            color   = logoColor,
                                            // pass flipCos so depth layers can be mirrored
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
                                targetValue  = percent,
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
                                progress = percent / 100f,
                                shimmer  = shimmerOffset,
                                color    = logoColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp)
                            )

                            // Animated percentage counter in logo's cycling colour
                            Text(
                                text  = "$displayPercent%",
                                color = logoColor,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            // File name fades in when available
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
            } // inner padding Box
        }
    }
}

// ── Draw the bold 3D "K" letter ───────────────────────────────────────────────
// flipCos: cosine of current flip angle (1→-1→1); used to mirror depth offset
// so the 3D depth layer stays on the correct "side" during a Y-axis flip.
private fun DrawScope.drawAnimatedK(color: Color, flipCos: Float) {
    val w = size.width
    val h = size.height
    val stroke = w * 0.19f

    // Depth offset follows the flip so it always looks like it's on the back face
    val depthSign   = if (flipCos >= 0f) 1f else -1f
    val depthOffset = w * 0.055f * depthSign

    val depthColor = Color(
        red   = (color.red   * 0.45f).coerceIn(0f, 1f),
        green = (color.green * 0.45f).coerceIn(0f, 1f),
        blue  = (color.blue  * 0.45f).coerceIn(0f, 1f),
        alpha = 0.75f
    )

    drawIntoCanvas { canvas ->
        val paint = Paint().apply { isAntiAlias = true }
        val fw    = paint.asFrameworkPaint()

        // ── Depth / shadow layer ──────────────────────────────────────────────
        fw.color = depthColor.toArgb()
        fw.setShadowLayer(0f, 0f, 0f, android.graphics.Color.TRANSPARENT)

        canvas.drawRect(w * 0.10f + depthOffset, h * 0.08f + depthOffset,
                        w * 0.10f + stroke + depthOffset, h * 0.92f + depthOffset, paint)
        canvas.drawRect(w * 0.29f + depthOffset, h * 0.08f + depthOffset,
                        w * 0.90f + depthOffset, h * 0.08f + stroke + depthOffset, paint)
        canvas.drawRect(w * 0.29f + depthOffset, h * 0.92f - stroke + depthOffset,
                        w * 0.90f + depthOffset, h * 0.92f + depthOffset, paint)
        canvas.drawRect(w * 0.29f + depthOffset, h * 0.44f + depthOffset,
                        w * 0.29f + stroke + depthOffset, h * 0.56f + depthOffset, paint)

        // ── Main glowing "K" ─────────────────────────────────────────────────
        fw.color = color.toArgb()
        fw.setShadowLayer(w * 0.14f, 0f, 0f, color.copy(alpha = 0.95f).toArgb())

        canvas.drawRect(w * 0.10f, h * 0.08f, w * 0.10f + stroke, h * 0.92f, paint)
        canvas.drawRect(w * 0.29f, h * 0.08f, w * 0.90f, h * 0.08f + stroke, paint)
        canvas.drawRect(w * 0.29f, h * 0.92f - stroke, w * 0.90f, h * 0.92f, paint)
        canvas.drawRect(w * 0.29f, h * 0.44f, w * 0.29f + stroke, h * 0.56f, paint)

        // ── Top-left highlight edge (3D plastic shine) ────────────────────────
        fw.color = Color.White.copy(alpha = 0.38f).toArgb()
        fw.setShadowLayer(0f, 0f, 0f, android.graphics.Color.TRANSPARENT)

        canvas.drawRect(w * 0.10f, h * 0.08f,
                        w * 0.10f + stroke * 0.22f, h * 0.52f, paint)
        canvas.drawRect(w * 0.29f, h * 0.08f,
                        w * 0.68f, h * 0.08f + stroke * 0.22f, paint)
    }
}

// ── 10 particles drifting upward around the logo ─────────────────────────────
private fun DrawScope.drawFloatingParticles(particlePhase: Float, accentColor: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f

    // (x-offset, radius, color index)
    val particles = listOf(
        Triple(-62f,  5f, 0),
        Triple( 58f,  4f, 1),
        Triple(-38f,  6f, 2),
        Triple( 44f,  3f, 3),
        Triple(-70f,  4f, 4),
        Triple( 22f,  5f, 5),
        Triple(-22f,  3f, 6),
        Triple( 68f,  6f, 7),
        Triple(-48f,  4f, 8),
        Triple( 36f,  5f, 9),
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
// phase: 0 = just landed, 1 = fully expanded/faded
private fun DrawScope.drawLandingBurst(phase: Float, color: Color) {
    if (phase <= 0f || phase >= 1f) return

    val cx = size.width  / 2f
    val cy = size.height * 0.25f   // slightly above the true ground line
    val maxRadius = size.width * 0.45f
    val spikes    = 8
    val alpha     = (1f - phase).coerceIn(0f, 1f) * 0.85f

    for (i in 0 until spikes) {
        val angle = (i * (2f * PI / spikes)).toFloat()
        val r     = maxRadius * phase

        // Spike base width shrinks as it expands (sharp tip)
        val perpAngle = angle + PI.toFloat() / 2f
        val baseW     = 6f * (1f - phase * 0.8f)

        val tipX   = cx + cos(angle) * r
        val tipY   = cy + sin(angle) * r
        val baseX1 = cx + cos(perpAngle) * baseW
        val baseY1 = cy + sin(perpAngle) * baseW
        val baseX2 = cx - cos(perpAngle) * baseW
        val baseY2 = cy - sin(perpAngle) * baseW

        val path = Path().apply {
            moveTo(tipX, tipY)
            lineTo(baseX1, baseY1)
            lineTo(baseX2, baseY2)
            close()
        }

        drawPath(
            path  = path,
            color = color.copy(alpha = alpha),
            style = Fill
        )
    }

    // Small centre circle that pops then fades
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

        // Track (dark pill)
        drawRoundRect(
            color        = Color(0xFF1A2540),
            size         = size,
            cornerRadius = CornerRadius(r)
        )

        if (animatedProgress > 0f) {
            val fillW = w * animatedProgress.coerceIn(0f, 1f)

            // Glow halo
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

            // Gradient fill — cycles with the logo colour
            drawRoundRect(
                brush  = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(KhwabBlue, color, KhwabViolet),
                    startX = 0f,
                    endX   = fillW
                ),
                size   = Size(fillW, h),
                cornerRadius = CornerRadius(r)
            )

            // Shimmer stripe
            val stripeW = fillW * 0.38f
            val stripeX = (fillW + stripeW) * shimmer - stripeW
            drawRoundRect(
                brush  = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.50f),
                        Color.Transparent
                    ),
                    startX = stripeX,
                    endX   = stripeX + stripeW
                ),
                size   = Size(fillW, h),
                cornerRadius = CornerRadius(r)
            )

            // 3D top-edge gloss highlight
            drawRoundRect(
                brush  = androidx.compose.ui.graphics.Brush.horizontalGradient(
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
    val a = colors[idx]
    val b = colors[idx + 1]
    return Color(
        red   = a.red   + (b.red   - a.red)   * frac,
        green = a.green + (b.green - a.green) * frac,
        blue  = a.blue  + (b.blue  - a.blue)  * frac,
        alpha = 1f
    )
}
