package com.stanleycx.fruitos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.stanleycx.fruitos.data.AppInfo
import com.stanleycx.fruitos.data.HomeItem
import com.stanleycx.fruitos.ui.home.useJiggleAngle
import com.stanleycx.fruitos.ui.components.LoupeLevel
import com.stanleycx.fruitos.ui.components.GlossLevel
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

/**
 * Nouvelle implémentation complète des icônes de dossier (Fruit OS style).
 *
 * Design goals:
 * - Même forme squircle que les icônes d'apps (FruitIconShape)
 * - Look glassmorphic / Fruity Glass premium
 * - Intérieur uni et propre avec les 9 mini-icônes bien intégrées
 * - Aucun artefact de "carré blanc" disgracieux
 */
@Composable
fun FolderIcon(
    folder: HomeItem.Folder,
    onOpen: () -> Unit,
    onLongClickForMenu: (Rect) -> Unit = {},   // Long press hors mode édition → menu contextuel (passe les bounds de l'icône)
    isEditing: Boolean = false,
    isBeingDragged: Boolean = false,
    onDragStart: (Offset) -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    iconSize: Dp = 68.dp,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    glassLevel: GlassLevel = GlassLevel.Thick,
    glassTint: GlassTint = GlassTint.None,
    customTintColor: Color? = null,
    loupeLevel: LoupeLevel = LoupeLevel.None,
    glossLevel: GlossLevel = GlossLevel.None,
    isContextMenuTarget: Boolean = false,
    isMenuHighlight: Boolean = false,       // version agrandie rendue au-dessus du calque sombre (comme pour les apps)
    badgeCount: Int = 0,
    labelMaxWidth: Dp = Dp.Unspecified,     // pour contraindre le wrapping du nom en mode highlight par rapport à la grid
    showLabel: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val jiggleAngle = useJiggleAngle(isEditing = isEditing, seed = folder.id.hashCode())

    // PERF: updated state for callbacks to keep pointerInput stable (same as AppIcon).
    val onOpenUpdated by rememberUpdatedState(onOpen)
    val onLongClickForMenuUpdated by rememberUpdatedState(onLongClickForMenu)
    val onDragStartUpdated by rememberUpdatedState(onDragStart)

    val positionRef = remember { mutableStateOf(Offset.Zero) }
    // Position précise de la tuile visuelle (le Box +10.dp qui contient le squircle centré).
    // Utilisée uniquement pour le rect du menu contextuel afin que les dossiers
    // à noms longs ne décalent plus la capture (le Column racine s'élargit avec le label).
    val tilePositionRef = remember { mutableStateOf(Offset.Zero) }

    // Léger grossissement supplémentaire pour le highlight du menu (cohérent avec les apps)
    val menuHighlightScale by animateFloatAsState(
        targetValue = if (isMenuHighlight) 1.13f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "folder_menu_highlight_scale"
    )

    // Pour le highlight du menu on désactive complètement les gestes
    val gestureModifier = if (isMenuHighlight) {
        Modifier
    } else {
        Modifier.pointerInput(folder.id, isEditing) {
            if (isEditing) {
                detectTapGestures(
                    onTap = { onOpenUpdated() },
                    onLongPress = { offset ->
                        Haptics.light(context)
                        onDragStartUpdated(positionRef.value + offset)
                    }
                )
            } else {
                detectTapGestures(
                    onTap = { onOpenUpdated() },
                    onLongPress = { _ ->
                        val iconSizePx = with(density) { iconSize.toPx() }
                        // On utilise la position réelle de la tuile visuelle (Box +10.dp centré)
                        // + un petit offset de 5.dp pour viser le centre du squircle à l'intérieur.
                        // Cela rend la capture insensible à la largeur du label en dessous
                        // (problème spécifique aux dossiers longs noms).
                        val visualTileOffset = with(density) { 5.dp.toPx() }
                        val iconRect = Rect(
                            offset = tilePositionRef.value + Offset(visualTileOffset, visualTileOffset),
                            size = Size(width = iconSizePx, height = iconSizePx)
                        )
                        onLongClickForMenuUpdated(iconRect)
                    }
                )
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                positionRef.value = coordinates.positionInRoot()
            }
            .then(gestureModifier)
            .alpha(
                when {
                    isMenuHighlight -> 1f
                    isBeingDragged -> 0f
                    isContextMenuTarget -> 0f
                    else -> 1f
                }
            )
    ) {
        // === Le dossier lui-même (la tuile) ===
        // En mode highlight (menu contextuel ouvert), on veut un grossissement
        // "statique" : l'icône ne doit pas bouger latéralement, même si le nom
        // est long et passe sur 2 lignes.
        val baseScale = if (isEditing) 0.96f else 1f
        val finalScale = baseScale * menuHighlightScale

        if (isMenuHighlight) {
            // === MODE HIGHLIGHT (menu contextuel) ===
            // Structure identique à AppIcon : icône verrouillée (TopCenter) dans un Box.
            // Le label large est un sibling aligné au même centre vertical mais offset en Y.
            // Sa largeur n'a aucun effet sur la position X de la tuile du dossier.
            // Grossissement par la taille du param iconSize (1.13x), pas de scale supplémentaire.
            Box(modifier = Modifier) {
                // Tuile dossier — position d'ancrage fixe (centre = centre d'origine)
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .graphicsLayer {
                            rotationZ = jiggleAngle
                        }
                        .onGloballyPositioned { coordinates ->
                            tilePositionRef.value = coordinates.positionInRoot()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val glassModifier = if (hazeState != null) {
                        Modifier.glass(
                            hazeState = hazeState,
                            level = glassLevel,
                            glassTint = glassTint,
                            customTintColor = customTintColor,
                            shape = FruitIconShape,
                            loupeLevel = loupeLevel, glossLevel = glossLevel
                        )
                    } else {
                        Modifier
                            .clip(FruitIconShape)
                            .background(Color.White.copy(alpha = 0.14f))
                    }

                    Box(
                        modifier = Modifier
                            .size(iconSize)
                            .clip(FruitIconShape)
                            .then(glassModifier),
                        contentAlignment = Alignment.Center
                    ) {
                        FolderMiniGrid(folder.apps)
                    }
                }

                // Pastille de notification (mode highlight)
                if (badgeCount > 0) {
                    NotificationBadge(
                        count = badgeCount,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 7.dp, y = (-4).dp)
                    )
                }

                // Nom du dossier centré sur l'axe de la tuile, libre de s'étendre latéralement
                // (widthIn large) sans jamais pousser la tuile.
                if (showLabel) {
                    val labelSpacing = 8.dp
                    Text(
                        text = folder.name,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = iconSize + labelSpacing)
                            .widthIn(max = labelMaxWidth.coerceAtLeast(iconSize))
                            .padding(horizontal = 4.dp),
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                                blurRadius = 4f
                            )
                        )
                    )
                }
            }
        } else {
            // Comportement normal - structure CALQUÉE sur le mode highlight (qui marche)
            // et sur le DragGhost. Point clé : on NE met PAS graphicsLayer sur le même
            // node que .glass(). Le hazeEffect doit être sur un node "propre" pour que
            // glassLevel (thickness) et glassTint soient réellement appliqués —
            // sinon la composition GPU du graphicsLayer empêche le blur d'échantillonner
            // correctement le hazeSource derrière, et level/tint n'ont aucun effet visible.
            //
            // Structure :
            //   Box wrapper (iconSize + 10.dp, pour aligner verticalement avec AppIcon)
            //     └─ Box d'ancrage (iconSize, porte rotation + scale via graphicsLayer)
            //          └─ Box visuel (iconSize, porte clip + glass)
            //               └─ FolderMiniGrid
            Box(
                modifier = Modifier
                    .size(iconSize + 10.dp)
                    .onGloballyPositioned { coordinates ->
                        tilePositionRef.value = coordinates.positionInRoot()
                    },
                contentAlignment = Alignment.Center
            ) {
                // Box d'ancrage : rotation jiggle + scale d'édition.
                // graphicsLayer ISOLÉ ici (pas sur le même node que glass).
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .graphicsLayer {
                            rotationZ = jiggleAngle
                            scaleX = finalScale
                            scaleY = finalScale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val glassModifier = if (hazeState != null) {
                        Modifier.glass(
                            hazeState = hazeState,
                            level = glassLevel,
                            glassTint = glassTint,
                            customTintColor = customTintColor,
                            shape = FruitIconShape,
                            loupeLevel = loupeLevel, glossLevel = glossLevel
                        )
                    } else {
                        Modifier
                            .clip(FruitIconShape)
                            .background(Color.White.copy(alpha = 0.14f))
                    }

                    // Box visuel : porte glass — strictement comme dans le mode highlight.
                    Box(
                        modifier = Modifier
                            .size(iconSize)
                            .clip(FruitIconShape)
                            .then(glassModifier),
                        contentAlignment = Alignment.Center
                    ) {
                        FolderMiniGrid(folder.apps)
                    }
                }

                // Pastille de notification (coins haut droite, hors du graphicsLayer pour ne pas tourner)
                if (badgeCount > 0) {
                    NotificationBadge(
                        count = badgeCount,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 3.dp, y = (-2).dp)
                    )
                }
            }

            val labelSpacing = 3.dp
            Spacer(modifier = Modifier.height(labelSpacing))

            // Pour maximiser le nombre de lettres visibles sur les noms longs,
            // on donne une largeur très généreuse et on réduit le padding horizontal
            // au minimum. Le but est d'afficher le plus de caractères possible
            // avant les "...", en utilisant l'espace que prendraient les 3 points.
            Text(
                text = folder.name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                // On retire volontairement l'Ellipsis.
                // On préfère afficher le maximum de lettres réelles ("Divertissemen")
                // plutôt que de remplacer les derniers caractères par "...".
                // Le texte sera simplement coupé si vraiment trop long pour la cellule.
                // On retire complètement la limite de largeur en mode normal
                // pour autoriser le plus d'espace possible au nom (sous les apps
                // comme sous les dossiers). La grille reste la vraie limite.
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .widthIn(max = iconSize * 2.2f),      // un peu plus large que l'icône → noms longs ont de la place,
                // noms courts restent bien centrés dessous
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                        blurRadius = 4f
                    )
                )
            )
        }
    }
}

