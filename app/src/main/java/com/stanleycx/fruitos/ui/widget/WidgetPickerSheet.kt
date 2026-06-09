package com.stanleycx.fruitos.ui.widget

import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.stanleycx.fruitos.ui.components.GlassLevel
import com.stanleycx.fruitos.ui.components.GlassTint
import com.stanleycx.fruitos.ui.components.LoupeLevel
import com.stanleycx.fruitos.ui.components.GlossLevel
import com.stanleycx.fruitos.ui.components.glass
import dev.chrisbanes.haze.HazeState

@Composable
fun WidgetPickerSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onPickWidget: (AppWidgetProviderInfo) -> Unit,
    hazeState: HazeState,
    glassLevel: GlassLevel = GlassLevel.Thick,
    glassTint: GlassTint = GlassTint.None,
    customTintColor: Color? = null,
    loupeLevel: LoupeLevel = LoupeLevel.None,
    glossLevel: GlossLevel = GlossLevel.None
) {
    if (!visible) return

    val context = LocalContext.current
    val awm = LocalAppWidgetManager.current
    val pm = context.packageManager

    // Groupes par app, triés par label
    val grouped = remember {
        awm.installedProviders
            .groupBy { it.provider.packageName }
            .entries
            .sortedBy { (pkg, _) ->
                try { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }
                catch (_: Exception) { pkg }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .glass(
                    hazeState = hazeState,
                    level = glassLevel,
                    glassTint = glassTint,
                    customTintColor = customTintColor,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    loupeLevel = loupeLevel,
                    glossLevel = glossLevel
                )
                .clickable(onClick = {}) // absorbe les taps pour ne pas fermer
        ) {
            // Handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp, 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                )
            }

            // Titre + bouton fermer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Widgets",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Fermer",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                grouped.forEach { (pkg, providers) ->
                    // En-tête de groupe
                    item(key = "header_$pkg") {
                        val appLabel = remember(pkg) {
                            try { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }
                            catch (_: Exception) { pkg }
                        }
                        val appIcon: ImageBitmap? = remember(pkg) {
                            try { pm.getApplicationIcon(pkg).toBitmap().asImageBitmap() }
                            catch (_: Exception) { null }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (appIcon != null) {
                                Image(
                                    bitmap = appIcon,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Text(
                                text = appLabel,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Widgets de cette app
                    items(providers, key = { "${it.provider.packageName}/${it.provider.className}" }) { info ->
                        WidgetProviderItem(
                            info = info,
                            pm = pm,
                            onClick = { onPickWidget(info) }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun WidgetProviderItem(
    info: AppWidgetProviderInfo,
    pm: android.content.pm.PackageManager,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val label = remember(info.provider) { info.loadLabel(pm) }
    val preview: ImageBitmap? = remember(info.provider) {
        var drawable: Drawable? = null
        try {
            if (info.previewImage != 0) {
                drawable = pm.getResourcesForApplication(info.provider.packageName)
                    .getDrawable(info.previewImage, null)
            }
        } catch (_: Exception) { }
        if (drawable == null) {
            try { drawable = info.loadIcon(context, context.resources.displayMetrics.densityDpi) }
            catch (_: Exception) { }
        }
        try { drawable?.toBitmap()?.asImageBitmap() } catch (_: Exception) { null }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Aperçu / icône
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (preview != null) {
                Image(
                    bitmap = preview,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            val subtitle = remember(info.provider) {
                val desc = try { info.loadDescription(context)?.toString() } catch (_: Exception) { null }
                if (!desc.isNullOrBlank()) desc else "${info.minWidth}×${info.minHeight} dp"
            }
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
