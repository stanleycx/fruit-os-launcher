package com.stanleycx.fruitos.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

enum class GlassLevel {
    Clear,     // Verre transparent — aucun flou, tint seule
    Thin,      // Très fin (petits éléments)
    Regular,   // Standard (dossiers, dock)
    Thick,     // Plus profond
    Ultra      // Très profond (context menus, overlays)
}

/**
 * Niveaux d'effet loupe — zoom réel du fond derrière le verre.
 * @param factor facteur de zoom (1.0 = aucun zoom, 2.0 = 2× zoom, etc.)
 */
enum class LoupeLevel(val factor: Float) {
    None(1.0f),
    Light(1.2f),    // subtil, par défaut recommandé (≈ x1.2, ne dégrade pas le glass)
    Medium(1.5f),
    Strong(1.9f),
    Ultra(2.4f)
}

/** Niveaux de brillance — reflet lumineux en haut des éléments verre (rendu sous le contenu enfant). */
enum class GlossLevel {
    None,
    Subtle,
    Medium,
    High,
    Prismatic
}

fun glassTintColor(glassTint: GlassTint, customTintColor: Color? = null): Color {
    return when {
        glassTint == GlassTint.Custom && customTintColor != null -> customTintColor
        else -> when (glassTint) {
            GlassTint.None    -> Color(0xE8ECF0)
            GlassTint.White   -> Color.White
            GlassTint.Black   -> Color.Black
            GlassTint.Gray    -> Color(0xFF888888)
            GlassTint.Blue    -> Color(0xFF5B9BD5)
            GlassTint.Warm    -> Color(0xFFE8C39E)
            GlassTint.Cool    -> Color(0xFF9ECAE8)
            GlassTint.Purple  -> Color(0xFF9B59B6)
            GlassTint.Pink    -> Color(0xFFFF69B4)
            GlassTint.Green   -> Color(0xFF2ECC71)
            GlassTint.Orange  -> Color(0xFFE67E22)
            GlassTint.Teal    -> Color(0xFF1ABC9C)
            GlassTint.Red     -> Color(0xFFE74C3C)
            GlassTint.Gold    -> Color(0xFFF1C40F)
            GlassTint.Magenta -> Color(0xFFC0392B)
            GlassTint.Custom  -> Color(0xE8ECF0)
        }
    }
}

fun backgroundBlurFor(level: GlassLevel): Dp = when (level) {
    GlassLevel.Clear   -> 0.dp
    GlassLevel.Thin    -> 8.dp
    GlassLevel.Regular -> 16.dp
    GlassLevel.Thick   -> 24.dp
    GlassLevel.Ultra   -> 34.dp
}

fun progressiveBackgroundBlurFor(level: GlassLevel): Dp = when (level) {
    GlassLevel.Clear   -> 0.dp
    GlassLevel.Thin    -> 18.dp
    GlassLevel.Regular -> 26.dp
    GlassLevel.Thick   -> 34.dp
    GlassLevel.Ultra   -> 42.dp
}

/**
 * Matériau de verre réutilisable style Fruit OS.
 *
 * Le gloss (brillance) est toujours rendu *sous* le contenu enfant (icônes, texte, etc.)
 * pour que la brillance fasse partie du fond verre et ne recouvre pas les apps.
 *
 * @param loupeLevel Zoom réel du fond : échantillonne une zone réduite du wallpaper et
 *   l'agrandit pour remplir l'élément → vrai effet loupe (pas juste du blur en plus).
 * @param glossLevel Reflet lumineux du matériau verre (None = aucun). Dessiné sous les enfants.
 */
