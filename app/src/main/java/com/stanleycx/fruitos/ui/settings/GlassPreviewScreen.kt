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
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * Page de preview du niveau de verre (style Fruit OS).
 * Montre un aperçu réaliste du Dock + un dossier avec le verre sélectionné (level + tint).
 * Le dossier et le dock reflètent aussi la teinte courante pour un aperçu fidèle.
 */
@Composable
fun GlassPreviewScreen(
    currentLevel: GlassLevel,
    onLevelSelected: (GlassLevel) -> Unit,
    onBack: () -> Unit,
    hazeState: HazeState,
    glassTint: GlassTint = GlassTint.None,
    customTintColor: Color? = null,
    loupeLevel: LoupeLevel = LoupeLevel.None,
    glossLevel: GlossLevel = GlossLevel.None,
    modifier: Modifier = Modifier
) {
    val levels = GlassLevel.values()

    // === Données factices pour un preview réaliste (le verre du dossier est 100% réel) ===
    val previewFolder = HomeItem.Folder(
        id = "preview_folder",
        name = "Dossier",
        apps = emptyList()   // Le container du dossier utilise le vrai glassLevel de toute façon
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7)) // Fond clair Fruit OS
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
                text = "Niveau de verre",
                color = Color.Black,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Zone de Preview (style écran d'accueil)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFE5E5EA)) // Couleur de fond d'écran clair simulé
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Aperçu",
                color = Color.Black.copy(alpha = 0.6f),
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // === Dossier exemple - RENDU RÉEL (FolderIcon avec le vrai glassLevel) ===
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                FolderIcon(
                    folder = previewFolder,
                    onOpen = {},
                    isEditing = false,
                    hazeState = hazeState,
                    glassLevel = currentLevel,
                    glassTint = glassTint,
                    customTintColor = customTintColor,
                    loupeLevel = loupeLevel,
                    glossLevel = glossLevel,
                    showLabel = true,
                    iconSize = 78.dp
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Dossier",
                    color = Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // === Dock exemple (rendu très proche du vrai Dock) ===
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(88.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .glass(
                        hazeState = hazeState,
                        level = currentLevel,
                        glassTint = glassTint,
                        customTintColor = customTintColor,
                        shape = RoundedCornerShape(36.dp),
                        loupeLevel = loupeLevel,
                        glossLevel = glossLevel
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(4) {
                        PreviewDockAppIcon()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Liste des niveaux de verre
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "CHOISIR UN NIVEAU",
                color = Color.Black.copy(alpha = 0.5f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
            )

            levels.forEach { level ->
                GlassLevelRow(
                    level = level,
                    isSelected = level == currentLevel,
                    onClick = { onLevelSelected(level) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Info
        Text(
            text = "L'effet s'applique en temps réel sur l'écran d'accueil.",
            color = Color.Black.copy(alpha = 0.45f),
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 32.dp)
        )
    }
}

@Composable
private fun GlassLevelRow(
    level: GlassLevel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFFE5F1FF) else Color.White)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = level.name,
                color = Color.Black,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = when (level) {
                    GlassLevel.Clear   -> "Transparent – aucun flou"
                    GlassLevel.Thin    -> "Très fin – discret"
                    GlassLevel.Regular -> "Standard (recommandé)"
                    GlassLevel.Thick   -> "Profond et prononcé"
                    GlassLevel.Ultra -> "Maximum de verre"
                },
                color = Color.Black.copy(alpha = 0.55f),
                fontSize = 14.sp
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sélectionné",
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// Icône du dock dans la preview (taille et forme proches des vraies)
@Composable
private fun PreviewDockAppIcon() {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF2C2C2E))
    )
}
