package ru.brifi.app.ui

import android.content.Context
import android.graphics.Typeface
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import ru.brifi.app.R

/**
 * Точная палитра из Figma-макета «Главный экран» и Брифи v2.pdf
 */
object BrifiColors {
    val BgBlack = Color(0xFF000000)
    val TileDark = Color(0xFF0E0E0E)
    val TileBorder = Color(0xFF414141)

    // Секции главного экрана («В работе:», «Другие задачи:», «Last but not least»)
    val SheetHome = Color(0xFF1A1A1A)
    // Секции Профиля и Статистики (clip2_0_1 / clip3_0_1)
    val SheetDark = Color(0xFF2C2C2C)
    val SheetDarkBorder = Color(0xFF3B3B3B)

    // Карточка «В работе»
    val WorkCardDark = Color(0xFF2C2C2C)
    val WorkCardBorder = Color(0xFF606060)

    val CardDark = Color(0xFF141414)
    val CardDarkElevated = Color(0xFF1A1A1A)

    // Акценты и аура из SVG
    val GlowBlue = Color(0xFF4638FF)
    val AccentIndigo = Color(0xFF6C61FF)
    val AccentIndigoButton = Color(0xFF6C61FE)
    val AccentIndigoActive = Color(0xFF6157E5)
    val AccentIndigoStroke = Color(0xFF4638FF)
    val AccentIndigoSoft = Color(0xFF6C61FF)
    val AccentDeepBlue = Color(0xFF0E04AC)
    val AccentCyan = Color(0xFF61CDFF)
    val AccentCyanTile = Color(0xFF54B0DB)
    val AccentCyanLight = Color(0xFF61CDFF)
    val AccentSoftBlue = Color(0xFFC1BCFF)

    // Карточки «Другие задачи»
    val DeckFrontLight = Color(0xFFE7E7E7)
    val DeckBackGray = Color(0xFFE7E7E7)
    val ScrollRail = Color(0xFFD8D8E0)
    val ScrollRailIcon = Color(0xFF737373)
    val StatsCardLight = Color(0xFFE7E7E7)

    // Плитки Статистики по макету
    val KpiTile1 = Color(0xFF0E04AC)
    val KpiTile2 = Color(0xFF6C61FF)
    val KpiTile3 = Color(0xFF50A5D1)
    val KpiTile4 = Color(0xFFE7E7E7)

    val ProfileTileDark = Color(0xFF0E0E0E)
    val ProfileDashedBorder = Color(0xFF5A5A66)

    // Нижняя стеклянная панель навигации
    val NavPillBg = Color(0xCC0D0D10)
    val NavPillBorder = Color(0x26FFFFFF)
    val NavActivePillBg = Color(0x1EFFFFFF)
    val NavActivePillBorder = Color(0x26FFFFFF)
    val NavActive = Color(0xFFFFFFFF)
    val NavInactive = Color(0xFFA2A2A2)
    val SegmentDark = Color(0xFF1A1A1A)

    val TextWhite = Color(0xFFFFFFFF)
    val TextBlack = Color(0xFF000000)
    val TextDarkSecondary = Color(0xFF2C2C2C)
    val TextMutedLight = Color(0xFFE1E1E1)
    val TextMutedDark = Color(0xFF737373)
    val TextMetaDark = Color(0xFF3B3B3B)
    val TextFooterMuted = Color(0x82E1E1E1)

    val UrgencyRed = Color(0xFFE53935)
    val UrgencyYellow = Color(0xFFFFC107)
    val UrgencyGreen = Color(0xFF2E9E5B)
}

fun urgencyBadgeColors(urgency: String): Pair<Color, Color> {
    return when (urgency.trim()) {
        "!!!" -> BrifiColors.UrgencyRed to BrifiColors.TextWhite
        "!!" -> BrifiColors.UrgencyYellow to BrifiColors.TextBlack
        else -> BrifiColors.UrgencyGreen to BrifiColors.TextWhite
    }
}

val GoogleSansFamily: FontFamily = runCatching {
    FontFamily(Typeface.create("google-sans", Typeface.NORMAL))
}.getOrDefault(FontFamily.SansSerif)

val A24FontFamily: FontFamily = FontFamily(
    Font(R.font.a24_regular)
)

private val BaseTypography = Typography()

val BrifiTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = GoogleSansFamily),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = GoogleSansFamily),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = GoogleSansFamily),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = GoogleSansFamily),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = GoogleSansFamily),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = GoogleSansFamily),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = GoogleSansFamily),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = GoogleSansFamily),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = GoogleSansFamily),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = GoogleSansFamily),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = GoogleSansFamily),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = GoogleSansFamily),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = GoogleSansFamily),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = GoogleSansFamily),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = GoogleSansFamily)
)

@Composable
fun BrifiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = BrifiColors.AccentIndigo,
            secondary = BrifiColors.AccentCyan,
            background = BrifiColors.BgBlack,
            surface = BrifiColors.SheetHome,
            onPrimary = BrifiColors.TextWhite,
            onBackground = BrifiColors.TextWhite,
            onSurface = BrifiColors.TextWhite
        ),
        typography = BrifiTypography,
        content = content
    )
}