@Composable
fun Modifier.glass(
    hazeState: HazeState,
    level: GlassLevel = GlassLevel.Regular,
    glassTint: GlassTint = GlassTint.None,
    customTintColor: Color? = null,
    shape: Shape = RoundedCornerShape(36.dp),
    showBorder: Boolean = true,
    loupeLevel: LoupeLevel = LoupeLevel.None,
    glossLevel: GlossLevel = GlossLevel.None
): Modifier {

    val wallpaperBitmap = LocalWallpaperBitmap.current
    val isLoupeActive = loupeLevel != LoupeLevel.None && wallpaperBitmap != null

    // HazeState vide : utilisé en mode loupe pour que hazeEffect soit neutre
    // (toujours appelé pour stabilité de la composition @Composable)
    val emptyHazeState = remember { HazeState() }
    val activeHazeState = if (isLoupeActive) emptyHazeState else hazeState

    // Position et taille de cet élément sur l'écran (remplis au premier layout)
    val elementPos = remember { mutableStateOf(Offset.Zero) }
    val elementSize = remember { mutableStateOf(IntSize.Zero) }

    // Métriques d'écran pour le mapping bitmap ↔ pixels écran
    val displayMetrics = LocalContext.current.resources.displayMetrics
    val screenW = displayMetrics.widthPixels.toFloat()
    val screenH = displayMetrics.heightPixels.toFloat()

    val (baseBlur, tintAlpha) = when (level) {
        GlassLevel.Clear   -> Pair(0.5.dp, if (glassTint == GlassTint.None) 0.05f else 0.12f)
        GlassLevel.Thin    -> Pair(backgroundBlurFor(GlassLevel.Thin) + 5.dp, 0.08f)
        GlassLevel.Regular -> Pair(backgroundBlurFor(GlassLevel.Regular) + 7.dp, 0.13f)
        GlassLevel.Thick   -> Pair(backgroundBlurFor(GlassLevel.Thick) + 9.dp, 0.18f)
        GlassLevel.Ultra   -> Pair(backgroundBlurFor(GlassLevel.Ultra) + 11.dp, 0.24f)
    }
    val effectiveBlur = if (level == GlassLevel.Clear) 0.5.dp else baseBlur
    val noiseAmount = if (level == GlassLevel.Clear) 0.01f else 0.035f

    val tintColor = glassTintColor(glassTint, customTintColor)
    val baseTint = HazeTint(tintColor.copy(alpha = tintAlpha))

    // Modifier de suivi de position (utilisé en mode loupe uniquement)
    val positionTracker = if (isLoupeActive) {
        Modifier.onGloballyPositioned { coords ->
            val newPos = coords.positionInRoot()
            val newSize = coords.size
            if (newPos != elementPos.value) elementPos.value = newPos
            if (newSize != elementSize.value) elementSize.value = newSize
        }
    } else Modifier

    // Teinte du verre en mode loupe : le flou réel (couche dédiée ci-dessous) fournit déjà
    // l'épaisseur, donc on garde la MÊME teinte que le verre normal (pas de sur-boost).
    val glassThicknessOverlay: Color =
        if (isLoupeActive) tintColor.copy(alpha = tintAlpha) else Color.Transparent

    // Couche backdrop dédiée au mode loupe : on y enregistre le fond AGRANDI puis on lui
    // applique un VRAI flou (BlurEffect). Toujours obtenue (règle des hooks) même hors loupe.
    val loupeLayer = rememberGraphicsLayer()

    // Modifier de zoom réel : recadre une zone réduite du wallpaper et l'agrandit, PUIS la
    // floute via la couche dédiée → l'épaisseur du verre (glass thickness) est conservée.
    // Tout est dans drawBehind → s'exécute TOUJOURS derrière les enfants (icônes/texte nets).
    val loupeMod = if (isLoupeActive) {
        // isLoupeActive garantit que wallpaperBitmap != null
        val bmp = wallpaperBitmap!!
        val zoom = loupeLevel.factor
        Modifier.drawBehind {
            val sz = elementSize.value
            if (sz.width == 0 || sz.height == 0) return@drawBehind

            val pos = elementPos.value
            val elemW = sz.width.toFloat()
            val elemH = sz.height.toFloat()

            // Zone à recadrer dans l'espace écran (pixels) : centrée sur l'élément, plus petite → zoom
            val cropW = elemW / zoom
            val cropH = elemH / zoom
            val cropX = pos.x + (elemW - cropW) / 2f
            val cropY = pos.y + (elemH - cropH) / 2f

            // Mapping écran → bitmap via ContentScale.Crop
            val bmpW = bmp.width.toFloat()
            val bmpH = bmp.height.toFloat()
            val scale = maxOf(screenW / bmpW, screenH / bmpH)
            val cropOffsetX = (bmpW * scale - screenW) / 2f
            val cropOffsetY = (bmpH * scale - screenH) / 2f

            val bmpCropX = ((cropX + cropOffsetX) / scale).toInt().coerceIn(0, bmp.width)
            val bmpCropY = ((cropY + cropOffsetY) / scale).toInt().coerceIn(0, bmp.height)
            val bmpCropW = (cropW / scale).toInt().coerceAtLeast(1).coerceAtMost(bmp.width - bmpCropX)
            val bmpCropH = (cropH / scale).toInt().coerceAtLeast(1).coerceAtMost(bmp.height - bmpCropY)

            // 1. Enregistre le fond zoomé dans la couche dédiée…
            loupeLayer.record(this, this.layoutDirection, IntSize(sz.width, sz.height)) {
                drawImage(
                    image = bmp,
                    srcOffset = IntOffset(bmpCropX, bmpCropY),
                    srcSize = IntSize(bmpCropW, bmpCropH),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(sz.width, sz.height),
                    filterQuality = FilterQuality.High
                )
            }
            // 2. …puis applique le VRAI flou de verre à CETTE couche seulement.
            val blurPx = effectiveBlur.toPx()
            loupeLayer.renderEffect =
                if (blurPx > 0.5f) BlurEffect(blurPx, blurPx, TileMode.Clamp) else null
            drawLayer(loupeLayer)

            // 3. Overlay épaisseur/teinte verre (par-dessus le fond zoomé+flouté, sous les enfants).
            drawRect(color = glassThicknessOverlay)
        }
    } else Modifier

    val borderModifier = if (showBorder) {
        Modifier.border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.52f),
                    Color.White.copy(alpha = 0.16f),
                    Color.White.copy(alpha = 0.05f),
                    Color.White.copy(alpha = 0.11f)
                )
            ),
            shape = shape
        )
    } else Modifier

    // En mode loupe : loupeMod (drawBehind = derrière enfants) + hazeEffect neutre (emptyHazeState)
    // En mode normal : hazeEffect réel (blur + tint)
    // Gloss (si présent) est aussi dessiné avant drawContent() → brillance sous les enfants.
    val withEffect = this
        .clip(shape)
        .then(positionTracker)
        .then(loupeMod)
        .hazeEffect(state = activeHazeState) {
            blurRadius = effectiveBlur
            tints = listOf(baseTint)
            noiseFactor = noiseAmount
        }
        .then(borderModifier)

    return if (glossLevel == GlossLevel.None) withEffect
    else withEffect.drawWithContent {
        // Brillance / gloss : dessinée AVANT drawContent() pour qu'elle fasse partie
        // du matériau de verre (sous les icônes, labels, contenu enfant).
        // Avant : elle était après → shine passait par-dessus les apps (surtout visible dans le Dock).
        applyGloss(glossLevel)
        drawContent()
    }
}

