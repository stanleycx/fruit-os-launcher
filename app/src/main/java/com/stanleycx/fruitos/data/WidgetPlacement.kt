package com.stanleycx.fruitos.data

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Un widget placé quelque part dans le launcher. */
data class WidgetPlacement(
    val widgetId: String,          // UUID stable (≠ appWidgetId qui peut changer)
    val appWidgetId: Int,           // alloué par AppWidgetHost
    val provider: ComponentName,
    /** 0 = page widget dédiée, 1..N = index pager de la page d'app */
    val pageIndex: Int,
    val col: Float,                 // 0..3 (peut être .5 pour demi-colonne)
    val row: Float,                 // 0..5
    val colSpan: Int,               // 1..4
    val rowSpan: Int,               // 1..6
    val label: String? = null,      // nom personnalisé (null = nom du provider)
    val labelHidden: Boolean = false // masquer le nom sous le widget (écran d'accueil)
)

data class WidgetLayout(
    val placements: List<WidgetPlacement> = emptyList()
) {
    companion object {
        val Empty = WidgetLayout()
    }
}

/** Chevauchement de deux rectangles de grille (col/row en cellules, lignes ILLIMITÉES). */
fun rectsOverlap(
    aCol: Float, aRow: Float, aCs: Int, aRs: Int,
    bCol: Float, bRow: Float, bCs: Int, bRs: Int
): Boolean =
    aCol < bCol + bCs && bCol < aCol + aCs &&
            aRow < bRow + bRs && bRow < aRow + aRs

/**
 * Vrai si [candidate] chevauche un AUTRE widget de la même page (même pageIndex).
 * Basé sur un test rectangulaire (et non sur blockedSlots() qui plafonne à 6 lignes),
 * donc valable aussi sur la page widgets dédiée où les lignes sont illimitées.
 */
fun WidgetLayout.collidesOnPage(selfWidgetId: String, candidate: WidgetPlacement): Boolean =
    placements.any { other ->
        other.pageIndex == candidate.pageIndex &&
                other.widgetId != selfWidgetId &&
                rectsOverlap(
                    candidate.col, candidate.row, candidate.colSpan, candidate.rowSpan,
                    other.col, other.row, other.colSpan, other.rowSpan
                )
    }

/**
 * 1ère position (col,row) où un widget cs×rs tient dans une grille 4×[maxRows] sans chevaucher
 * aucun rectangle de [occupied] (balayage haut→bas, gauche→droite). null si rien ne tient.
 */
private fun firstFreeWidgetSlot(
    occupied: List<WidgetPlacement>, cs: Int, rs: Int, maxRows: Int
): Pair<Int, Int>? {
    for (r in 0..(maxRows - rs)) {
        for (c in 0..(4 - cs)) {
            val free = occupied.none {
                rectsOverlap(c.toFloat(), r.toFloat(), cs, rs, it.col, it.row, it.colSpan, it.rowSpan)
            }
            if (free) return c to r
        }
    }
    return null
}

/** Pose [widget] sur la 1ère page (à partir de [startPage]) où il tient ; cascade si pleine. */
private fun WidgetLayout.placeWidgetOverflow(
    widget: WidgetPlacement, startPage: Int, maxRows: Int
): WidgetLayout {
    var pageIdx = startPage
    var guard = 0
    while (guard++ < 16) {
        val pageWidgets = placements.filter { it.pageIndex == pageIdx }
        val slot = firstFreeWidgetSlot(pageWidgets, widget.colSpan, widget.rowSpan, maxRows)
        if (slot != null) {
            return WidgetLayout(placements + widget.copy(
                pageIndex = pageIdx, col = slot.first.toFloat(), row = slot.second.toFloat()
            ))
        }
        pageIdx++
    }
    return WidgetLayout(placements + widget.copy(pageIndex = pageIdx, col = 0f, row = 0f))
}

/**
 * Dépose le widget [widgetId] à (targetCol,targetRow) sur sa page en POUSSANT les voisins,
 * au lieu d'interdire le chevauchement :
 *  - le widget déplacé est clampé dans la grille (cols 0..4-cs, rows 0..[maxRows]-rs) → il
 *    reste sur sa page ;
 *  - les widgets de la même page qui NE le chevauchent pas restent en place ;
 *  - ceux qu'il chevauche sont relogés à la 1ère place libre ; s'ils ne tiennent plus sur
 *    la page, ils DÉBORDENT sur la (les) page(s) suivante(s).
 *
 * Travaille en index pager (page d'apps = pageIndex 1..N → débordement vers pageIndex+1).
 * Le caller doit garantir que les pages d'apps existent pour les pageIndex produits.
 */
