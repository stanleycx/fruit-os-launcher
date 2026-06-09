package com.stanleycx.fruitos

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.stanleycx.fruitos.data.WidgetHostManager
import com.stanleycx.fruitos.ui.home.HomeScreen
import com.stanleycx.fruitos.ui.widget.LocalAppWidgetHost
import com.stanleycx.fruitos.ui.widget.LocalAppWidgetManager

class MainActivity : ComponentActivity() {

    private val appWidgetHost: AppWidgetHost by lazy {
        WidgetHostManager.get(this)
    }
    private val appWidgetManager: AppWidgetManager by lazy {
        AppWidgetManager.getInstance(this)
    }

    // isStatusBarContrastEnforced est déprécié mais sans remplacement direct : il empêche le
    // scrim système translucide sur une status bar transparente (les couleurs de barres sont
    // déjà gérées par enableEdgeToEdge ci-dessous, qui remplace statusBarColor/navigationBarColor).
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, window.decorView)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        // Status bar SYSTÈME affichée : on ne pose plus FLAG_FULLSCREEN. Le contenu passe
        // sous la barre (edge-to-edge) ; on réserve son inset via WindowInsets.statusBars.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val exclusionRects = listOf(
                Rect(0, 0, 120, resources.displayMetrics.heightPixels),
                Rect(resources.displayMetrics.widthPixels - 120, 0, resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
            )
            window.decorView.systemGestureExclusionRects = exclusionRects
        }

        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false

        // La status bar SYSTÈME reste visible. Le contrôleur immersif ne gère QUE la nav bar.
        controller.hide(WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Si la nav bar est ré-affichée, on la re-cache (sans toucher à la status bar).
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            if (insets.isVisible(WindowInsetsCompat.Type.navigationBars())) {
                WindowInsetsControllerCompat(window, window.decorView)
                    .hide(WindowInsetsCompat.Type.navigationBars())
            }
            insets
        }

        setContent {
            CompositionLocalProvider(
                LocalAppWidgetHost provides appWidgetHost,
                LocalAppWidgetManager provides appWidgetManager
            ) {
                HomeScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Status bar système visible ; on ne re-cache QUE la nav bar.
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onStart() {
        super.onStart()
        appWidgetHost.startListening()
    }

    override fun onStop() {
        super.onStop()
        appWidgetHost.stopListening()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val exclusionRects = listOf(
                    Rect(0, 0, 120, window.decorView.height),
                    Rect(window.decorView.width - 120, 0, window.decorView.width, window.decorView.height)
                )
                window.decorView.systemGestureExclusionRects = exclusionRects
            }
        }
    }
}