@Composable
fun Modifier.simpleGlass(
    hazeState: HazeState,
    level: GlassLevel = GlassLevel.Regular,
    glassTint: GlassTint = GlassTint.None,
    customTintColor: Color? = null,
    shape: Shape = RoundedCornerShape(36.dp),
    loupeLevel: LoupeLevel = LoupeLevel.None,
    glossLevel: GlossLevel = GlossLevel.None
): Modifier = glass(
    hazeState = hazeState,
    level = level,
    glassTint = glassTint,
    customTintColor = customTintColor,
    shape = shape,
    showBorder = false,
    loupeLevel = loupeLevel,
    glossLevel = glossLevel
)

private fun DrawScope.applyGloss(glossLevel: GlossLevel) {
    if (glossLevel == GlossLevel.None) return
    if (glossLevel == GlossLevel.Prismatic) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFF6B9D).copy(alpha = 0.22f),
                    Color(0xFFFFE66D).copy(alpha = 0.18f),
                    Color(0xFF48DBFB).copy(alpha = 0.16f),
                    Color(0xFF45B7D1).copy(alpha = 0.12f),
                    Color(0xFFFF9FF3).copy(alpha = 0.08f),
                    Color.Transparent
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height * 0.65f)
            )
        )
    } else {
        val topAlpha = when (glossLevel) {
            GlossLevel.Subtle -> 0.10f
            GlossLevel.Medium -> 0.20f
            GlossLevel.High   -> 0.36f
            else              -> 0f
        }
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = topAlpha),
                    Color.White.copy(alpha = topAlpha * 0.28f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = size.height * 0.52f
            )
        )
    }
}

enum class GlassTint {
    None,
    White,
    Black,
    Gray,
    Blue,
    Warm,
    Cool,
    Purple,
    Pink,
    Green,
    Orange,
    Teal,
    Red,
    Gold,
    Magenta,
    Custom
}
