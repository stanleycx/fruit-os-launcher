package com.stanleycx.fruitos

import android.content.ComponentName
import com.stanleycx.fruitos.data.AppInfo
import com.stanleycx.fruitos.data.HomeItem
import com.stanleycx.fruitos.data.LauncherState
import com.stanleycx.fruitos.data.WidgetLayout
import com.stanleycx.fruitos.data.WidgetPlacement
import com.stanleycx.fruitos.ui.home.placeAppFirstFree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Une app sortie d'un dossier (placée via [placeAppFirstFree]) ne doit JAMAIS atterrir dans
 * une cellule recouverte par un widget (sinon elle paraît "derrière" le widget). Bug v1.0.
 */
class PlaceAppFirstFreeTest {

    private fun pkg(item: HomeItem?): String? = (item as? HomeItem.App)?.app?.packageName
    private val app = AppInfo(label = "NEW", packageName = "new")

    /** Widget 2×2 en haut à gauche (page d'apps 0 = index pager 1) → couvre {0,1,4,5}. */
    private fun widget2x2at00() = WidgetLayout(
        listOf(
            WidgetPlacement(
                widgetId = "w", appWidgetId = 1,
                provider = ComponentName("p", "c"),
                pageIndex = 1, col = 0f, row = 0f, colSpan = 2, rowSpan = 2
            )
        )
    )

    @Test
    fun pulled_app_skips_widget_covered_slots() {
        val state = LauncherState(pages = listOf(emptyMap()), dock = emptyList())

        val res = placeAppFirstFree(state, app, widget2x2at00())
        val p0 = res.pages[0]

        // Aucune cellule sous le widget ne doit recevoir l'app.
        setOf(0, 1, 4, 5).forEach { assertNull("slot $it (sous widget) doit rester vide", p0[it]) }
        // Elle va dans la 1re cellule libre NON couverte = slot 2.
        assertEquals("new", pkg(p0[2]))
        assertEquals(1, res.pages.size)
    }

    @Test
    fun without_widgets_uses_first_slot() {
        val state = LauncherState(pages = listOf(emptyMap()), dock = emptyList())
        val res = placeAppFirstFree(state, app, WidgetLayout.Empty)
        assertEquals("new", pkg(res.pages[0][0]))
    }
}
