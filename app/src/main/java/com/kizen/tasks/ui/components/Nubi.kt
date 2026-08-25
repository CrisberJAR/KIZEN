package com.kizen.tasks.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kizen.tasks.ui.theme.CreamYellow
import com.kizen.tasks.ui.theme.Mint
import com.kizen.tasks.ui.theme.PinkSoft
import com.kizen.tasks.ui.theme.Sky
import com.kizen.tasks.ui.theme.TextMain
import kotlin.math.cos
import kotlin.math.sin

enum class NubiMood { Idle, Happy, Sleepy, Party, Think }

@Composable
fun Nubi(
    mood: NubiMood,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
) {
    val motion = rememberInfiniteTransition(label = "nubi")
    val bob by motion.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bob",
    )
    val blink by motion.animateFloat(
        initialValue = 1f,
        targetValue = if (mood == NubiMood.Sleepy) 0.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (mood == NubiMood.Sleepy) 900 else 4200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blink",
    )
    val sparkle by motion.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "sparkle",
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f + bob

        fun star(center: Offset, r: Float, color: Color) {
            val path = Path()
            for (i in 0 until 10) {
                val angle = Math.toRadians(-90.0 + i * 36.0)
                val radius = if (i % 2 == 0) r else r * 0.45f
                val x = center.x + (cos(angle) * radius).toFloat()
                val y = center.y + (sin(angle) * radius).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, color)
        }

        if (mood == NubiMood.Party || mood == NubiMood.Happy) {
            star(Offset(cx - w * 0.38f, cy - h * 0.28f), 10f * sparkle, CreamYellow)
            star(Offset(cx + w * 0.4f, cy - h * 0.18f), 8f * sparkle, PinkSoft)
            star(Offset(cx + w * 0.32f, cy + h * 0.28f), 7f * sparkle, Mint)
        }

        val cloud = Color(0xFFFFFBFE)
        val puff = Color(0xFFEAF6FF)
        drawCircle(puff, radius = w * 0.22f, center = Offset(cx - w * 0.18f, cy + h * 0.04f))
        drawCircle(cloud, radius = w * 0.28f, center = Offset(cx, cy - h * 0.04f))
        drawCircle(Color(0xFFFFF0F6), radius = w * 0.2f, center = Offset(cx + w * 0.2f, cy + h * 0.05f))
        drawCircle(cloud, radius = w * 0.18f, center = Offset(cx - w * 0.02f, cy + h * 0.12f))

        val eyeY = cy - h * 0.02f
        val eyeH = 14f * blink
        val eyeColor = TextMain
        drawOval(eyeColor, topLeft = Offset(cx - 28f, eyeY - eyeH / 2f), size = Size(12f, eyeH))
        drawOval(eyeColor, topLeft = Offset(cx + 16f, eyeY - eyeH / 2f), size = Size(12f, eyeH))
        if (blink > 0.5f && mood != NubiMood.Sleepy) {
            drawCircle(Color.White, 2.4f, Offset(cx - 21f, eyeY - 3f))
            drawCircle(Color.White, 2.4f, Offset(cx + 23f, eyeY - 3f))
        }

        drawOval(PinkSoft.copy(alpha = 0.85f), topLeft = Offset(cx - 42f, cy + 10f), size = Size(16f, 10f))
        drawOval(PinkSoft.copy(alpha = 0.85f), topLeft = Offset(cx + 26f, cy + 10f), size = Size(16f, 10f))

        val smile = Path()
        when (mood) {
            NubiMood.Sleepy -> {
                smile.moveTo(cx - 10f, cy + 18f)
                smile.quadraticTo(cx, cy + 22f, cx + 10f, cy + 18f)
            }
            NubiMood.Think -> {
                smile.moveTo(cx - 6f, cy + 20f)
                smile.lineTo(cx + 10f, cy + 20f)
            }
            else -> {
                smile.moveTo(cx - 12f, cy + 16f)
                smile.quadraticTo(cx, cy + 30f, cx + 12f, cy + 16f)
            }
        }
        drawPath(smile, TextMain, style = Stroke(width = 3.4f, cap = StrokeCap.Round))

        if (mood == NubiMood.Party) {
            rotate(12f, Offset(cx, cy - h * 0.28f)) {
                val hat = Path().apply {
                    moveTo(cx - 18f, cy - h * 0.18f)
                    lineTo(cx, cy - h * 0.38f)
                    lineTo(cx + 18f, cy - h * 0.18f)
                    close()
                }
                drawPath(hat, Sky)
                drawCircle(PinkSoft, 7f, Offset(cx, cy - h * 0.38f))
            }
        }
    }
}
