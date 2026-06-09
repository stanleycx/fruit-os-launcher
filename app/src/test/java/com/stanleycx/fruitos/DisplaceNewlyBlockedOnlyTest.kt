package com.stanleycx.fruitos

import com.stanleycx.fruitos.data.AppInfo
import com.stanleycx.fruitos.data.HomeItem
import com.stanleycx.fruitos.data.LauncherState
import com.stanleycx.fruitos.ui.home.displaceNewlyBlockedOnly
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Réarrangement CHIRURGICAL à l'agrandissement d'un widget : seules les icônes des cellules
 * nouvellement recouvertes bougent ; les autres restent à leur place (tant que la page n'est
 * pas pleine).
 *
 * Grille 4×6 = 24 slots. Widget initial 2×1 = {0,1}, agrandi en 2×2 = {0,1,4,5}
 * → newlyBlocked = {4,5}.
 */
class DisplaceNewlyBlockedOnlyTest {

    private fun app(name: String): HomeItem = HomeItem.App(AppInfo(label = name, packageName = name))
    private fun pkg(item: HomeItem?): String? = (item as? HomeItem.App)?.app?.packageName

    private val oldWidget = setOf(0, 1)
    private val newWidget = setOf(0, 1, 4, 5)

    @Test
    fun only_covered_icons_move_others_stay() {
        // A en slot 2 (non recouverte), C/D en 4/5 (recouvertes), X en 20 (non recouverte).
        val page = mapOf(2 to app("A"), 4 to app("C"), 5 to app("D"), 20 to app("X"))
        val state = LauncherState(pages = listOf(page), dock = emptyList())

        val res = displaceNewlyBlockedOnly(state, appPageIndex = 0, newBlocked = newWidget, oldBlocked = oldWidget)
        val p0 = res.pages[0]

        // Aucune icône sous le widget.
        newWidget.forEach { assertNull("slot $it doit rester vide (sous widget)", p0[it]) }

        // Les NON recouvertes ne bougent pas : A reste en 2, X reste en 20.
        assertEquals("A", pkg(p0[2]))
        assertEquals("X", pkg(p0[20]))

        // C et D (recouvertes) vont aux premières cellules libres : 3 puis 6 (2 occupé, 4/5 condamnés).
        assertEquals("C", pkg(p0[3]))
        assertEquals("D", pkg(p0[6]))

        // Une seule page : rien n'a débordé.
        assertEquals(1, res.pages.size)
    }

    @Test
    fun widget_move_does_not_disturb_far_apps() {
        // Bug rapporté : widget déplacé de {0,1,4,5} → {1,2,5,6}, apps en 20,21,22 au loin.
        // Les cellules nouvellement recouvertes sont {2,6}, toutes deux VIDES → rien ne bouge.
        val oldPos = setOf(0, 1, 4, 5)
        val newPos = setOf(1, 2, 5, 6)
        val page = mapOf(20 to app("A"), 21 to app("B"), 22 to app("C"))
        val state = LauncherState(pages = listOf(page), dock = emptyList())

        val res = displaceNewlyBlockedOnly(state, appPageIndex = 0, newBlocked = newPos, oldBlocked = oldPos)
        val p0 = res.pages[0]

        // Les apps du fond de page restent STRICTEMENT en place.
        assertEquals("A", pkg(p0[20]))
        assertEquals("B", pkg(p0[21]))
        assertEquals("C", pkg(p0[22]))
        assertEquals("seules les 3 apps, aucune déplacée", 3, p0.size)
        assertEquals(1, res.pages.size)
    }

    @Test
    fun widget_move_only_relocates_app_under_new_cell() {
        // Variante : une app EST sous une cellule nouvellement recouverte.
        // Widget {0,1,4,5} → {1,2,5,6} : newlyBlocked = {2,6}. App Z en slot 2 → doit bouger
        // vers la 1ère cellule libre (slot 0, libéré par le déplacement). App lointaine intacte.
        val oldPos = setOf(0, 1, 4, 5)
        val newPos = setOf(1, 2, 5, 6)
        val page = mapOf(2 to app("Z"), 20 to app("Far"))
        val state = LauncherState(pages = listOf(page), dock = emptyList())

        val res = displaceNewlyBlockedOnly(state, appPageIndex = 0, newBlocked = newPos, oldBlocked = oldPos)
        val p0 = res.pages[0]

        newPos.forEach { assertNull("slot $it sous le widget", p0[it]) }
        assertEquals("Z", pkg(p0[0]))     // recasée dans la 1ère cellule libre (slot 0 libéré)
        assertEquals("Far", pkg(p0[20]))  // lointaine intacte
        assertEquals(1, res.pages.size)
    }

    @Test
    fun full_page_cascades_covered_first_and_pushes_tail_to_next_page() {
        // Exemple de l'utilisateur : widget 0..3 agrandi en 0..7, PAGE PLEINE.
        val oldTop = setOf(0, 1, 2, 3)
        val newTop = setOf(0, 1, 2, 3, 4, 5, 6, 7)
        // Apps a4..a23 dans les slots 4..23 (slots 0..3 sous l'ancien widget).
        val page = (4 until 24).associateWith { app("a$it") }
        val state = LauncherState(pages = listOf(page), dock = emptyList())

        val res = displaceNewlyBlockedOnly(state, appPageIndex = 0, newBlocked = newTop, oldBlocked = oldTop)
        val p0 = res.pages[0]
        val p1 = res.pages.getOrNull(1) ?: emptyMap()

        // Aucune icône sous le widget (0..7).
        newTop.forEach { assertNull("slot $it doit rester vide (sous widget)", p0[it]) }

        // Les recouvertes (ex-4,5,6,7) prennent les 1ères places dispo, JUSTE APRÈS le widget.
        assertEquals("a4", pkg(p0[8]))
        assertEquals("a5", pkg(p0[9]))
        assertEquals("a6", pkg(p0[10]))
        assertEquals("a7", pkg(p0[11]))

        // Les autres se décalent : a8 (ex-8) passe en 12, a19 finit en 23.
        assertEquals("a8", pkg(p0[12]))
        assertEquals("a19", pkg(p0[23]))

        // La queue (a20..a23) déborde sur la page suivante, dans l'ordre.
        assertEquals("a20", pkg(p1[0]))
        assertEquals("a21", pkg(p1[1]))
        assertEquals("a22", pkg(p1[2]))
        assertEquals("a23", pkg(p1[3]))
    }
}