fun WidgetLayout.dropWidgetWithPush(
    widgetId: String,
    targetCol: Int,
    targetRow: Int,
    maxRows: Int = 6,
): WidgetLayout {
    val dragged = placements.find { it.widgetId == widgetId } ?: return this
    val page = dragged.pageIndex
    val cs = dragged.colSpan
    val rs = dragged.rowSpan

    val draggedNew = dragged.copy(
        col = targetCol.coerceIn(0, 4 - cs).toFloat(),
        row = targetRow.coerceIn(0, (maxRows - rs).coerceAtLeast(0)).toFloat()
    )

    val samePageOthers = placements.filter { it.pageIndex == page && it.widgetId != widgetId }
    val otherPages = placements.filter { it.pageIndex != page }

    // Widgets restant en place (pas de chevauchement avec la cible) vs à reloger.
    val occupied = mutableListOf(draggedNew)
    val toRelocate = mutableListOf<WidgetPlacement>()
    for (o in samePageOthers.sortedWith(compareBy({ it.row }, { it.col }))) {
        val overlaps = rectsOverlap(
            draggedNew.col, draggedNew.row, draggedNew.colSpan, draggedNew.rowSpan,
            o.col, o.row, o.colSpan, o.rowSpan
        )
        if (overlaps) toRelocate.add(o) else occupied.add(o)
    }

    // Relogement sur la page, sinon débordement page suivante.
    val overflow = mutableListOf<WidgetPlacement>()
    for (o in toRelocate) {
        val slot = firstFreeWidgetSlot(occupied, o.colSpan, o.rowSpan, maxRows)
        if (slot != null) occupied.add(o.copy(col = slot.first.toFloat(), row = slot.second.toFloat()))
        else overflow.add(o)
    }

    var result = WidgetLayout(otherPages + occupied)
    for (o in overflow) result = result.placeWidgetOverflow(o, page + 1, maxRows)
    return result
}

/**
 * Ajoute un NOUVEAU widget en le posant à la 1ʳᵉ place libre de [startPage] (sans chevauchement) ;
 * si la page n'a pas de place pour le contenir ENTIÈREMENT, il cascade sur la page suivante
 * (« pas la place ⇒ page suivante »). [widget] doit déjà porter son colSpan/rowSpan.
 */
fun WidgetLayout.placeNewWidgetOverflow(widget: WidgetPlacement, startPage: Int, maxRows: Int = 6): WidgetLayout =
    placeWidgetOverflow(widget, startPage, maxRows)

/**
 * Déplace le widget [widgetId] vers [targetPage] : le retire de sa page actuelle et le repose à
 * la 1ʳᵉ place libre de [targetPage] (cascade sur les pages suivantes si pleine → « page pleine ⇒
 * page suivante »). No-op si le widget n'existe pas.
 */
fun WidgetLayout.moveWidgetToPage(widgetId: String, targetPage: Int, maxRows: Int = 6): WidgetLayout {
    val w = placements.find { it.widgetId == widgetId } ?: return this
    val without = WidgetLayout(placements.filter { it.widgetId != widgetId })
    return without.placeWidgetOverflow(w.copy(pageIndex = targetPage), targetPage, maxRows)
}

/** Retourne l'ensemble des indices de slots (0..23, grille 4×6) couverts par ce widget. */
fun WidgetPlacement.blockedSlots(): Set<Int> {
    val set = mutableSetOf<Int>()
    for (r in row.toInt() until (row + rowSpan).toInt().coerceAtMost(6)) {
        for (c in col.toInt() until (col + colSpan).toInt().coerceAtMost(4)) {
            set.add(r * 4 + c)
        }
    }
    return set
}

