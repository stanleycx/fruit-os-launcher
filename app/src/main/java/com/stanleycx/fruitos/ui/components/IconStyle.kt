package com.stanleycx.fruitos.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState

/**
 * Style d'apparence GLOBAL appliqué à toutes les icônes d'apps du launcher.
 * Inspiré des modes d'icônes Fruit OS/26.
 */
enum class IconStyleMode {
    /** Couleurs d'origine de l'app (comportement historique). */
    Default,
    /** Fond d'icône foncé, glyphe conservé. */
    Dark,
    /** Glyphe monochrome (luminance) recoloré par [IconStyle.tintColor] sur fond foncé. */
    Tinted,
    /** Fond d'icône en matériau Fruity Glass + glyphe monochrome clair (« Clear » Fruit OS). */
    Glass
}

/** Rendu du glyphe (logo) en mode Verre. */
enum class GlassGlyphStyle {
    /** Recoloré par la teinte du verre système (« Adapté à la teinte du système »). */
    SystemTint,
    /** Couleurs d'origine de l'app (« Couleur de base »). */
    Original,
    /** Monochrome blanc (« Noir & blanc »). */
    Mono,
    /** Monochrome recoloré par une couleur personnalisée (« Teinte personnalisée »). */
    CustomTint
}

/** Source de la teinte du MATÉRIAU verre des icônes en mode Verre. */
enum class GlassTintSource {
    /** Reprend la teinte du verre système (dossiers/dock). */
    System,
    /** Couleur personnalisée propre aux icônes. */
    Custom
}

data class IconStyle(
    val mode: IconStyleMode = IconStyleMode.Default,
    val tintColor: Color = Color(0xFF5B9BD5),   // couleur par défaut du mode Teinté
    val glassGlyph: GlassGlyphStyle = GlassGlyphStyle.Mono,  // logo en mode Verre (défaut = N&B)
    val glassGlyphOpacity: Float = 0.75f,    // opacité du logo en mode Verre (0..1)
    val glassGlyphBrightness: Float = 1f,    // luminosité du logo en mode Verre (0..2, 1 = normal)
    val glassGlyphTintColor: Color = Color(0xFF5B9BD5),  // couleur du logo si glassGlyph = CustomTint
    val glassTintSource: GlassTintSource = GlassTintSource.System,  // teinte du verre : système ou perso
    val glassCustomTint: Color = Color(0xFF5B9BD5),       // couleur du verre si glassTintSource = Custom
    /** Bordure légère (style verre/dock) sur les icônes, indépendamment du mode. */
    val lightBorder: Boolean = true
)

/** Style courant, fourni au plus haut niveau par HomeScreen (défaut = aucun effet). */
val LocalIconStyle = compositionLocalOf { IconStyle() }

/**
 * HazeState utilisé par le mode Verre des icônes. Null → repli sur un fond translucide teinté
 * (les contextes sans wallpaper/haze, ex. aperçus, ne plantent pas).
 */
val LocalIconHazeState = compositionLocalOf<HazeState?> { null }

/**
 * Paramètres du matériau verre GLOBAL du launcher (mêmes réglages que dossiers/dock).
 * Le mode Verre des icônes les reprend tels quels. Fourni par HomeScreen.
 */
data class GlassMaterial(
    val level: GlassLevel = GlassLevel.Regular,
    val tint: GlassTint = GlassTint.None,
    val customTintColor: Color? = null,
    val loupe: LoupeLevel = LoupeLevel.None,
    val gloss: GlossLevel = GlossLevel.None
)

val LocalGlassMaterial = compositionLocalOf { GlassMaterial() }