fun triggerBrifiHaptic(context: Context) {
    runCatching {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        val vibrator = vm?.defaultVibrator ?: return
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        }
    }
}

fun triggerBrifiConfirmHaptic(context: Context) {
    runCatching {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        val vibrator = vm?.defaultVibrator ?: return
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        }
    }
}

/**
 * Плавный, медленный «дышащий» 3-сферный градиент по точным координатам SVG:
 * Left #4638FF, Center #6C61FF, Right #61CDFF. Работает на GPU без рывков.
 */
@Composable
fun BreathingAuroraCanvas(
    primaryGlow: Color = BrifiColors.GlowBlue,
    centerGlow: Color = BrifiColors.AccentIndigo,
    secondaryGlow: Color = BrifiColors.AccentCyan,
    showBottomGlow: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BreathingAurora")
    val breathA by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathA"
    )
    val breathB by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathB"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // 1. Левая сфера (#4638FF) — Figma (183.5 / 1080 ≈ 0.17)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryGlow.copy(alpha = 0.72f + breathA * 0.16f),
                    primaryGlow.copy(alpha = 0.28f + breathB * 0.08f),
                    Color.Transparent
                ),
                center = Offset(
                    x = w * (0.16f + breathA * 0.08f),
                    y = h * (0.10f + breathB * 0.06f)
                ),
                radius = w * (0.68f + breathA * 0.10f)
            )
        )

        // 2. Центральная сфера (#6C61FF) — Figma (505.5 / 1080 ≈ 0.47)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    centerGlow.copy(alpha = 0.66f + breathB * 0.16f),
                    centerGlow.copy(alpha = 0.24f + breathA * 0.08f),
                    Color.Transparent
                ),
                center = Offset(
                    x = w * (0.48f + (breathB - 0.5f) * 0.10f),
                    y = h * (0.04f + breathA * 0.05f)
                ),
                radius = w * (0.64f + breathB * 0.10f)
            )
        )

        // 3. Правая сфера (#61CDFF) — Figma (899.5 / 1080 ≈ 0.83)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondaryGlow.copy(alpha = 0.64f + breathB * 0.18f),
                    secondaryGlow.copy(alpha = 0.22f + breathA * 0.08f),
                    Color.Transparent
                ),
                center = Offset(
                    x = w * (0.84f - breathA * 0.08f),
                    y = h * (0.14f + breathB * 0.06f)
                ),
                radius = w * (0.62f + breathB * 0.10f)
            )
        )

        if (showBottomGlow) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        centerGlow.copy(alpha = 0.68f + breathB * 0.14f),
                        secondaryGlow.copy(alpha = 0.28f + breathA * 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(
                        x = w * (0.50f + (breathA - 0.5f) * 0.14f),
                        y = h * (0.94f - breathB * 0.04f)
                    ),
                    radius = w * (0.90f + breathA * 0.12f)
                )
            )
        }
    }
}

@Composable
fun BrifiLogoImage(
    height: Dp = 26.dp,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.brifi_logo_v2),
        contentDescription = "Брифи",
        contentScale = ContentScale.FillHeight,
        modifier = modifier.height(height)
    )
}

@Composable
fun BrifiBreathingHeader(
    centered: Boolean,
    primaryGlow: Color = BrifiColors.GlowBlue,
    centerGlow: Color = BrifiColors.AccentIndigo,
    secondaryGlow: Color = BrifiColors.AccentCyan,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(BrifiColors.BgBlack)
    ) {
        BreathingAuroraCanvas(
            primaryGlow = primaryGlow,
            centerGlow = centerGlow,
            secondaryGlow = secondaryGlow,
            showBottomGlow = false,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            contentAlignment = if (centered) Alignment.Center else Alignment.CenterStart
        ) {
            BrifiLogoImage(height = 24.dp)
        }
    }
}

/**
 * Точная векторная иконка кнопки «Создать задачу» из Figma:
 * толстый белый скруглённый прямоугольник + фиолетовый круг #6C61FF с белым плюсом в правом нижнем углу.
 */
