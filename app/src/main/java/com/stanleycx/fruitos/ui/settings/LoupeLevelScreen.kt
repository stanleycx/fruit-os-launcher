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
fun LoupeLevelScreen(
    currentLevel: LoupeLevel,
    onLevelSelected: (LoupeLevel) -> Unit,
    onBack: () -> Unit,
    hazeState: HazeState,
    glassLevel: GlassLevel = GlassLevel.Regular,
    glassTint: GlassTint = GlassTint.None,
    customTintColor: Color? = null,
    glossLevel: GlossLevel = GlossLevel.None,
    modifier: Modifier = Modifier
) {
    val previewFolder = HomeItem.Folder(
        id = "preview_loupe_folder",
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
                text = "Effet loupe",
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
                    loupeLevel = currentLevel,
                    glossLevel = glossLevel,
                    showLabel = false,
                    iconSize = 72.dp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(62.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .glass(
                            hazeState = hazeState,
                            level = glassLevel,
                            glassTint = glassTint,
                            customTintColor = customTintColor,
                            shape = RoundedCornerShape(32.dp),
                            loupeLevel = currentLevel,
                            glossLevel = glossLevel
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
            text = "INTENSITÉ",
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
            LoupeLevel.entries.forEach { level ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (level == currentLevel) Color(0xFFE5F1FF) else Color.White)
                        .clickable { onLevelSelected(level) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (level) {
                                LoupeLevel.None   -> "Aucun"
                                LoupeLevel.Light  -> "Léger"
                                LoupeLevel.Medium -> "Moyen"
                                LoupeLevel.Strong -> "Fort"
                                LoupeLevel.Ultra  -> "Ultra"
                            },
                            color = Color.Black,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = when (level) {
                                LoupeLevel.None   -> "Blur standard, aucun effet supplémentaire"
                                LoupeLevel.Light  -> "Zoom subtil du fond (×${level.factor}) — préserve le verre"
                                LoupeLevel.Medium -> "Zoom léger du fond (×${level.factor})"
                                LoupeLevel.Strong -> "Zoom prononcé du fond (×${level.factor})"
                                LoupeLevel.Ultra  -> "Zoom maximal du fond (×${level.factor})"
                            },
                            color = Color.Black.copy(alpha = 0.55f),
                            fontSize = 13.sp
                        )
                    }
                    if (level == currentLevel) {
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
            text = "Le blur amplifié s'applique à tous les éléments en verre.",
            color = Color(0xFF8E8E93),
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 32.dp)
        )
    }
}