/**
 * Personnalisation PAR APP (prioritaire sur le style global). Champs nuls/neutres = on suit le global.
 *  - [bgColor] non nul → fond plein opaque (ignore le mode global pour cette icône).
 *  - [logoColor] non nul → **remplace** complètement la couleur du glyphe par cette teinte,
 *    en utilisant l'alpha/masque de forme de l'icône d'origine (et pas une multiplication luminance).
 *    Cela permet de colorer des logos **noirs** en blanc, bleu, etc. (un "tint" classique ne le peut pas).
 *    REMPLACE le traitement global du glyphe (prioritaire).
 *  - [logoBrightness] ≠ 1 → luminosité du logo (0 = noir, 1 = normal, 2 = clair), appliquée
 *    sur la couleur choisie. Toujours visible quel que soit le mode.
 *  - [logoZoom] → échelle du logo (1 = normal).
 */
data class IconOverride(
    val bgColor: Color? = null,
    val logoColor: Color? = null,
    // Teinte du logo : multiplie la luminance par la couleur (PRÉSERVE les détails/dégradés
    // du logo), contrairement à [logoColor] qui agit comme un pochoir (masque alpha, aplat).
    val logoTint: Color? = null,
    val logoBrightness: Float = 1f,
    val logoZoom: Float = 1f
) {
    val isEmpty: Boolean get() = bgColor == null && logoColor == null && logoTint == null &&
        logoBrightness == 1f && logoZoom == 1f
}

/** Overrides par package, fournis par HomeScreen. */
val LocalIconOverrides = compositionLocalOf { emptyMap<String, IconOverride>() }

/** Fond foncé commun aux modes Sombre et Teinté (gris anthracite Fruit OS).
 *  Léger dégradé vertical pour plus de profondeur (haut un peu plus clair). */
val IconDarkBackground = Color(0xFF1C1C1E)
val IconDarkBackgroundTop = Color(0xFF242428)  // un poil plus clair en haut du dégradé

/** Bordure légère identique à celle des éléments glass et du dock. */
private val lightBorderBrush = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.52f),
        Color.White.copy(alpha = 0.16f),
        Color.White.copy(alpha = 0.05f),
        Color.White.copy(alpha = 0.11f)
    )
)

/**
 * Filtre couleur « monochrome teinté » préservant la luminance :
 * sortie RGB = luminance(R,G,B) × tint{R,G,B}, alpha conservé.
 * luminance = 0.21·R + 0.72·G + 0.07·B (pondération perceptuelle).
 */
fun monochromeTintFilter(tint: Color, alpha: Float = 1f, brightness: Float = 1f): ColorFilter {
    val lr = 0.21f; val lg = 0.72f; val lb = 0.07f
    // Luminosité = facteur multiplicatif sur la contribution RGB.
    val r = tint.red * brightness; val g = tint.green * brightness; val b = tint.blue * brightness
    val matrix = ColorMatrix(
        floatArrayOf(
            lr * r, lg * r, lb * r, 0f,    0f,
            lr * g, lg * g, lb * g, 0f,    0f,
            lr * b, lg * b, lb * b, 0f,    0f,
            0f,     0f,     0f,     alpha, 0f
        )
    )
    return ColorFilter.colorMatrix(matrix)
}

/** Filtre qui conserve les couleurs d'origine mais module luminosité (×RGB) et alpha. */
fun originalGlyphFilter(alpha: Float = 1f, brightness: Float = 1f): ColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            brightness, 0f,         0f,         0f,    0f,
            0f,         brightness, 0f,         0f,    0f,
            0f,         0f,         brightness, 0f,    0f,
            0f,         0f,         0f,         alpha, 0f
        )
    )
)

/**
 * Filtre qui "recolore" le glyphe en utilisant son masque alpha (la forme de l'icône d'origine).
 *
 * - On ignore complètement la couleur d'origine du bitmap (y compris le noir).
 * - Les zones opaques du logo (alpha > 0) sont peintes avec [tint] × brightness.
 * - L'alpha original est conservé (anti-aliasing, contours fins).
 *
 * C'est **différent** d'un "tint" classique (multiplication/luminance) :
 * un tint classique ne peut pas éclaircir le noir (0 × n'importe quoi = 0).
 * Ici on remplace la couleur du logo par la teinte choisie, en se servant de l'alpha comme pochoir.
 *
 * Utilisé pour :
 *  - les overrides individuels "logoColor" (Personnaliser l'icône)
 *  - les modes Glass "Monochrome" et "Teinte personnalisée"
 *
 * Cela permet de mettre un logo initialement noir en blanc, rouge, bleu, etc.
 */
