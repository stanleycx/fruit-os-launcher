package com.stanleycx.fruitos.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stanleycx.fruitos.data.AppInfo
import com.stanleycx.fruitos.ui.components.AppIcon
import com.stanleycx.fruitos.ui.components.IconOverride
import com.stanleycx.fruitos.ui.components.LocalIconOverrides
import dev.chrisbanes.haze.HazeState

private val DefaultCustomColor = Color(0xFF5B9BD5)

@Composable
fun IconCustomizeScreen(
    app: AppInfo,
    current: IconOverride,
    onChange: (IconOverride) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
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
                modifier = Modifier.size(28.dp).clickable { onBack() }.padding(4.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Personnaliser", color = Color.Black, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }

        // === APERÇU live ===
        Text(
            text = app.label.uppercase(),
            color = Color(0xFF6C6C70),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(140.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF3A3A3C))
        ) {
            // Le draft est fourni en local → aperçu en direct (les autres styles globaux hérités).
            CompositionLocalProvider(LocalIconOverrides provides mapOf(app.packageName to current)) {
                Box(modifier = Modifier.align(Alignment.Center)) {
                    AppIcon(app = app, onClick = {}, showLabel = false, iconSize = 64.dp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // === FOND DE COULEUR ===
        ToggleRow(
            label = "Ajouter un fond de couleur",
            checked = current.bgColor != null,
            onCheckedChange = { on ->
                onChange(current.copy(bgColor = if (on) (current.bgColor ?: DefaultCustomColor) else null))
            }
        )
        if (current.bgColor != null) {
            Spacer(modifier = Modifier.height(8.dp))
            InlineColorPicker(
                initial = current.bgColor!!,
                default = DefaultCustomColor,
                onColorChange = { onChange(current.copy(bgColor = it)) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // === COULEUR DU LOGO ===
        ToggleRow(
            label = "Changer la couleur du logo",
            checked = current.logoColor != null,
            onCheckedChange = { on ->
                // Mutuellement exclusif avec la teinte (deux recolorations incompatibles).
                onChange(current.copy(
                    logoColor = if (on) (current.logoColor ?: DefaultCustomColor) else null,
                    logoTint = if (on) null else current.logoTint
                ))
            }
        )
        if (current.logoColor != null) {
            Spacer(modifier = Modifier.height(8.dp))
            InlineColorPicker(
                initial = current.logoColor!!,
                default = DefaultCustomColor,
                onColorChange = { onChange(current.copy(logoColor = it)) }
            )
            // Petit indice : cette couleur remplace la teinte du glyphe via son masque alpha.
            // Elle fonctionne même si le logo d'origine est noir (contrairement à un tint classique).
            Text(
                text = "Remplace la couleur du logo (fonctionne sur les logos noirs)",
                color = Color(0xFF8E8E93),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // === TEINTE DU LOGO ===
        ToggleRow(
            label = "Teinter le logo",
            checked = current.logoTint != null,
            onCheckedChange = { on ->
                // Mutuellement exclusif avec la couleur (pochoir).
                onChange(current.copy(
                    logoTint = if (on) (current.logoTint ?: DefaultCustomColor) else null,
                    logoColor = if (on) null else current.logoColor
                ))
            }
        )
        if (current.logoTint != null) {
            Spacer(modifier = Modifier.height(8.dp))
            InlineColorPicker(
                initial = current.logoTint!!,
                default = DefaultCustomColor,
                onColorChange = { onChange(current.copy(logoTint = it)) }
            )
            Text(
                text = "Teinte le logo en gardant ses détails et dégradés (contrairement à la couleur, qui l'aplatit en pochoir)",
                color = Color(0xFF8E8E93),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // === LUMINOSITÉ DU LOGO ===
        LabeledSlider(
            label = "LUMINOSITÉ DU LOGO",
            value = current.logoBrightness,
            valueRange = 0f..2f,
            valueLabel = "${(current.logoBrightness * 100).toInt()} %",
            defaultValue = 1f,
            onValueChange = { onChange(current.copy(logoBrightness = it)) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // === ZOOM DU LOGO ===
        LabeledSlider(
            label = "ZOOM DU LOGO",
            value = current.logoZoom,
            valueRange = 0.6f..1.6f,
            valueLabel = "${(current.logoZoom * 100).toInt()} %",
            defaultValue = 1f,
            onValueChange = { onChange(current.copy(logoZoom = it)) }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // === RESET ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable { onReset() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Réinitialiser cette icône", color = Color(0xFFFF3B30), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable { onCheckedChange(!checked) }
            .padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Black, fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF34C759))
        )
    }
}