/**
 * Nettoie + migre un layout chargé depuis le disque :
 *  1. **Fantômes** : retire les placements dont l'appWidgetId n'existe plus
 *     (host réinitialisé, app du widget désinstallée…) → plus de cartes "indisponible".
 *  2. **Migration des spans** : si le provider expose `targetCellWidth/Height` (API 31+)
 *     et que le span enregistré est PLUS GRAND, on le réduit à la valeur cible
 *     (corrige les widgets sur-dimensionnés par l'ancien calcul `+1`).
 *     On ne fait que rétrécir — jamais agrandir — pour ne pas recouvrir d'icônes.
 *  3. **Clamp grille** : garantit col+colSpan ≤ 4 et des spans valides.
 *
 * Retourne le même objet si rien n'a changé (évite une réécriture disque inutile).
 */
fun WidgetLayout.cleanAndMigrate(awm: AppWidgetManager): WidgetLayout {
    val migrated = placements.mapNotNull { p ->
        val info = runCatching { awm.getAppWidgetInfo(p.appWidgetId) }.getOrNull()
            ?: return@mapNotNull null   // fantôme → drop

        // La page widgets dédiée (pageIndex 0) scrolle verticalement et autorise le resize
        // libre jusqu'à 12 lignes : on n'y applique NI la réduction targetCell*, NI le clamp
        // à 6 lignes, sinon les tailles choisies par l'utilisateur seraient écrasées à chaque
        // (re)démarrage — ce qui cassait la restauration de cette page.
        val isWidgetPage = p.pageIndex == 0
        val maxRows = if (isWidgetPage) 12 else 6

        // On NE réduit plus selon targetCell* : sinon les widgets redimensionnés par l'utilisateur
        // seraient ramenés de force à la taille par défaut (targetCell=2) à chaque chargement.
        // targetCell ne sert qu'à la taille de POSE (voir placeWidget dans HomeScreen).
        var cs = p.colSpan
        var rs = p.rowSpan

        cs = cs.coerceIn(1, 4)
        rs = rs.coerceIn(1, maxRows)
        val col = p.col.coerceIn(0f, (4 - cs).toFloat())
        val row = p.row.coerceAtLeast(0f)

        if (cs == p.colSpan && rs == p.rowSpan && col == p.col && row == p.row) p
        else p.copy(col = col, row = row, colSpan = cs, rowSpan = rs)
    }

    val unchanged = migrated.size == placements.size &&
            migrated.indices.all { migrated[it] === placements[it] }
    return if (unchanged) this else WidgetLayout(migrated)
}

// ── Sérialisation JSON ────────────────────────────────────────────────────────

fun widgetLayoutToJson(layout: WidgetLayout): String {
    val root = JSONObject()
    root.put("v", 1)
    val arr = JSONArray()
    for (p in layout.placements) {
        val o = JSONObject()
        o.put("id", p.widgetId)
        o.put("awid", p.appWidgetId)
        o.put("pkg", p.provider.packageName)
        o.put("cls", p.provider.className)
        o.put("page", p.pageIndex)
        o.put("col", p.col.toDouble())
        o.put("row", p.row.toDouble())
        o.put("cs", p.colSpan)
        o.put("rs", p.rowSpan)
        p.label?.let { o.put("label", it) }
        if (p.labelHidden) o.put("lhid", true)
        arr.put(o)
    }
    root.put("placements", arr)
    return root.toString()
}

fun widgetLayoutFromJson(raw: String): WidgetLayout {
    if (raw.isBlank()) return WidgetLayout.Empty
    return try {
        val root = JSONObject(raw)
        val arr = root.optJSONArray("placements") ?: return WidgetLayout.Empty
        val placements = mutableListOf<WidgetPlacement>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val pkg = o.optString("pkg").ifBlank { continue }
            val cls = o.optString("cls").ifBlank { continue }
            placements.add(
                WidgetPlacement(
                    widgetId = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                    appWidgetId = o.optInt("awid", -1),
                    provider = ComponentName(pkg, cls),
                    pageIndex = o.optInt("page", 1),
                    col = o.optDouble("col", 0.0).toFloat(),
                    row = o.optDouble("row", 0.0).toFloat(),
                    colSpan = o.optInt("cs", 2).coerceIn(1, 4),
                    rowSpan = o.optInt("rs", 2).coerceIn(1, 6),
                    label = o.optString("label", "").ifBlank { null },
                    labelHidden = o.optBoolean("lhid", false)
                )
            )
        }
        WidgetLayout(placements)
    } catch (_: Exception) {
        WidgetLayout.Empty
    }
}