fun alphaTintFilter(tint: Color, alpha: Float = 1f, brightness: Float = 1f): ColorFilter {
    val r = (tint.red * brightness).coerceIn(0f, 1f)
    val g = (tint.green * brightness).coerceIn(0f, 1f)
    val b = (tint.blue * brightness).coerceIn(0f, 1f)
    val a = (alpha).coerceIn(0f, 1f)
    val effectiveColor = Color(r, g, b, a)
    // ColorFilter.tint est l'API officielle pour recolorer une forme en ignorant sa couleur d'origine
    // tout en respectant son masque alpha. Parfait pour les foregrounds noirs des AdaptiveIcons.
    return ColorFilter.tint(effectiveColor)
}

/** Échelle du logo pour [packageName] (override par app, défaut 1). */
@Composable
fun iconStyleLogoScale(packageName: String?): Float =
    packageName?.let { LocalIconOverrides.current[it]?.logoZoom } ?: 1f

/** Filtre couleur à appliquer au glyphe pour le style courant (null = aucun). */
@Composable
fun iconStyleGlyphFilter(packageName: String? = null): ColorFilter? {
    // Personnalisation PAR APP du logo (prioritaire / hiérarchiquement supérieure au global).
    val override = packageName?.let { LocalIconOverrides.current[it] }
    val logoBrightness = override?.logoBrightness ?: 1f

    // TEINTE imposée → REMPLACE entièrement le glyphe global (la luminosité s'applique dessus).
    // On utilise alphaTintFilter (masque alpha) plutôt que luminance, pour supporter
    // correctement les logos majoritairement noirs (cas très fréquent) → blanc s'affiche bien.
    if (override?.logoColor != null) {
        return alphaTintFilter(override.logoColor, brightness = logoBrightness)
    }
    // TEINTE du logo : préserve la luminance (détails/dégradés conservés), contrairement au
    // pochoir ci-dessus. REMPLACE aussi le glyphe global pour cette app.
    if (override?.logoTint != null) {
        return monochromeTintFilter(override.logoTint, brightness = logoBrightness)
    }

    // Sinon : on suit le glyphe du style GLOBAL, mais la LUMINOSITÉ par app (si ≠ 1) est repliée
    // DANS le glyphe global → elle reste toujours visible quel que soit le mode (Default/Sombre/
    // Teinté/Verre) sans casser la teinte du mode. C'est la « supériorité hiérarchique » voulue.
    val style = LocalIconStyle.current
    return when (style.mode) {
        IconStyleMode.Default, IconStyleMode.Dark ->
            if (logoBrightness != 1f) originalGlyphFilter(brightness = logoBrightness) else null
        IconStyleMode.Tinted -> monochromeTintFilter(style.tintColor, brightness = logoBrightness)
        IconStyleMode.Glass -> {
            val a = style.glassGlyphOpacity
            val br = style.glassGlyphBrightness * logoBrightness
            when (style.glassGlyph) {
                GlassGlyphStyle.Mono       -> alphaTintFilter(Color.White, a, br)
                GlassGlyphStyle.Original   -> originalGlyphFilter(a, br)
                GlassGlyphStyle.CustomTint -> alphaTintFilter(style.glassGlyphTintColor, a, br)
                GlassGlyphStyle.SystemTint -> {
                    val mat = LocalGlassMaterial.current
                    monochromeTintFilter(glassTintColor(mat.tint, mat.customTintColor), a, br)
                }
            }
        }
    }
}

