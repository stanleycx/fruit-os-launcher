package com.stanleycx.fruitos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stanleycx.fruitos.data.AppInfo
import com.stanleycx.fruitos.ui.home.useJiggleAngle
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import com.stanleycx.fruitos.ui.components.Haptics
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/**
 * Affiche une icône d'app style Fruit OS, avec gestion du mode édition.
 *
 * @param app L'app à afficher
 * @param onClick Action au tap normal
 * @param onLongClick Action au long-press (généralement : entrer en mode édition)
 * @param onRemove Action au clic sur le bouton "−" (en mode édition uniquement)
 * @param isEditing Si true, l'icône tremble et le bouton "−" est visible
 */
@Composable
fun AppIcon(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onLongClickForMenu: (Rect) -> Unit = {},   // Long press hors mode édition → menu contextuel (passe les bounds de l'icône)
    onRemove: () -> Unit = {},
    onDragStart: (Offset) -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    isEditing: Boolean = false,
    isLibraryContext: Boolean = false,   // true = dans App Library → bouton suppression = X au lieu de -
    isBeingDragged: Boolean = false,
    showLabel: Boolean = true,
    iconSize: androidx.compose.ui.unit.Dp = 68.dp,
    isContextMenuTarget: Boolean = false,   // true → on cache l'original (le highlight flottant passe devant le noir)
    isMenuHighlight: Boolean = false,       // true → version agrandie rendue au-dessus du calque sombre
    badgeCount: Int = 0,
    labelMaxWidth: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp.Unspecified,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cached = remember(app.packageName) {
        IconCache.getOrRender(app.packageName, context)
    }
    val imageBitmap = cached.imageBitmap
    val backgroundColor = cached.backgroundColor

    val jiggleAngle = useJiggleAngle(
        isEditing = isEditing,
        seed = app.packageName.hashCode()
    )

    val density = androidx.compose.ui.platform.LocalDensity.current

    // PERF: capture callbacks with rememberUpdatedState so pointerInput (which keys on package+editing)
    // doesn't recreate its internal detector every time parent recomposes and hands new lambda refs.
    // Critical for grid with 20+ icons + drag/edit interactions.
    val onClickUpdated by rememberUpdatedState(onClick)
    val onLongClickUpdated by rememberUpdatedState(onLongClick)
    val onLongClickForMenuUpdated by rememberUpdatedState(onLongClickForMenu)
    val onDragStartUpdated by rememberUpdatedState(onDragStart)
    val onRemoveUpdated by rememberUpdatedState(onRemove)

// Animation de "pop" quand le drag démarre : 1.0 → 1.15
    val popScale by animateFloatAsState(
        targetValue = if (isBeingDragged) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "pop_scale"
    )

    // Léger grossissement supplémentaire quand c'est le highlight du menu contextuel
    // (l'icône passe devant le calque noir + paraît plus grosse)
    val menuHighlightScale by animateFloatAsState(
        targetValue = if (isMenuHighlight) 1.13f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "menu_highlight_scale"
    )

    // On stocke la position dans une référence "non observée" pour éviter les recompositions
    val positionRef = remember { mutableStateOf(Offset.Zero) }
    // Position précise de la tuile visuelle (le Box +10.dp qui contient le squircle centré).
    // Utilisée uniquement pour le rect du menu contextuel afin que les dossiers
    // à noms longs ne décalent plus la capture (le Column racine s'élargit avec le label).
    val tilePositionRef = remember { mutableStateOf(Offset.Zero) }

    // Pour le highlight du menu on désactive complètement les gestes (c'est purement visuel au-dessus du noir)
    val gestureModifier = if (isMenuHighlight) {
        Modifier
    } else {
        Modifier.pointerInput(app.packageName, isEditing) {
            if (isEditing) {
                // En mode édition : long-press arme le drag global
                detectTapGestures(
                    onTap = { onClickUpdated() },
                    onLongPress = { offset ->
                        Haptics.light(context)
                        onDragStartUpdated(positionRef.value + offset)
                    }
                )
            } else {
                // Hors édition : tap lance l'app
                // Long-press :
                //   - Sur le Dock : onLongClick est configuré pour lancer directement le mode édition
                //   - Sur la grille : onLongClickForMenu ouvre le menu contextuel
                detectTapGestures(
                    onTap = { onClickUpdated() },
                    onLongPress = { _ ->
                        Haptics.light(context)
                        onLongClickUpdated()   // Dock : entre directement en mode édition. Grille : ne fait rien (guard inside)

                        // Menu contextuel (uniquement pour les icônes de la grille)
                        val iconSizePx = with(density) { iconSize.toPx() }
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
            // Capture la position absolue de cette icône (utile pour le drag)
            .onGloballyPositioned { coordinates ->
                // On stocke directement dans le ref sans déclencher de recomposition
                positionRef.value = coordinates.positionInRoot()
            }
            .then(gestureModifier)
            // Si on est en train d'être draggé ou que c'est la cible du menu contextuel → on cache l'original
            // (le highlight flottant au niveau racine passe devant le calque noir)
            .alpha(
                when {
                    isMenuHighlight -> 1f
                    isBeingDragged -> 0f
                    isContextMenuTarget -> 0f
                    else -> 1f
                }
            )
    ) {
        if (isMenuHighlight) {
            // === MODE HIGHLIGHT (menu contextuel) ===
            // L'icône est verrouillée via align TopCenter. Le label (potentiellement large)
            // est un sibling aligné au même centre mais offset en Y : sa largeur n'a
            // aucun impact sur la position X de la tuile.
            // Grossissement = taille du param iconSize (déjà 1.13x), pas de scale() transform.
            Box(modifier = Modifier) {
                // Tuile icône — son centre définit la position d'ancrage du highlight
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
                    StyledIconTile(
                        imageBitmap = imageBitmap,
                        backgroundColor = backgroundColor,
                        size = iconSize,
                        contentDescription = app.label,
                        shadowElevation = 18.dp,
                        packageName = app.packageName
                    )
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

                // Label centré sur le même axe vertical que l'icône, mais libre de s'étendre
                // horizontalement (widthIn large). Le Box parent + align TopCenter de l'icône
                // garantissent que l'icône ne bouge jamais latéralement.
                if (showLabel) {
                    Text(
                        text = app.label,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = iconSize + 8.dp)
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
            // === MODE NORMAL + édition ===
            Box(
                modifier = Modifier
                    .size(iconSize + 10.dp)  // un peu plus grand pour laisser de la place au "−"
                    .scale(popScale * menuHighlightScale)
                    .rotate(jiggleAngle)
                    .onGloballyPositioned { coordinates ->
                        tilePositionRef.value = coordinates.positionInRoot()
                    },
                contentAlignment = Alignment.Center
            ) {
                StyledIconTile(
                    imageBitmap = imageBitmap,
                    backgroundColor = backgroundColor,
                    size = iconSize,
                    contentDescription = app.label,
                    shadowElevation = 4.dp,
                    packageName = app.packageName
                )

                if (isEditing) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(24.dp)
                            .offset(x = (-3).dp, y = (-2).dp)
                            .shadow(elevation = 2.dp, shape = CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .pointerInput(app.packageName) {
                                detectTapGestures(onTap = { onRemoveUpdated() })
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLibraryContext) {
                            // X dans l'App Library (suppression d'app)
                            Box(
                                modifier = Modifier.size(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 12.dp, height = 2.dp)
                                        .graphicsLayer { rotationZ = 45f }
                                        .background(Color.Black)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(width = 12.dp, height = 2.dp)
                                        .graphicsLayer { rotationZ = -45f }
                                        .background(Color.Black)
                                )
                            }
                        } else {
                            // "-" classique sur l'écran d'accueil (retrait de la disposition)
                            Box(
                                modifier = Modifier
                                    .size(width = 10.dp, height = 2.dp)
                                    .background(Color.Black)
                            )
                        }
                    }
                }

                // Pastille de notification (coins haut droite du squircle)
                if (badgeCount > 0) {
                    NotificationBadge(
                        count = badgeCount,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 3.dp, y = (-2).dp)
                    )
                }
            }

            if (showLabel) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = app.label,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .widthIn(max = iconSize * 2.2f),
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
}