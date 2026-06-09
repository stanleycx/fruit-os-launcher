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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stanleycx.fruitos.ui.components.GlassLevel
import com.stanleycx.fruitos.ui.components.GlassTint
import com.stanleycx.fruitos.ui.components.GlossLevel
import com.stanleycx.fruitos.ui.components.LoupeLevel

/**
 * Page Réglages style Fruit OS (fond blanc / clair).
 */
@Composable
fun LauncherSettingsScreen(
    currentGlassLevel: GlassLevel,
    onGlassLevelChange: (GlassLevel) -> Unit,
    currentGlassTint: GlassTint,
    onGlassTintChange: (GlassTint) -> Unit,
    customGlassTintColor: Color? = null,
    onCustomGlassTintColorChange: (Color) -> Unit = {},
    currentLoupeLevel: LoupeLevel,
    onLoupeLevelChange: (LoupeLevel) -> Unit,
    currentGlossLevel: GlossLevel,
    onGlossLevelChange: (GlossLevel) -> Unit,
    darkModeEnabled: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onOpenGlassLevel: () -> Unit,
    onOpenGlassTint: () -> Unit,
    onOpenLoupeLevel: () -> Unit,
    onOpenGlossiness: () -> Unit,
    iconStyleSubtitle: String = "Forme et bordure",
    onOpenIconStyle: () -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7)) // Fond Fruit OS clair
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Retour",
                tint = Color(0xFF007AFF),
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onClose() }
                    .padding(4.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Réglages",
                color = Color.Black,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // === SECTION APPARENCE ===
            item { SettingsSectionHeader("APPARENCE") }

            item {
                SettingsRow(
                    icon = Icons.Default.Palette,
                    iconBackground = Color(0xFF007AFF),
                    title = "Niveau de verre",
                    subtitle = currentGlassLevel.name,
                    onClick = onOpenGlassLevel
                )
            }

            item {
                SettingsRow(
                    icon = Icons.Default.Palette,
                    iconBackground = Color(0xFFFF9500),
                    title = "Teinte du verre",
                    subtitle = if (currentGlassTint == GlassTint.Custom) "Personnalisée" else currentGlassTint.name,
                    onClick = onOpenGlassTint
                )
            }

            item {
                SettingsRow(
                    icon = Icons.Default.BrightnessMedium,
                    iconBackground = Color(0xFF34C759),
                    title = "Effet loupe",
                    subtitle = when (currentLoupeLevel) {
                        LoupeLevel.None   -> "Aucun"
                        LoupeLevel.Light  -> "Léger"
                        LoupeLevel.Medium -> "Moyen"
                        LoupeLevel.Strong -> "Fort"
                        LoupeLevel.Ultra  -> "Ultra"
                    },
                    onClick = onOpenLoupeLevel
                )
            }

            item {
                SettingsRow(
                    icon = Icons.Default.BrightnessMedium,
                    iconBackground = Color(0xFFFF9F0A),
                    title = "Brillance",
                    subtitle = when (currentGlossLevel) {
                        GlossLevel.None      -> "Aucune"
                        GlossLevel.Subtle    -> "Subtile"
                        GlossLevel.Medium    -> "Moyenne"
                        GlossLevel.High      -> "Forte"
                        GlossLevel.Prismatic -> "Prismatique"
                    },
                    onClick = onOpenGlossiness
                )
            }

            item {
                SettingsToggleRow(
                    icon = Icons.Default.DarkMode,
                    iconBackground = Color(0xFF5856D6),
                    title = "Mode sombre",
                    subtitle = "Interface du launcher",
                    checked = darkModeEnabled,
                    onCheckedChange = onDarkModeChange
                )
            }

            item {
                SettingsRow(
                    icon = Icons.Default.Palette,
                    iconBackground = Color(0xFFAF52DE),
                    title = "Style des icônes",
                    subtitle = iconStyleSubtitle,
                    onClick = onOpenIconStyle
                )
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF6C6C70),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 32.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconBackground: Color,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(17.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.Black,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = Color(0xFF8E8E93),
                    fontSize = 14.sp
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Color(0xFFC7C7CC),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    iconBackground: Color,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(17.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.Black,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = Color(0xFF8E8E93),
                    fontSize = 14.sp
                )
            }
        }

        // Toggle Fruit OS style
        Box(
            modifier = Modifier
                .size(width = 51.dp, height = 31.dp)
                .clip(RoundedCornerShape(50))
                .background(if (checked) Color(0xFF34C759) else Color(0xFFE9E9EA))
                .clickable { onCheckedChange(!checked) },
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .size(27.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
            )
        }
    }
}