@Composable
fun CreateTaskActionIcon(modifier: Modifier = Modifier.size(width = 56.dp, height = 38.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = w * 0.10f
        val rectW = w * 0.84f
        val rectH = h * 0.80f
        val left = w * 0.05f
        val top = h * 0.06f

        drawRoundRect(
            color = BrifiColors.TextWhite,
            topLeft = Offset(left, top),
            size = Size(rectW, rectH),
            cornerRadius = CornerRadius(h * 0.24f, h * 0.24f),
            style = Stroke(width = strokeW)
        )

        val badgeRadius = h * 0.26f
        val badgeCenter = Offset(left + rectW - badgeRadius * 0.22f, top + rectH - badgeRadius * 0.18f)
        drawCircle(
            color = BrifiColors.AccentIndigo,
            radius = badgeRadius,
            center = badgeCenter
        )
        val plusHalf = badgeRadius * 0.48f
        val plusStroke = badgeRadius * 0.26f
        drawLine(
            color = BrifiColors.TextWhite,
            start = Offset(badgeCenter.x - plusHalf, badgeCenter.y),
            end = Offset(badgeCenter.x + plusHalf, badgeCenter.y),
            strokeWidth = plusStroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = BrifiColors.TextWhite,
            start = Offset(badgeCenter.x, badgeCenter.y - plusHalf),
            end = Offset(badgeCenter.x, badgeCenter.y + plusHalf),
            strokeWidth = plusStroke,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Точная иконка кнопки «Сохранить макет» из Figma:
 * жирные белые буквы URL + фиолетовая стрелка-галочка вверх (#6C61FF) в правом нижнем углу.
 */
@Composable
fun SaveUrlActionIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.height(38.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "URL",
            color = BrifiColors.TextWhite,
            fontSize = 27.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.padding(end = 10.dp)
        )
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 3.dp)
                .size(16.dp)
        ) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.14f, h * 0.72f)
                lineTo(w * 0.50f, h * 0.24f)
                lineTo(w * 0.86f, h * 0.72f)
            }
            drawPath(
                path = path,
                color = BrifiColors.AccentIndigo,
                style = Stroke(width = w * 0.28f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

/**
 * Точные тройные векторные шевроны >>> для слайдеров (Figma stroke #6C61FF)
 */
@Composable
fun TripleChevronsIcon(
    color: Color,
    modifier: Modifier = Modifier.size(width = 28.dp, height = 14.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = h * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val step = w * 0.28f
        val chevW = w * 0.22f
        for (i in 0..2) {
            val x0 = w * 0.08f + i * step
            val path = Path().apply {
                moveTo(x0, h * 0.15f)
                lineTo(x0 + chevW, h * 0.50f)
                lineTo(x0, h * 0.85f)
            }
            drawPath(path = path, color = color, style = stroke)
        }
    }
}

/**
 * Иконка «Домой» из Figma — скруглённый домик со стилизованным вырезом «Б» внутри
 */
@Composable
fun NavHomeIcon(color: Color, cutoutColor: Color = BrifiColors.BgBlack, modifier: Modifier = Modifier.size(26.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val housePath = Path().apply {
            moveTo(w * 0.50f, h * 0.10f)
            cubicTo(w * 0.54f, h * 0.10f, w * 0.86f, h * 0.36f, w * 0.86f, h * 0.44f)
            lineTo(w * 0.86f, h * 0.80f)
            cubicTo(w * 0.86f, h * 0.88f, w * 0.80f, h * 0.90f, w * 0.72f, h * 0.90f)
            lineTo(w * 0.28f, h * 0.90f)
            cubicTo(w * 0.20f, h * 0.90f, w * 0.14f, h * 0.88f, w * 0.14f, h * 0.80f)
            lineTo(w * 0.14f, h * 0.44f)
            cubicTo(w * 0.14f, h * 0.36f, w * 0.46f, h * 0.10f, w * 0.50f, h * 0.10f)
            close()
        }
        drawPath(housePath, color = color)

        // Стилизованный вырез «Б» внутри домика как на макете
        val bStroke = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val bPath = Path().apply {
            moveTo(w * 0.62f, h * 0.46f)
            lineTo(w * 0.40f, h * 0.46f)
            lineTo(w * 0.40f, h * 0.74f)
            lineTo(w * 0.56f, h * 0.74f)
            cubicTo(w * 0.65f, h * 0.74f, w * 0.66f, h * 0.58f, w * 0.56f, h * 0.58f)
            lineTo(w * 0.40f, h * 0.58f)
        }
        drawPath(bPath, color = cutoutColor, style = bStroke)
    }
}

/**
 * Иконка «Статистика» из Figma — 3 скруглённых столбца с точными пропорциями высот 53 / 80 / 40
 */
@Composable
fun NavStatsIcon(color: Color, modifier: Modifier = Modifier.size(26.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barW = w * 0.19f
        val r = CornerRadius(barW / 2f, barW / 2f)
        val bottom = h * 0.88f
        val h1 = h * 0.48f
        val h2 = h * 0.74f
        val h3 = h * 0.37f

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.13f, bottom - h1),
            size = Size(barW, h1),
            cornerRadius = r
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.405f, bottom - h2),
            size = Size(barW, h2),
            cornerRadius = r
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.68f, bottom - h3),
            size = Size(barW, h3),
            cornerRadius = r
        )
    }
}

@Composable
fun NavProfileIcon(color: Color, modifier: Modifier = Modifier.size(26.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(
            color = color,
            radius = w * 0.18f,
            center = Offset(w * 0.50f, h * 0.30f)
        )
        val bodyPath = Path().apply {
            moveTo(w * 0.16f, h * 0.86f)
            cubicTo(
                w * 0.16f, h * 0.54f,
                w * 0.84f, h * 0.54f,
                w * 0.84f, h * 0.86f
            )
            close()
        }
        drawPath(bodyPath, color = color)
    }
}

@Composable
fun PlusIcon(color: Color, strokeFraction: Float = 0.12f, modifier: Modifier = Modifier.size(18.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * strokeFraction
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.16f),
            end = Offset(w * 0.5f, h * 0.84f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.16f, h * 0.5f),
            end = Offset(w * 0.84f, h * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}