/**
 * Applique le FOND de tuile du style courant (clip + couleur/anthracite/verre) à un Modifier
 * déjà dimensionné. Réutilisé par [StyledIconTile] (taille fixe) et FillingAppIcon (fillMaxSize).
 */
@Composable
fun Modifier.iconStyleBackground(
    backgroundColor: Color,
    shape: Shape = FruitIconShape,
    packageName: String? = null
): Modifier {
    // Override par app prioritaire : fond de couleur perso → fond plein opaque (ignore le global).
    val override = packageName?.let { LocalIconOverrides.current[it] }
    if (override?.bgColor != null) return this.clip(shape).background(override.bgColor)

    val style = LocalIconStyle.current
    val hazeState = LocalIconHazeState.current
    val material = LocalGlassMaterial.current
    val clipped = this.clip(shape)
    return when (style.mode) {
        IconStyleMode.Default -> {
            var mod = clipped.background(backgroundColor)
            if (style.lightBorder) {
                mod = mod.border(1.dp, lightBorderBrush, shape)
            }
            mod
        }
        IconStyleMode.Dark, IconStyleMode.Tinted -> {
            val gradient = Brush.verticalGradient(
                colors = listOf(IconDarkBackgroundTop, IconDarkBackground)
            )
            var mod = clipped.background(gradient)
            if (style.lightBorder) {
                mod = mod.border(1.dp, lightBorderBrush, shape)
            }
            mod
        }
        IconStyleMode.Glass -> {
            // Niveau/loupe/brillance = système ; la TEINTE peut être système ou personnalisée.
            val customTint = style.glassTintSource == GlassTintSource.Custom
            val tint = if (customTint) GlassTint.Custom else material.tint
            val tintColor = if (customTint) style.glassCustomTint else material.customTintColor
            if (hazeState != null) {
                clipped.glass(
                    hazeState = hazeState,
                    level = material.level,
                    glassTint = tint,
                    customTintColor = tintColor,
                    shape = shape,
                    loupeLevel = material.loupe,
                    glossLevel = material.gloss,
                    showBorder = style.lightBorder
                )
            } else {
                clipped.background(glassTintColor(tint, tintColor).copy(alpha = 0.28f))
            }
        }
    }
}

/**
 * Tuile visuelle d'une icône (squircle + fond/verre + glyphe) rendue selon le style GLOBAL
 * courant ([LocalIconStyle]) — point unique réutilisé par tous les rendus d'icônes à partir
 * d'un bitmap : [com.stanleycx.fruitos.ui.components.AppIcon], drag ghost, vignettes de
 * dossiers, dossier agrandi, etc.
 *
 * @param size côté de la tuile.
 * @param shadowElevation ombre portée (0.dp = aucune, pour les petites vignettes).
 */
@Composable
fun StyledIconTile(
    imageBitmap: ImageBitmap,
    backgroundColor: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    shadowElevation: Dp = 0.dp,
    shape: Shape = FruitIconShape,
    packageName: String? = null
) {
    val sized = modifier
        .size(size)
        .then(if (shadowElevation > 0.dp) Modifier.shadow(shadowElevation, shape, clip = false) else Modifier)

    val zoom = iconStyleLogoScale(packageName)
    val glyphFilter = iconStyleGlyphFilter(packageName)

    Box(
        modifier = sized.iconStyleBackground(backgroundColor, shape, packageName),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = imageBitmap,
            contentDescription = contentDescription,
            colorFilter = glyphFilter,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Buffer offscreen UNIQUEMENT quand un filtre couleur est réellement appliqué
                    // (Teinté / Verre / override par app). En mode Défaut le filtre est null → l'offscreen
                    // ne change rien aux pixels mais coûte cher, multiplié par toutes les icônes affichées.
                    compositingStrategy =
                        if (glyphFilter != null) CompositingStrategy.Offscreen else CompositingStrategy.Auto
                    if (zoom != 1f) { scaleX = zoom; scaleY = zoom }
                }
        )
    }
}
