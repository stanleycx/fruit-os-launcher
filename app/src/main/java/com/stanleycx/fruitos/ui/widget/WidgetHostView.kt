package com.stanleycx.fruitos.ui.widget

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * FrameLayout qui applique un zoom centré + clip arrondi.
 *
 * PERF : le clip arrondi se fait via `clipToOutline` + `ViewOutlineProvider` (clip
 * d'outline MATÉRIEL, accéléré GPU, recalculé seulement au changement de taille) au lieu
 * d'un `canvas.clipPath()` REFAIT À CHAQUE FRAME — ce dernier était coûteux et faisait
 * ramer le scroll du pager (l'AppWidgetHostView est lourde à redessiner).
 *
 * Le zoom (centerCrop) reste un `canvas.scale()` dans dispatchDraw : il s'applique à tous
 * les draw calls suivants, y compris le RenderNode de l'AppWidgetHostView, et le clip
 * d'outline découpe proprement le contenu agrandi aux coins arrondis.
 */
private class ZoomClipFrameLayout(
    context: Context,
    var fillScale: Float,
    val cornerRadiusPx: Float
) : FrameLayout(context) {

    /** Le AppWidgetHostView enfant, à qui on pousse la taille allouée. */
    var hostView: AppWidgetHostView? = null

    init {
        // Clip arrondi matériel : évalué une seule fois (et à chaque resize), pas par frame.
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadiusPx)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Recalcule l'outline arrondi pour la nouvelle taille (sinon clip à l'ancienne taille).
        invalidateOutline()

        // ── CRUCIAL ──────────────────────────────────────────────────────────
        // Informe le widget de la taille EXACTE de la zone qu'il occupe (en dp).
        // Sans ça, beaucoup de widgets (dont la plupart des widgets Google /
        // responsive RemoteViews) ne reçoivent jamais onAppWidgetOptionsChanged,
        // ne savent pas quelle disposition pousser et restent vides.
        pushSizeToWidget(w, h)
    }

    // updateAppWidgetSize(Bundle, min/max w/h) est déprécié mais reste nécessaire ici : c'est
    // ce qui notifie réellement le provider (onAppWidgetOptionsChanged) pour qu'un widget
    // responsive pousse sa disposition. La variante non dépréciée ne couvre pas tous les widgets.
    @Suppress("DEPRECATION")
    fun pushSizeToWidget(wPx: Int, hPx: Int) {
        val host = hostView ?: return
        if (wPx <= 0 || hPx <= 0) return
        val d = resources.displayMetrics.density
        // Le zoom (fillScale) agrandit le contenu : on dimensionne le widget à la
        // taille logique AVANT zoom pour que sa mise en page corresponde à la cellule.
        val wDp = (wPx / d / fillScale).toInt()
        val hDp = (hPx / d / fillScale).toInt()
        if (wDp <= 0 || hDp <= 0) return

        val options = Bundle().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // API 31+ : les widgets responsive choisissent leur layout via la
                // liste de tailles fournies. On donne la taille réelle de la cellule.
                putParcelableArrayList(
                    android.appwidget.AppWidgetManager.OPTION_APPWIDGET_SIZES,
                    arrayListOf(SizeF(wDp.toFloat(), hDp.toFloat()))
                )
            }
        }
        // updateAppWidgetSize met à jour les OPTION_* min/max ET notifie le provider.
        host.updateAppWidgetSize(options, wDp, hDp, wDp, hDp)
    }

    override fun dispatchDraw(canvas: Canvas) {
        // Coins arrondis = clipToOutline (matériel). Ici, seul le zoom centerCrop.
        // En taille normale (fillScale == 1) : aucun coût par frame ajouté.
        if (fillScale != 1f) {
            canvas.save()
            canvas.scale(fillScale, fillScale, width / 2f, height / 2f)
            super.dispatchDraw(canvas)
            canvas.restore()
        } else {
            super.dispatchDraw(canvas)
        }
    }
}

@Composable
fun WidgetHostView(
    appWidgetId: Int,
    fillScale: Float = 1f,
    cornerRadius: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val awh = LocalAppWidgetHost.current
    val awm = LocalAppWidgetManager.current
    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }

    val info = remember(appWidgetId) { awm.getAppWidgetInfo(appWidgetId) }

    if (info == null) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Text("Widget indisponible", color = Color.White.copy(alpha = 0.4f))
        }
        return
    }

    AndroidView(
        factory = { ctx ->
            val hostView = awh.createView(ctx, appWidgetId, info).apply {
                setAppWidget(appWidgetId, info)
                // Pas de scaleX/scaleY natif : le zoom est géré par le canvas dans dispatchDraw.
            }
            ZoomClipFrameLayout(ctx, fillScale, cornerRadiusPx).apply {
                this.hostView = hostView
                addView(hostView, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ))
            }
        },
        update = { container ->
            val frame = container as ZoomClipFrameLayout
            if (frame.fillScale != fillScale) {
                frame.fillScale = fillScale
                frame.invalidate()
                // Le zoom a changé → re-pousse la taille logique au widget.
                frame.pushSizeToWidget(frame.width, frame.height)
            }
        },
        modifier = modifier
    )
}
