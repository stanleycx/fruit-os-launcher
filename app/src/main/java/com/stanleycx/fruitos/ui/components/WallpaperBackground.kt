package com.stanleycx.fruitos.ui.components

import android.app.WallpaperManager
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Bitmap du fond d'écran partagé avec tous les éléments glass pour l'effet loupe. */
val LocalWallpaperBitmap = compositionLocalOf<ImageBitmap?> { null }

/**
 * Charge et mémorise le bitmap du fond d'écran système.
 * À appeler au niveau de HomeScreen et fournir via [LocalWallpaperBitmap].
 */
@Composable
fun rememberWallpaperBitmap(): ImageBitmap? {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var wallpaper: ImageBitmap? by remember { mutableStateOf(null) }
    var hasPermission: Boolean by remember {
        mutableStateOf(Environment.isExternalStorageManager())
    }

    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = Environment.isExternalStorageManager()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)
                val drawable = wallpaperManager.drawable
                if (drawable != null) {
                    var src = if (drawable is BitmapDrawable && drawable.bitmap != null) {
                        drawable.bitmap
                    } else {
                        drawable.toBitmap(
                            width = drawable.intrinsicWidth.coerceAtLeast(1),
                            height = drawable.intrinsicHeight.coerceAtLeast(1)
                        )
                    }
                    // PERF CRITIQUE pour fluidité + mémoire launcher:
                    // Le wallpaper full-res (souvent 1440x3000+) consomme 20-60MB + rend les
                    // haze + loupe drawBehind très chers à chaque frame/redraw.
                    // On downscale agressivement à ~1280px max pour le source de tous les glass.
                    // Qualité visuelle du blur/loupe reste excellente (haze + draw scale de toute façon).
                    val maxDim = 1280
                    if (src.width > maxDim || src.height > maxDim) {
                        val scale = minOf(maxDim.toFloat() / src.width, maxDim.toFloat() / src.height)
                        val newW = (src.width * scale).toInt().coerceAtLeast(1)
                        val newH = (src.height * scale).toInt().coerceAtLeast(1)
                        val scaled = android.graphics.Bitmap.createScaledBitmap(src, newW, newH, true)
                        // Libère l'original si on l'a créé nous (sinon le WM gère)
                        if (src != (drawable as? BitmapDrawable)?.bitmap) {
                            src.recycle()
                        }
                        src = scaled
                    }
                    wallpaper = src.asImageBitmap()
                }
            } catch (e: Exception) {
            }
        }
    }

    return wallpaper
}

/**
 * Affiche le fond d'écran système via [LocalWallpaperBitmap].
 * Le bitmap est chargé par [rememberWallpaperBitmap] au niveau parent (HomeScreen).
 */
@Composable
fun WallpaperBackground(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bmp = LocalWallpaperBitmap.current
    val hasPermission = remember { mutableStateOf(Environment.isExternalStorageManager()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission.value = Environment.isExternalStorageManager()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize()
        )
    } else if (!hasPermission.value) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable {
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                    }
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Autoriser l'accès au wallpaper\npour activer le Fruity Glass",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