/**
 * Grille 3x3 moderne et compacte pour l'intérieur du dossier.
 * Les mini-icônes respectent la forme squircle des apps.
 */
@Composable
internal fun FolderMiniGrid(apps: List<AppInfo>) {
    val mini = apps.take(9)

    // Plus d'espace entre les mini-icônes pour un rendu plus aéré et agréable
    val miniIconSize = 16.5.dp
    val spacing = 2.2.dp

    // Grille des mini-icônes, bien centrée et dense (style Fruit OS moderne)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        for (r in 0 until 3) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                for (c in 0 until 3) {
                    val app = mini.getOrNull(r * 3 + c)
                    Box(
                        modifier = Modifier.size(miniIconSize),
                        contentAlignment = Alignment.Center
                    ) {
                        if (app != null) {
                            MiniFolderAppIcon(app, miniIconSize)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Mini icône d'application à l'intérieur d'un dossier.
 * Utilise exactement la même forme que les grandes icônes d'apps.
 */
@Composable
private fun MiniFolderAppIcon(app: AppInfo, size: Dp) {
    val context = LocalContext.current
    val cached = remember(app.packageName) {
        IconCache.getOrRender(app.packageName, context)
    }

    // Même forme que les grandes icônes + suit le style global + override par app.
    StyledIconTile(
        imageBitmap = cached.imageBitmap,
        backgroundColor = cached.backgroundColor,
        size = size,
        packageName = app.packageName
    )
}