package com.stanleycx.fruitos.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stanleycx.fruitos.data.HomeItem
import com.stanleycx.fruitos.ui.components.FolderIcon
import com.stanleycx.fruitos.ui.components.GlassLevel
import com.stanleycx.fruitos.ui.components.GlassTint
import dev.chrisbanes.haze.HazeState

/**
 * Écran dédié à la Teinte du Verre (style Fruit OS).
 */
@Composable
fun GlassTintScreen(
    currentTint: GlassTint,
    onTintSelected: (GlassTint) -> Unit,
    customTintColor: Color? = null,
    onCustomTintColorSelected: (Color) -> Unit = {},
    currentLevel: GlassLevel,
    onBack: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    var showCustomScreen by remember { mutableStateOf(false) }

    if (showCustomScreen) {
        CustomGlassTintScreen(
            currentLevel = currentLevel,
            initialColor = customTintColor,
            onApply = { chosenColor ->
                onCustomTintColorSelected(chosenColor)
            },
            onBack = { showCustomScreen = false },
            hazeState = hazeState
        )
    } else {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
    ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 8.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = Color(0xFF007AFF),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onBack() }
                        .padding(4.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Teinte du verre",
                    color = Color.Black,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // === Preview : Icône de dossier réelle (glassLevel/thickness + glassTint) ===
            // Montre l'effet complet du niveau de verre (blur + épaisseur) + teinte, comme sur le Dock.
            Text(
                text = "APERÇU",
                color = Color(0xFF6C6C70),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(160.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1C1C1E))
            ) {
                val previewFolder = remember {
                    HomeItem.Folder(
                        id = "preview_tint_folder",
                        name = "Dossier",
                        apps = emptyList()
                    )
                }

                FolderIcon(
                    folder = previewFolder,
                    onOpen = {},
                    isEditing = false,
                    hazeState = hazeState,
                    glassLevel = currentLevel,
                    glassTint = currentTint,
                    customTintColor = if (currentTint == GlassTint.Custom) customTintColor else null,
                    showLabel = true,
                    iconSize = 78.dp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // === Liste des teintes (scrollable) ===
            Text(
                text = "TEINTE",
                color = Color(0xFF6C6C70),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 20.dp, bottom = 10.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassTint.entries.forEach { tint ->
                    val isSelected = tint == currentTint

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .clickable {
                                if (tint == GlassTint.Custom) {
                                    showCustomScreen = true
                                } else {
                                    onTintSelected(tint)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pastille de couleur
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (tint) {
                                        GlassTint.None    -> Color(0xFFE8ECF0)
                                        GlassTint.White   -> Color.White
                                        GlassTint.Black   -> Color(0xFF2C2C2E)
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
                                        GlassTint.Custom  -> Color(0xFF888888)
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFCCCCCC),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = when (tint) {
                                GlassTint.None    -> "Aucune"
                                GlassTint.White   -> "Blanc"
                                GlassTint.Black   -> "Noir"
                                GlassTint.Gray    -> "Gris"
                                GlassTint.Blue    -> "Bleu"
                                GlassTint.Warm    -> "Chaud"
                                GlassTint.Cool    -> "Froid"
                                GlassTint.Purple  -> "Violet"
                                GlassTint.Pink    -> "Rose"
                                GlassTint.Green   -> "Vert"
                                GlassTint.Orange  -> "Orange"
                                GlassTint.Teal    -> "Turquoise"
                                GlassTint.Red     -> "Rouge"
                                GlassTint.Gold    -> "Or"
                                GlassTint.Magenta -> "Magenta"
                                GlassTint.Custom  -> "Personnalisée"
                            },
                            color = Color.Black,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // "Autres couleurs..." directement en dernier dans la liste scrollable
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable { showCustomScreen = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Autres couleurs...",
                        color = Color(0xFF007AFF),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = Color(0xFFC7C7CC),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "La teinte s'applique à tous les éléments en verre du launcher.",
                color = Color(0xFF8E8E93),
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 32.dp)
            )
        }
    }
}
