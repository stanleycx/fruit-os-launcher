package com.stanleycx.fruitos

import android.content.ComponentName
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stanleycx.fruitos.data.WidgetLayout
import com.stanleycx.fruitos.data.WidgetPlacement
import com.stanleycx.fruitos.data.dropWidgetWithPush
import com.stanleycx.fruitos.data.rectsOverlap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.runner.RunWith
import org.junit.Test

@RunWith(AndroidJUnit4::class)
class WidgetPushTest {

    private val prov = ComponentName("pkg", "cls")

    private fun w(id: String, page: Int, col: Int, row: Int, cs: Int = 2, rs: Int = 2) =
        WidgetPlacement(id, id.hashCode(), prov, page, col.toFloat(), row.toFloat(), cs, rs)

    private fun overlap(a: WidgetPlacement, b: WidgetPlacement) =
        rectsOverlap(a.col, a.row, a.colSpan, a.rowSpan, b.col, b.row, b.colSpan, b.rowSpan)

    /** Déposer un widget sur un autre : le voisin chevauché est RELOGÉ (pas de snap-back). */
    @Test
    fun push_relocates_overlapped_widget_same_page() {
        val layout = WidgetLayout(listOf(w("clock", 1, 0, 0), w("dino", 1, 0, 3)))
        // Dino (rows 3-4) tiré sur (0,0) → chevauche l'horloge.
        val res = layout.dropWidgetWithPush("dino", targetCol = 0, targetRow = 0, maxRows = 6)

        val dino = res.placements.first { it.widgetId == "dino" }
        val clock = res.placements.first { it.widgetId == "clock" }

        // Dino est bien à la cible.
        assertEquals(0, dino.col.toInt()); assertEquals(0, dino.row.toInt())
        // L'horloge a été décalée mais reste sur la page, sans chevauchement.
        assertEquals(1, clock.pageIndex)
        assertFalse("les deux widgets ne doivent plus se chevaucher", overlap(dino, clock))
    }

    /** Page pleine : un widget qui ne tient plus DÉBORDE sur la page suivante. */
    @Test
    fun overflow_pushes_widget_to_next_page_when_full() {
        // Page 1 saturée : a(4x2 @row0), b(4x2 @row2), c(2x2 @col0,row4), d(2x2 @col2,row4) = grille 4x6 pleine.
        val layout = WidgetLayout(listOf(
            w("a", 1, 0, 0, cs = 4, rs = 2),
            w("b", 1, 0, 2, cs = 4, rs = 2),
            w("c", 1, 0, 4),
            w("d", 1, 2, 4),
        ))
        // On tire "c" sur (0,0) → chevauche "a". "a" (4x2) ne tient plus sur la page → page 2.
        val res = layout.dropWidgetWithPush("c", targetCol = 0, targetRow = 0, maxRows = 6)

        val a = res.placements.first { it.widgetId == "a" }
        val c = res.placements.first { it.widgetId == "c" }

        assertEquals("c reste sur la page d'origine", 1, c.pageIndex)
        assertEquals(0, c.col.toInt()); assertEquals(0, c.row.toInt())
        assertEquals("a a débordé sur la page suivante", 2, a.pageIndex)

        // Aucun chevauchement entre widgets d'une même page dans le résultat.
        val byPage = res.placements.groupBy { it.pageIndex }
        byPage.values.forEach { pagePlacements ->
            for (i in pagePlacements.indices) for (j in i + 1 until pagePlacements.size) {
                assertFalse(
                    "chevauchement résiduel sur la page ${pagePlacements[i].pageIndex}",
                    overlap(pagePlacements[i], pagePlacements[j])
                )
            }
        }
    }
}
