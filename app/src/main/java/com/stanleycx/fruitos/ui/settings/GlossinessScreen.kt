package com.stanleycx.fruitos.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.stanleycx.fruitos.ui.components.GlossLevel
import com.stanleycx.fruitos.ui.components.LoupeLevel
import com.stanleycx.fruitos.ui.components.glass
import dev.chrisbanes.haze.HazeState

@Composable
fun GlossinessScreen(
    currentGloss: GlossLevel,
    onGlossSelected: (GlossLevel) -> Unit,
    onBack: () -> Unit,
    hazeState: HazeState,
    glassLevel: GlassLevel = GlassLevel.Regular,
    glassTint: GlassTint = GlassTint.None,
    customTintColor: Color? = null,
    loupeLevel: LoupeLevel = LoupeLevel.None,
    modifier: Modifier = Modifier
) {
    val previewFolder = HomeItem.Folder(
        id = "preview_gloss_folder",
        name = "Dossier",
        apps = emptyList()
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
    ) {
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
                text = "Brillance",
                color = Color.Black,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Preview
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
                .height(220.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1C1C1E))
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                FolderIcon(
                    folder = previewFolder,
                    onOpen = {},
                    isEditing = false,
                    hazeState = hazeState,
                    glassLevel = glassLevel,
                    glassTint = glassTint,
                    customTintColor = customTintColor,
                    loupeLevel = loupeLevel,
                    glossLevel = currentGloss,
                    showLabel = false,
                    iconSize = 72.dp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(62.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .glass(
                            hazeState = hazeState,
                            level = glassLevel,
                            glassTint = glassTint,
                            customTintColor = customTintColor,
                            shape = RoundedCornerShape(28.dp),
                            loupeLevel = loupeLevel,
                            glossLevel = currentGloss
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(Color(0xFF2C2C2E))
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "NIVEAU DE BRILLANCE",
            color = Color(0xFF6C6C70),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlossLevel.entries.forEach { gloss ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (gloss == currentGloss) Color(0xFFE5F1FF) else Color.White)
                        .clickable { onGlossSelected(gloss) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pastille de couleur représentant le reflet
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (gloss) {
                                    GlossLevel.None      -> Color(0xFFCCCCCC)
                                    GlossLevel.Subtle    -> Color(0xFFEEEEEE)
                                    GlossLevel.Medium    -> Color(0xFFF5F5F5)
                                    GlossLevel.High      -> Color.White
                                    GlossLevel.Prismatic -> Color(0xFFFF9FF3)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (gloss) {
                                GlossLevel.None      -> "Aucune"
                                GlossLevel.Subtle    -> "Subtile"
                                GlossLevel.Medium    -> "Moyenne"
                                GlossLevel.High      -> "Forte"
                                GlossLevel.Prismatic -> "Prismatique"
                            },
                            color = Color.Black,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = when (gloss) {
                                GlossLevel.None      -> "Aucun reflet — verre pur"
                                GlossLevel.Subtle    -> "Légère lueur en haut du verre"
                                GlossLevel.Medium    -> "Reflet lumineux modéré"
                                GlossLevel.High      -> "Brillance forte, effet premium"
                                GlossLevel.Prismatic -> "Reflet arc-en-ciel irisé"
                            },
                            color = Color.Black.copy(alpha = 0.55f),
                            fontSize = 13.sp
                        )
                    }
                    if (gloss == currentGloss) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Le reflet s'applique à tous les éléments en verre du launcher.",
            color = Color(0xFF8E8E93),
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 32.dp)
        )
    }
}
