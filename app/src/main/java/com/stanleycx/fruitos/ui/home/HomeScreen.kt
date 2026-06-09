package com.stanleycx.fruitos.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.stanleycx.fruitos.data.AppInfo
import com.stanleycx.fruitos.data.AppRepository
import com.stanleycx.fruitos.data.LauncherState
import com.stanleycx.fruitos.data.LayoutRepository
import com.stanleycx.fruitos.data.buildLauncherState
import com.stanleycx.fruitos.data.toLayout
import com.stanleycx.fruitos.ui.components.PageIndicatorPill
import com.stanleycx.fruitos.ui.dock.Dock
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.chrisbanes.haze.hazeSource
import com.stanleycx.fruitos.ui.components.WallpaperBackground
import com.stanleycx.fruitos.ui.components.LocalWallpaperBitmap
import com.stanleycx.fruitos.ui.components.rememberWallpaperBitmap
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.derivedStateOf
import com.stanleycx.fruitos.ui.components.SpotlightTriggerButton
import com.stanleycx.fruitos.ui.components.GlassQuickActionButton
import com.stanleycx.fruitos.ui.components.GlassLevel
import com.stanleycx.fruitos.ui.components.GlassTint
import com.stanleycx.fruitos.ui.components.GlossLevel
import com.stanleycx.fruitos.ui.components.LoupeLevel
import com.stanleycx.fruitos.ui.components.glass
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import com.stanleycx.fruitos.ui.library.AppLibraryScreen
import com.stanleycx.fruitos.ui.library.rememberAppLibraryState
import com.stanleycx.fruitos.data.HomeItem
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.stanleycx.fruitos.ui.components.ContextMenu
import com.stanleycx.fruitos.ui.components.ContextMenuAction
import com.stanleycx.fruitos.ui.components.AppIcon
import com.stanleycx.fruitos.ui.components.FolderIcon
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import com.stanleycx.fruitos.data.BindWidgetContract
import com.stanleycx.fruitos.data.BindWidgetInput
import com.stanleycx.fruitos.data.ConfigureWidgetContract
import com.stanleycx.fruitos.data.ConfigureWidgetInput
import com.stanleycx.fruitos.data.WidgetLayout
import com.stanleycx.fruitos.data.cleanAndMigrate
import com.stanleycx.fruitos.data.collidesOnPage
import com.stanleycx.fruitos.data.WidgetPlacement
import com.stanleycx.fruitos.data.blockedSlots
import com.stanleycx.fruitos.data.dropWidgetWithPush
import com.stanleycx.fruitos.data.moveWidgetToPage
import com.stanleycx.fruitos.data.placeNewWidgetOverflow
import com.stanleycx.fruitos.data.WidgetRepository
import com.stanleycx.fruitos.ui.widget.LocalAppWidgetHost
import com.stanleycx.fruitos.ui.widget.LocalAppWidgetManager
import com.stanleycx.fruitos.ui.widget.WIDGET_PAGE_INDEX
import com.stanleycx.fruitos.ui.widget.WidgetCard
import com.stanleycx.fruitos.ui.widget.WidgetPage
import com.stanleycx.fruitos.ui.widget.WidgetPickerSheet
import com.stanleycx.fruitos.ui.widget.rememberWidgetResizeState
import androidx.compose.foundation.layout.BoxWithConstraints
import java.util.UUID
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.GridView

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement







@Composable
fun HomeScreen() {
    val wallpaperBitmap = rememberWallpaperBitmap()
    // Une seule InfiniteTransition partagée → remplace les ~28 instances per-icône en mode édition.
    val jiggleTransition = rememberInfiniteTransition(label = "jiggle")
    val jiggleBase by jiggleTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "jiggle_base"
    )
    CompositionLocalProvider(
        LocalWallpaperBitmap provides wallpaperBitmap,
        LocalJiggleBase provides jiggleBase
    ) {
        HomeScreenContent()
    }
}

@Composable
private fun HomeScreenContent() {
    val context = LocalContext.current
    val appRepository = remember { AppRepository(context) }
    val layoutRepository = remember { LayoutRepository(context) }
    val scope = rememberCoroutineScope()
    // Debounce pour les saves (DataStore + JSON) pendant drags/ réorgs rapides.
    // Évite 10+ sérialisations inutiles par réarrangement.
    // Non-observable (plain ref) : écrire dans mutableStateOf<Job?> déclencherait des
    // recompositions inutiles de HomeScreenContent à chaque cancel/restart de job.
    val layoutSaveJob = remember { object { var job: kotlinx.coroutines.Job? = null } }
    val mergeDwellJob = remember { object { var job: kotlinx.coroutines.Job? = null } }

    // Cache de toutes les apps installées (utilisé par le Spotlight)
    // Mutable pour se mettre à jour quand une app est installée / désinstallée
    // PERF: initialisé vide ; le chargement (PM query) se fait en LaunchedEffect pour ne pas bloquer
    // le premier frame de composition du launcher.
    var allInstalledApps by remember { mutableStateOf(emptyList<AppInfo>()) }

    // Chargement async des apps (rapide maintenant sans les loadIcon)
    // Sur Default pour ne pas taxer le main thread même brièvement (PM binder + string work).
    LaunchedEffect(Unit) {
        allInstalledApps = withContext(kotlinx.coroutines.Dispatchers.Default) {
            appRepository.loadInstalledApps()
        }
    }

    // État de la permission UsageStats (re-vérifié quand on revient des paramètres)
    var hasUsagePermission by remember {
        mutableStateOf(com.stanleycx.fruitos.data.UsageStatsHelper.hasPermission(context))
    }

    // Packages les plus utilisés → convertis en AppInfo
    val suggestedApps = remember(hasUsagePermission, allInstalledApps) {
        if (hasUsagePermission) {
            val topPackages = com.stanleycx.fruitos.data.UsageStatsHelper
                .getMostUsedPackages(context, limit = 8)
            topPackages.mapNotNull { pkg ->
                allInstalledApps.find { it.packageName == pkg }
            }
        } else {
            emptyList()
        }
    }

    // Re-vérifie la permission UsageStats quand l'app revient au premier plan
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasUsagePermission = com.stanleycx.fruitos.data.UsageStatsHelper.hasPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var state by remember { mutableStateOf<LauncherState?>(null) }

    // On utilise allInstalledApps (chargé async ci-dessus) pour éviter double query PM au démarrage.
    // Si allInstalledApps arrive après, on rebuild (rare, <50ms).
    LaunchedEffect(allInstalledApps) {
        if (allInstalledApps.isNotEmpty()) {
            val layout = layoutRepository.load()
            state = buildLauncherState(allInstalledApps, layout)
            // Pré-chauffe le cache d'icônes sur IO : les appels binder PM + render bitmap
            // se font en arrière-plan. Quand l'App Library s'ouvre, toutes les icônes sont
            // déjà en mémoire → zéro appel synchrone pendant la composition.
            val appsToWarm = allInstalledApps
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                appsToWarm.forEach { app ->
                    com.stanleycx.fruitos.ui.components.IconCache.getOrRender(app.packageName, context)
                }
            }
        }
    }

    // ── Widget state (déclaré avant updateState pour que removeEmptyPages y ait accès) ──
    val widgetRepository = remember { WidgetRepository(context) }
    var widgetLayout by remember { mutableStateOf(WidgetLayout.Empty) }
    val widgetResizeState = rememberWidgetResizeState()

    LaunchedEffect(Unit) {
        val loaded = widgetRepository.load()
        // Nettoie les widgets fantômes (appWidgetId invalide après réinstall/désinstall)
        // et migre les spans sur-dimensionnés vers targetCell*. Réécrit seulement si ça change.
        val cleaned = loaded.cleanAndMigrate(AppWidgetManager.getInstance(context))
        widgetLayout = cleaned
        if (cleaned !== loaded) widgetRepository.save(cleaned)
    }

    val updateState: (LauncherState) -> Unit = { newState ->
        // La suppression des pages vides est gérée par un LaunchedEffect dédié (au repos,
        // avec réindexation des widgets) — voir plus bas. Ici on applique l'état tel quel.
        state = newState

        layoutSaveJob.job?.cancel()
        layoutSaveJob.job = scope.launch {
            kotlinx.coroutines.delay(280) // debounce
            // Lit l'état FRAIS au moment du save (et non `newState` capturé) : si le
            // LaunchedEffect de suppression des pages vides a nettoyé l'état entre-temps,
            // c'est la version nettoyée qui est persistée (évite de réécrire une page vide).
            (state ?: newState).let { layoutRepository.save(it.toLayout()) }
        }
    }

    // Ref fraîche de updateState pour les callbacks capturés une seule fois (BroadcastReceiver)
    val updateStateRef = remember { mutableStateOf(updateState) }
    updateStateRef.value = updateState

    val updateWidgetLayout: (WidgetLayout) -> Unit = { newLayout ->
        widgetLayout = newLayout
        scope.launch { widgetRepository.save(newLayout) }
    }

    // Picker : null = fermé ; "widget_page" ou "app_page:<pagerIndex>"
    var widgetPickerDestination by remember { mutableStateOf<String?>(null) }

    // État du widget en cours de binding
    // pendingWidgetDest stocke directement l'index pager sous forme de String
    val pendingWidgetInfo = remember { mutableStateOf<android.appwidget.AppWidgetProviderInfo?>(null) }
    val pendingWidgetId = remember { mutableStateOf(-1) }
    val pendingWidgetPageIdx = remember { mutableStateOf(WIDGET_PAGE_INDEX) }

    val awh = LocalAppWidgetHost.current
    val awm = LocalAppWidgetManager.current

    // Suppression fiable d'un widget : libère l'allocation host (appWidgetId) PUIS retire le
    // placement par widgetId. Centralisé pour rester cohérent entre page d'apps, page widgets
    // et menus contextuels (lit widgetLayout en direct via le delegate d'état).
    val removeWidget: (String) -> Unit = removeW@{ id ->
        val placement = widgetLayout.placements.find { it.widgetId == id } ?: return@removeW
        if (placement.appWidgetId >= 0) runCatching { awh.deleteAppWidgetId(placement.appWidgetId) }
        updateWidgetLayout(widgetLayout.copy(
            placements = widgetLayout.placements.filter { it.widgetId != id }
        ))
    }

    // Lance le placement une fois le binding (et la config optionnelle) terminés.
    // N'utilise que les mutableState ci-dessus, pas de forward refs.
    val placeWidget: () -> Unit = placeW@{
        val id = pendingWidgetId.value
        val info = pendingWidgetInfo.value ?: return@placeW
        val pageIdx = pendingWidgetPageIdx.value

        val dm = context.resources.displayMetrics
        val screenWidthDp = dm.widthPixels / dm.density
        val screenHeightDp = dm.heightPixels / dm.density
        val cellWDp = (screenWidthDp - 24f) / 4f
        // Hauteur de cellule réelle d'une page d'apps (≈ celle utilisée par l'overlay) :
        // hauteur écran − status bar (~32) − réserve dock (184) − marges (16), sur 6 rangées.
        val appCellHDp = ((screenHeightDp - 32f - 184f - 16f) / 6f).coerceAtLeast(64f)
        val isWidgetPage = pageIdx == WIDGET_PAGE_INDEX
        val cellHDp = if (isWidgetPage) 100f else appCellHDp

        // Spans voulus par le widget en CELLULES (API 31+, toujours dispo car minSdk=31).
        // 0 = non renseigné → on retombe sur un calcul par minWidth/minHeight avec ceil
        // (et non +1 systématique, qui sur-dimensionnait toujours d'une cellule).
        val targetCols = info.targetCellWidth
        val targetRows = info.targetCellHeight
        val minCols = when {
            isWidgetPage -> 4
            targetCols in 1..4 -> targetCols
            else -> kotlin.math.ceil(info.minWidth / cellWDp).toInt().coerceIn(1, 4)
        }
        val minRows = when {
            targetRows in 1..6 -> targetRows
            else -> kotlin.math.ceil(info.minHeight / cellHDp).toInt().coerceIn(1, 6)
        }

        val placement = WidgetPlacement(
            widgetId = UUID.randomUUID().toString(),
            appWidgetId = id,
            provider = info.provider,
            pageIndex = pageIdx,
            col = 0f,
            row = (widgetLayout.placements.filter { it.pageIndex == pageIdx }
                .maxOfOrNull { it.row + it.rowSpan } ?: 0f),
            colSpan = minCols,
            rowSpan = minRows
        )

        if (pageIdx != WIDGET_PAGE_INDEX) {
            // Page d'apps : pose à la 1ʳᵉ place libre (4×6) ; si le widget ne tient pas
            // ENTIÈREMENT → page suivante (création de la page au besoin). Évite qu'il déborde
            // sous le bouton Recherche.
            val newLayout = widgetLayout.placeNewWidgetOverflow(placement, pageIdx, maxRows = 6)
            val landed = newLayout.placements.find { it.widgetId == placement.widgetId }
            var st = state ?: return@placeW
            if (landed != null) {
                val landedAppIdx = landed.pageIndex - 1
                while (st.pages.size <= landedAppIdx) st = st.copy(pages = st.pages + emptyMap())
                // Reflow des apps de la page d'arrivée (le widget y bloque de nouveaux slots).
                st = displaceNewlyBlockedOnly(st, landedAppIdx, landed.blockedSlots(), emptySet())
            }
            updateState(st)
            updateWidgetLayout(newLayout)
        } else {
            // Page widgets dédiée (scroll vertical) : empilement simple sous les widgets existants.
            updateWidgetLayout(widgetLayout.copy(placements = widgetLayout.placements + placement))
        }
        pendingWidgetId.value = -1
        pendingWidgetInfo.value = null
    }

    val configureLauncher = rememberLauncherForActivityResult(ConfigureWidgetContract()) { ok ->
        if (ok) placeWidget()
        else {
            val id = pendingWidgetId.value
            if (id >= 0) awh.deleteAppWidgetId(id)
            pendingWidgetId.value = -1
            pendingWidgetInfo.value = null
        }
    }

    // Relance la configuration d'un widget déjà placé (depuis le menu contextuel)
    val reconfigureLauncher = rememberLauncherForActivityResult(ConfigureWidgetContract()) { /* widget se reconfigure lui-même */ }

    val bindLauncher = rememberLauncherForActivityResult(BindWidgetContract()) { granted ->
        val id = pendingWidgetId.value
        val info = pendingWidgetInfo.value
        if (!granted || id < 0 || info == null) {
            if (id >= 0) awh.deleteAppWidgetId(id)
            pendingWidgetId.value = -1
            pendingWidgetInfo.value = null
            return@rememberLauncherForActivityResult
        }
        val freshInfo = awm.getAppWidgetInfo(id)
        if (freshInfo?.configure != null) {
            configureLauncher.launch(ConfigureWidgetInput(id, freshInfo.configure))
        } else {
            placeWidget()
        }
    }

    // dest = "widget_page" ou "app_page:<pagerIndex>"
    val startAddWidget: (android.appwidget.AppWidgetProviderInfo, String) -> Unit =
        { providerInfo, dest ->
            widgetPickerDestination = null
            val pageIdx = when {
                dest == "widget_page" -> WIDGET_PAGE_INDEX
                dest.startsWith("app_page:") ->
                    dest.removePrefix("app_page:").toIntOrNull() ?: 1
                else -> WIDGET_PAGE_INDEX
            }
            val id = awh.allocateAppWidgetId()
            pendingWidgetInfo.value = providerInfo
            pendingWidgetId.value = id
            pendingWidgetPageIdx.value = pageIdx
            if (!awm.bindAppWidgetIdIfAllowed(id, providerInfo.provider)) {
                bindLauncher.launch(BindWidgetInput(id, providerInfo.provider))
            } else {
                val info = awm.getAppWidgetInfo(id)
                if (info?.configure != null) {
                    configureLauncher.launch(ConfigureWidgetInput(id, info.configure))
                } else {
                    placeWidget()
                }
            }
        }
    // ─────────────────────────────────────────────────────────────────────────

    val currentState = state ?: return

    // Compteurs de notifications (mis à jour en temps réel par NotificationService)
    val notificationCounts by com.stanleycx.fruitos.data.NotificationService.notificationCounts
        .collectAsState()

    // Réf "fraîche" de l'état : lue par le pointerInput(Unit) global (évite stale closure)
    val currentStateRef = remember { mutableStateOf(currentState) }
    currentStateRef.value = currentState

    val editMode = rememberEditModeState()
    val dragState = rememberDragState()

    // hazeState principal → utilisé uniquement par tous les éléments .glass() (Dock, boutons, cartes, etc.)
    val hazeState = remember { dev.chrisbanes.haze.HazeState() }

    val spotlightState = com.stanleycx.fruitos.ui.spotlight.rememberSpotlightState()
    val controlCenterState = com.stanleycx.fruitos.ui.controlcenter.rememberControlCenterState()
    val appLibraryState = rememberAppLibraryState()
    // Dossier actuellement ouvert en plein écran : (pageIndex, slotIndex), ou null.
    var openFolderLoc by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val openFolder = openFolderLoc?.let { (p, s) ->
        currentState.pages.getOrNull(p)?.get(s) as? HomeItem.Folder
    }

    val renameOpenFolder: (String) -> Unit = renameFolder@{ newName ->
        val loc = openFolderLoc ?: return@renameFolder
        val (p, s) = loc
        val folder = currentState.pages.getOrNull(p)?.get(s) as? HomeItem.Folder
            ?: return@renameFolder
        val finalName = newName.ifBlank { folder.name }
        if (finalName == folder.name) return@renameFolder
        val newPages = currentState.pages.toMutableList()
        val page = newPages[p].toMutableMap()
        page[s] = folder.copy(name = finalName)
        newPages[p] = page
        updateState(currentState.copy(pages = newPages))
    }

    val reorderOpenFolder: (List<AppInfo>) -> Unit = reorder@{ newOrder ->
        val (p, s) = openFolderLoc ?: return@reorder
        val folder = currentState.pages.getOrNull(p)?.get(s) as? HomeItem.Folder ?: return@reorder
        if (newOrder.map { it.packageName } == folder.apps.map { it.packageName }) return@reorder
        val newPages = currentState.pages.toMutableList()
        val page = newPages[p].toMutableMap()
        page[s] = folder.copy(apps = newOrder)
        newPages[p] = page
        updateState(currentState.copy(pages = newPages))
    }

    val pullOutOfFolder: (AppInfo) -> Unit = pull@{ app ->
        val (p, s) = openFolderLoc ?: return@pull
        val folder = currentState.pages.getOrNull(p)?.get(s) as? HomeItem.Folder ?: return@pull
        val remaining = folder.apps.filter { it.packageName != app.packageName }

        val pages = currentState.pages.map { it.toMutableMap() }.toMutableList()
        when {
            remaining.isEmpty() -> pages[p].remove(s)
            remaining.size == 1 -> pages[p][s] = HomeItem.App(remaining.first())  // dissolution Fruit OS
            else -> pages[p][s] = folder.copy(apps = remaining)
        }
        var newState = currentState.copy(pages = pages.map { it.toMap() })
        newState = placeAppFirstFree(newState, app, widgetLayout)
        updateState(newState)
        openFolderLoc = null
        com.stanleycx.fruitos.ui.components.Haptics.light(context)
    }

    val removeFromOpenFolder: (AppInfo) -> Unit = removeF@{ app ->
        val (p, s) = openFolderLoc ?: return@removeF
        val folder = currentState.pages.getOrNull(p)?.get(s) as? HomeItem.Folder ?: return@removeF
        val remaining = folder.apps.filter { it.packageName != app.packageName }
        val pages = currentState.pages.map { it.toMutableMap() }.toMutableList()
        when {
            // Retirer du home = l'app sort du dossier mais reste dans l'App Library.
            remaining.isEmpty() -> { pages[p].remove(s); openFolderLoc = null }
            remaining.size == 1 -> pages[p][s] = HomeItem.App(remaining.first())  // dissolution
            else -> pages[p][s] = folder.copy(apps = remaining)
        }
        // Ajoute dans hidden pour éviter que l'app réapparaisse au redémarrage du launcher
        updateState(currentState.copy(
            pages = pages.map { it.toMap() },
            hidden = currentState.hidden + app.packageName
        ))
        com.stanleycx.fruitos.ui.components.Haptics.light(context)
    }

    // Nombre de pages d'apps (home).
    val appPageCount = currentState.pages.size.coerceAtLeast(1)
    // Page widget = index 0 (swipe droite depuis la page 1).
    // Pages d'apps = indices 1..appPageCount.
    // App Library = appPageCount + 1.
    val appPagesStart = 1   // WIDGET_PAGE_OFFSET
    val libraryPageIndex = appPagesStart + appPageCount
    val totalPageCount = libraryPageIndex + 1

    val pagerState = rememberPagerState(
        initialPage = appPagesStart,  // ouvre sur la première page d'apps
        pageCount = { totalPageCount }
    )


    // ── Suppression automatique des pages d'apps devenues totalement vides ──────────
    // Détecte (icône OU widget) par page et réindexe pages + widgets. Gardé HORS drag /
    // changement de page pour ne pas faire disparaître une page en cours de manipulation.
    // Recale la page courante en douceur si elle se retrouve hors limites.
    LaunchedEffect(currentState, widgetLayout, dragState.isDragging, dragState.isChangingPage) {
        if (dragState.isDragging || dragState.isChangingPage) return@LaunchedEffect
        val (newState, newLayout) = removeEmptyPages(currentState, widgetLayout)
        if (newState !== currentState) {
            state = newState
            currentStateRef.value = newState
            if (newLayout !== widgetLayout) {
                widgetLayout = newLayout
                scope.launch { widgetRepository.save(newLayout) }
            }
            scope.launch { layoutRepository.save(newState.toLayout()) }

            // Recale le pager si la page courante (une page d'apps) dépasse la nouvelle dernière.
            val newAppCount = newState.pages.size.coerceAtLeast(1)
            val newLastAppPager = appPagesStart + newAppCount - 1
            val cur = pagerState.currentPage
            if (cur in appPagesStart..(libraryPageIndex - 1) && cur > newLastAppPager) {
                pagerState.animateScrollToPage(newLastAppPager)
            }
        }
    }

    // True quand l'utilisateur est sur la page App Library.
    val onLibraryPage = pagerState.currentPage >= libraryPageIndex
    // True quand l'utilisateur est sur la page dédiée widgets (index 0).
    val onWidgetPage = pagerState.currentPage == WIDGET_PAGE_INDEX

    // Quand on quitte la page App Library, on réinitialise sa recherche / dossier ouvert.
    LaunchedEffect(onLibraryPage) {
        if (!onLibraryPage) appLibraryState.reset()
    }

    // Bouton "Recherche" qui apparaît après 2 secondes sans scroll.
    // Sur une seule page, on le montre dès le départ (pas de point indicateur utile).
    var showSearchButton by remember(currentState.pages.size) {
        mutableStateOf(currentState.pages.size <= 1)
    }

    // === État du menu contextuel (géré ici pour que l'overlay sombre couvre VRAIMENT tout l'écran + dock) ===
    var contextMenuItem by remember { mutableStateOf<HomeItem?>(null) }
    var contextMenuSourceRect by remember { mutableStateOf<Rect?>(null) }

    // Menu contextuel widget (long press sur un widget)
    var widgetContextMenuId by remember { mutableStateOf<String?>(null) }
    var widgetContextMenuPos by remember { mutableStateOf<Offset?>(null) }


    // === Nouveau : menu rapide sur long press dans le vide (Paramètres + Widgets) ===
    var showHomeQuickMenu by remember { mutableStateOf(false) }

    // === Réglages visuels (branché depuis le bouton Paramètres) ===
    var showVisualSettings by remember { mutableStateOf(false) }
    var showGlassPreview by remember { mutableStateOf(false) }
    var showGlassTintScreen by remember { mutableStateOf(false) }
    var showLoupeLevelScreen by remember { mutableStateOf(false) }
    var showGlossinessScreen by remember { mutableStateOf(false) }
    var showIconStyleScreen by remember { mutableStateOf(false) }

    // N'inclut PAS widgetPickerDestination : le picker gère ses propres touches,
    // il ne doit pas être étouffé par le Box de réglages qui consomme tout.
    val anyFullScreenOverlay = showVisualSettings || showGlassPreview || showGlassTintScreen ||
            showLoupeLevelScreen || showGlossinessScreen || showIconStyleScreen

    // === Glass settings avec persistance (SharedPreferences) ===
    val glassPrefs = remember {
        context.getSharedPreferences("glass_settings", android.content.Context.MODE_PRIVATE)
    }

    var currentGlassLevel by remember {
        val saved = glassPrefs.getString("glass_level", GlassLevel.Regular.name)
        mutableStateOf(
            runCatching { GlassLevel.valueOf(saved!!) }.getOrDefault(GlassLevel.Regular)
        )
    }
    var currentGlassTint by remember {
        val saved = glassPrefs.getString("glass_tint", GlassTint.None.name)
        mutableStateOf(
            runCatching { GlassTint.valueOf(saved!!) }.getOrDefault(GlassTint.None)
        )
    }
    var customGlassTintColor by remember {
        val argb = glassPrefs.getInt("glass_custom_color", -1)
        mutableStateOf(if (argb != -1) Color(argb) else null)
    }
    var currentLoupeLevel by remember {
        val saved = glassPrefs.getString("loupe_level", LoupeLevel.None.name)
        mutableStateOf(runCatching { LoupeLevel.valueOf(saved!!) }.getOrDefault(LoupeLevel.None))
    }
    var currentGlossLevel by remember {
        val saved = glassPrefs.getString("gloss_level", GlossLevel.None.name)
        mutableStateOf(runCatching { GlossLevel.valueOf(saved!!) }.getOrDefault(GlossLevel.None))
    }

    // === Style des icônes (Default / Dark / Tinted / Glass) avec persistance ===
    var iconStyleMode by remember {
        val saved = glassPrefs.getString("icon_style_mode", com.stanleycx.fruitos.ui.components.IconStyleMode.Default.name)
        mutableStateOf(
            runCatching { com.stanleycx.fruitos.ui.components.IconStyleMode.valueOf(saved!!) }
                .getOrDefault(com.stanleycx.fruitos.ui.components.IconStyleMode.Default)
        )
    }
    var iconTintColor by remember {
        val argb = glassPrefs.getInt("icon_tint_color", Color(0xFF5B9BD5).toArgb())
        mutableStateOf(Color(argb))
    }
    var iconGlassGlyph by remember {
        val saved = glassPrefs.getString("icon_glass_glyph", com.stanleycx.fruitos.ui.components.GlassGlyphStyle.Mono.name)
        mutableStateOf(
            runCatching { com.stanleycx.fruitos.ui.components.GlassGlyphStyle.valueOf(saved!!) }
                .getOrDefault(com.stanleycx.fruitos.ui.components.GlassGlyphStyle.Mono)
        )
    }
    var iconGlassOpacity by remember { mutableStateOf(glassPrefs.getFloat("icon_glass_opacity", 0.75f)) }
    var iconGlassBrightness by remember { mutableStateOf(glassPrefs.getFloat("icon_glass_brightness", 1f)) }
    var iconGlassGlyphTint by remember { mutableStateOf(Color(glassPrefs.getInt("icon_glass_glyph_tint", Color(0xFF5B9BD5).toArgb()))) }
    var iconGlassTintSource by remember {
        val saved = glassPrefs.getString("icon_glass_tint_source", com.stanleycx.fruitos.ui.components.GlassTintSource.System.name)
        mutableStateOf(
            runCatching { com.stanleycx.fruitos.ui.components.GlassTintSource.valueOf(saved!!) }
                .getOrDefault(com.stanleycx.fruitos.ui.components.GlassTintSource.System)
        )
    }
    var iconGlassCustomTint by remember { mutableStateOf(Color(glassPrefs.getInt("icon_glass_custom_tint", Color(0xFF5B9BD5).toArgb()))) }
    var iconLightBorder by remember { mutableStateOf(glassPrefs.getBoolean("icon_light_border", true)) }
    val iconStyle = remember(iconStyleMode, iconTintColor, iconGlassGlyph, iconGlassOpacity, iconGlassBrightness, iconGlassGlyphTint, iconGlassTintSource, iconGlassCustomTint, iconLightBorder) {
        com.stanleycx.fruitos.ui.components.IconStyle(
            mode = iconStyleMode,
            tintColor = iconTintColor,
            glassGlyph = iconGlassGlyph,
            glassGlyphOpacity = iconGlassOpacity,
            glassGlyphBrightness = iconGlassBrightness,
            glassGlyphTintColor = iconGlassGlyphTint,
            glassTintSource = iconGlassTintSource,
            glassCustomTint = iconGlassCustomTint,
            lightBorder = iconLightBorder
        )
    }

    // Sauvegarde automatique à chaque changement
    LaunchedEffect(currentGlassLevel, currentGlassTint, customGlassTintColor, currentLoupeLevel, currentGlossLevel, iconStyleMode, iconTintColor, iconGlassGlyph, iconGlassOpacity, iconGlassBrightness, iconGlassGlyphTint, iconGlassTintSource, iconGlassCustomTint, iconLightBorder) {
        glassPrefs.edit()
            .putString("glass_level", currentGlassLevel.name)
            .putString("glass_tint", currentGlassTint.name)
            .putInt("glass_custom_color", customGlassTintColor?.toArgb() ?: -1)
            .putString("loupe_level", currentLoupeLevel.name)
            .putString("gloss_level", currentGlossLevel.name)
            .putString("icon_style_mode", iconStyleMode.name)
            .putInt("icon_tint_color", iconTintColor.toArgb())
            .putString("icon_glass_glyph", iconGlassGlyph.name)
            .putFloat("icon_glass_opacity", iconGlassOpacity)
            .putFloat("icon_glass_brightness", iconGlassBrightness)
            .putInt("icon_glass_glyph_tint", iconGlassGlyphTint.toArgb())
            .putString("icon_glass_tint_source", iconGlassTintSource.name)
            .putInt("icon_glass_custom_tint", iconGlassCustomTint.toArgb())
            .putBoolean("icon_light_border", iconLightBorder)
            .apply()
    }

    // === Personnalisation d'icône PAR APP (long-press) ===
    var iconOverrides by remember { mutableStateOf(com.stanleycx.fruitos.data.IconOverrideStore.load(context)) }
    var customizingApp by remember { mutableStateOf<com.stanleycx.fruitos.data.AppInfo?>(null) }
    val setOverride: (String, com.stanleycx.fruitos.ui.components.IconOverride) -> Unit = { pkg, ov ->
        val next = iconOverrides.toMutableMap()
        if (ov.isEmpty) next.remove(pkg) else next[pkg] = ov
        iconOverrides = next
        com.stanleycx.fruitos.data.IconOverrideStore.save(context, next)
        // Invalide le cache raster : le filtre de couleur (logoColor) est dynamique,
        // mais c'est propre de rafraîchir la base au cas où (et ça aide si on ajoute
        // plus tard des modifs sur le bitmap lui-même).
        com.stanleycx.fruitos.ui.components.IconCache.invalidate(pkg)
    }

    var darkModeEnabled by remember { mutableStateOf(false) }

    // ID du dossier en cours de renommage via le petit dialogue (depuis le menu contextuel)
    var renamingFolderId by remember { mutableStateOf<String?>(null) }
    // ID du widget en cours de renommage (dialogue avec option « Masquer le nom »).
    var renamingWidgetId by remember { mutableStateOf<String?>(null) }

// Timer d'inactivité — le bouton Recherche apparaît après 2s d'inactivité,
// même s'il n'y a qu'une seule page d'apps (le point indicateur n'est plus prioritaire).
    LaunchedEffect(pagerState.isScrollInProgress, pagerState.currentPage) {
        if (pagerState.isScrollInProgress) {
            showSearchButton = false
        } else {
            kotlinx.coroutines.delay(2000)
            if (!pagerState.isScrollInProgress) {
                showSearchButton = true
            }
        }
    }

    // Densité (utilisée par handleDragEnd ET les LaunchedEffect de changement de page)
    val density = androidx.compose.ui.platform.LocalDensity.current

    // Factorisation de la logique de drop, utilisée par la grille ET le dock
    val handleDragEnd: () -> Unit = {
        val draggedApp = dragState.draggedApp
        val draggedFolder = dragState.draggedFolder
        val freshState = currentStateRef.value

        if (dragState.isChangingPage) {
            dragState.end()
        } else {
            when {
                // === CAS 1 : On draggue un DOSSIER ===
                draggedFolder != null -> {
                    when {
                        // Les dossiers ne vont JAMAIS dans le dock (comportement Fruit OS)
                        dragState.isOverDock() -> {
                            // On ne fait rien → le dossier revient à sa place
                            com.stanleycx.fruitos.ui.components.Haptics.light(context)
                        }
                        dragState.hoverPageIndex >= 0 && dragState.hoverSlotIndex >= 0 -> {
                            updateState(
                                moveFolderToPageSlot(
                                    state = freshState,
                                    folder = draggedFolder,
                                    targetPage = dragState.hoverPageIndex,
                                    targetSlot = dragState.hoverSlotIndex
                                )
                            )
                            com.stanleycx.fruitos.ui.components.Haptics.light(context)
                        }
                        dragState.pendingNewPageIndex >= 0 -> {
                            updateState(
                                moveFolderToPageSlot(
                                    state = freshState,
                                    folder = draggedFolder,
                                    targetPage = dragState.pendingNewPageIndex,
                                    targetSlot = 0
                                )
                            )
                            com.stanleycx.fruitos.ui.components.Haptics.light(context)
                        }
                    }
                }

                // === CAS 2 : On draggue une APP (logique existante) ===
                draggedApp != null -> {
                    when {
                        // 🆕 Fusion en dossier (pause sur une autre icône)
                        dragState.hasMergeTarget -> {
                            updateState(
                                mergeIntoFolder(
                                    state = freshState,
                                    dragged = draggedApp,
                                    targetPage = dragState.mergeTargetPage,
                                    targetSlot = dragState.mergeTargetSlot
                                )
                            )
                            com.stanleycx.fruitos.ui.components.Haptics.heavy(context)
                        }
                        dragState.isOverDock() -> {
                            val dockSlot = dragState.computeDockSlot()
                            updateState(moveAppToDock(freshState, draggedApp, dockSlot))
                            com.stanleycx.fruitos.ui.components.Haptics.light(context)
                        }
                        dragState.hoverPageIndex >= 0 && dragState.hoverSlotIndex >= 0 -> {
                            updateState(
                                moveAppToPageSlot(
                                    state = freshState,
                                    app = draggedApp,
                                    targetPage = dragState.hoverPageIndex,
                                    targetSlot = dragState.hoverSlotIndex
                                )
                            )
                            com.stanleycx.fruitos.ui.components.Haptics.light(context)
                        }
                        dragState.pendingNewPageIndex >= 0 -> {
                            updateState(
                                moveAppToPageSlot(
                                    state = freshState,
                                    app = draggedApp,
                                    targetPage = dragState.pendingNewPageIndex,
                                    targetSlot = 0
                                )
                            )
                            com.stanleycx.fruitos.ui.components.Haptics.light(context)
                        }
                    }
                }
            }
            dragState.end()
        }
    }

    // Réf "fraîche" du handler de drop (le pointerInput global est créé une seule fois)
    val handleDragEndRef = remember { mutableStateOf(handleDragEnd) }
    handleDragEndRef.value = handleDragEnd

    // ── Détection d'installation / désinstallation d'apps en temps réel ─────────
    // Chaque changement de package incrémente ce compteur → déclenche le LaunchedEffect.
    var packageChangeVersion by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: Intent?) {
                val pkg = intent?.data?.schemeSpecificPart ?: return
                val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)

                if (intent.action == Intent.ACTION_PACKAGE_REPLACED) {
                    // L'icône / le label peuvent avoir changé : on invalide le cache
                    com.stanleycx.fruitos.ui.components.IconCache.invalidate(pkg)
                }
                // On ne réagit pas aux mises à jour silencieuses (PACKAGE_ADDED avec REPLACING=true
                // est doublé par PACKAGE_REPLACED, évite un double rebuild)
                if (intent.action == Intent.ACTION_PACKAGE_ADDED && replacing) return

                packageChangeVersion++
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    // Traitement des changements de packages : rebuild complet depuis le layout courant
    // buildLauncherState place automatiquement les nouvelles apps dans les slots libres
    // et retire les apps désinstallées (toHomeItem retourne null pour les packages manquants).
    LaunchedEffect(packageChangeVersion) {
        if (packageChangeVersion == 0) return@LaunchedEffect  // ignore la valeur initiale
        val freshApps = withContext(kotlinx.coroutines.Dispatchers.Default) {
            appRepository.loadInstalledApps()
        }
        allInstalledApps = freshApps
        val currentLayout = currentStateRef.value.toLayout()
        val newState = buildLauncherState(freshApps, currentLayout)
        updateStateRef.value(newState)
    }
    // ─────────────────────────────────────────────────────────────────────────────

    // Changement de page automatique quand on drague une app près d'un bord

    // Détection de fusion : si le doigt reste posé sur une AUTRE icône assez longtemps,
    // on arme la formation d'un dossier (façon Fruit OS).
    // REFACTORED: plus de polling 20fps. On réagit aux changements de rawHover* (mis à jour par
    // le global pointer + per-page derived) et on arme un one-shot delay job pour le 450ms.
    LaunchedEffect(dragState.isDragging, dragState.rawHoverPageIndex, dragState.rawHoverSlotIndex, dragState.isDraggingFolder) {
        mergeDwellJob.job?.cancel()
        mergeDwellJob.job = null

        if (!dragState.isDragging || dragState.isDraggingFolder) {
            if (dragState.hasMergeTarget) {
                dragState.mergeTargetPage = -1
                dragState.mergeTargetSlot = -1
            }
            return@LaunchedEffect
        }

        val p = dragState.rawHoverPageIndex
        val s = dragState.rawHoverSlotIndex
        if (p < 0 || s < 0 || dragState.hoveringDock) {
            if (dragState.hasMergeTarget) {
                dragState.mergeTargetPage = -1
                dragState.mergeTargetSlot = -1
            }
            return@LaunchedEffect
        }

        val dragged = dragState.draggedApp
        val occupant = currentStateRef.value.pages.getOrNull(p)?.get(s)
        val isOther = occupant != null && dragged != null &&
                !(occupant is HomeItem.App && occupant.app.packageName == dragged.packageName)

        if (isOther) {
            // Arm 450ms one-shot (canceled if finger moves to different slot)
            mergeDwellJob.job = scope.launch {
                kotlinx.coroutines.delay(450)
                if (dragState.isDragging &&
                    dragState.rawHoverPageIndex == p &&
                    dragState.rawHoverSlotIndex == s &&
                    !dragState.hasMergeTarget
                ) {
                    dragState.mergeTargetPage = p
                    dragState.mergeTargetSlot = s
                    com.stanleycx.fruitos.ui.components.Haptics.medium(context)
                }
            }
        } else {
            if (dragState.hasMergeTarget) {
                dragState.mergeTargetPage = -1
                dragState.mergeTargetSlot = -1
            }
        }
    }

    // ── UN SEUL mécanisme gère tout : changement de page ET création de page.
    //    (l'ancien bloc séparé de création se déclenchait trop tôt → app qui repop)
    LaunchedEffect(dragState.isDragging) {
        if (!dragState.isDragging) return@LaunchedEffect

        val edgeZonePx = with(density) { 50.dp.toPx() }
        var canChangePage = true

        while (dragState.isDragging) {
            val currentPage = pagerState.currentPage
            // pageGridBounds est keyé par l'index de page d'apps (0-based), pas l'index pager.
            // On lit les bounds de la page COURANTE (toujours composée → largeur fiable ;
            // toutes les pages d'apps ont de toute façon la même largeur). Lire avec l'index
            // pager renvoyait souvent null → fallback 400.dp > largeur réelle → la zone de
            // bord DROIT tombait hors écran → impossible de passer à la page suivante.
            val screenWidthPx = dragState.pageGridBounds[currentPage - appPagesStart]?.right
                ?: with(density) { 400.dp.toPx() }

            val fingerX = dragState.position.x
            val atRightEdge = fingerX > screenWidthPx - edgeZonePx
            val atLeftEdge = fingerX < edgeZonePx

            if (!atRightEdge && !atLeftEdge) {
                canChangePage = true
            }

            if (canChangePage) {
                // La "dernière page" pertinente = dernière page d'apps en pager index.
                // Pages d'apps : appPagesStart .. appPagesStart + appPageCount - 1
                val lastAppPagePager = appPagesStart + appPageCount - 1
                when {
                    // Bord droit + dernière page d'apps → créer une nouvelle page
                    atRightEdge && pagerState.currentPage == lastAppPagePager -> {
                        kotlinx.coroutines.delay(650)
                        val stillAtEdge = dragState.position.x > screenWidthPx - edgeZonePx
                        if (dragState.isDragging && stillAtEdge && canChangePage &&
                            dragState.pendingNewPageIndex < 0
                        ) {
                            val freshState = currentStateRef.value
                            val newPages = freshState.pages + listOf(emptyMap<Int, HomeItem>())
                            val newAppIndex = newPages.lastIndex   // app page index (0-based)
                            val newPagerIndex = newAppIndex + appPagesStart
                            val newState = freshState.copy(pages = newPages)

                            dragState.pendingNewPageIndex = newAppIndex
                            state = newState
                            currentStateRef.value = newState

                            dragState.isChangingPage = true
                            try {
                                kotlinx.coroutines.delay(120)
                                pagerState.animateScrollToPage(newPagerIndex)
                            } finally {
                                dragState.isChangingPage = false
                            }
                            canChangePage = false
                        }
                    }

                    // Bord droit sur une page intermédiaire d'apps → page suivante
                    atRightEdge && pagerState.currentPage in appPagesStart until lastAppPagePager -> {
                        val nextPage = currentPage + 1
                        kotlinx.coroutines.delay(650)
                        val stillAtEdge = dragState.position.x > screenWidthPx - edgeZonePx
                        if (dragState.isDragging && stillAtEdge && canChangePage) {
                            dragState.isChangingPage = true
                            pagerState.animateScrollToPage(nextPage)
                            kotlinx.coroutines.delay(120)
                            dragState.isChangingPage = false
                            canChangePage = false
                        }
                    }

                    // Bord gauche → page précédente (jamais la page widget pendant un drag)
                    atLeftEdge -> {
                        val prevPage = currentPage - 1
                        if (prevPage >= appPagesStart) {   // ne pas entrer dans la page widget (index 0)
                            kotlinx.coroutines.delay(650)
                            val stillAtEdge = dragState.position.x < edgeZonePx
                            if (dragState.isDragging && stillAtEdge && canChangePage) {
                                dragState.isChangingPage = true
                                pagerState.animateScrollToPage(prevPage)
                                kotlinx.coroutines.delay(120)
                                dragState.isChangingPage = false
                                canChangePage = false
                            }
                        }
                    }
                }
            }

            kotlinx.coroutines.delay(80)
        }
    }

    // État du tracking de swipe vertical (intercepté en mode initial)
    var accumulatedDownY by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var accumulatedUpY by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var accumulatedAbsX by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    // Position de DÉPART du swipe → distingue "haut-droite" (Control Center) du reste (Spotlight).
    var swipeStartX by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var swipeStartY by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    // Style d'icônes global fourni à tous les AppIcon de l'arbre (accueil, dock, dossiers,
    // App Library, Spotlight) + HazeState pour le mode Verre des icônes + le matériau verre
    // système (le mode Verre des icônes reprend les mêmes réglages que dossiers/dock).
    val glassMaterial = remember(currentGlassLevel, currentGlassTint, customGlassTintColor, currentLoupeLevel, currentGlossLevel) {
        com.stanleycx.fruitos.ui.components.GlassMaterial(
            level = currentGlassLevel,
            tint = currentGlassTint,
            customTintColor = customGlassTintColor,
            loupe = currentLoupeLevel,
            gloss = currentGlossLevel
        )
    }
    CompositionLocalProvider(
        com.stanleycx.fruitos.ui.components.LocalIconStyle provides iconStyle,
        com.stanleycx.fruitos.ui.components.LocalIconHazeState provides hazeState,
        com.stanleycx.fruitos.ui.components.LocalGlassMaterial provides glassMaterial,
        com.stanleycx.fruitos.ui.components.LocalIconOverrides provides iconOverrides
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Pendant un drag, exclut tout l'écran des gestes système (back, etc.)
            .then(
                if (dragState.isDragging) Modifier.systemGestureExclusion()
                else Modifier
            )
            .pointerInput(editMode.isEditing) {
                if (editMode.isEditing) {
                    detectTapGestures(onTap = { editMode.exit() })
                }
            }
            // Détection swipe-down EN MODE INITIAL (avant les enfants)
            // Désactivée sur la page App Library : elle a sa propre recherche et son
            // contenu scrolle verticalement (sinon le scroll ouvrirait le Spotlight).
            .pointerInput(dragState.isDragging, editMode.isEditing, spotlightState.isOpen, controlCenterState.isOpen, onLibraryPage, onWidgetPage, openFolder, anyFullScreenOverlay, widgetPickerDestination) {
                if (dragState.isDragging || editMode.isEditing || spotlightState.isOpen || controlCenterState.isOpen || onLibraryPage || onWidgetPage || openFolder != null || anyFullScreenOverlay || widgetPickerDestination != null) {
                    return@pointerInput
                }
                val topZoneLimitPx = 170.dp.toPx()
                // Limite BASSE de la zone de départ du swipe-haut ouvrant le Control Center :
                // la ligne médiane du dock. Au-dessus (jusqu'en haut de l'écran) = OK ; en dessous
                // (moitié basse du dock + bord écran) = réservé au geste de nav système.
                // Dock : Column ancrée en bas, padding bas 12.dp, hauteur dock 96.dp.
                val dockBottomMarginPx = 12.dp.toPx()
                val dockHeightPx = 96.dp.toPx()
                val dockMidPx = size.height - dockBottomMarginPx - dockHeightPx / 2f
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: continue

                        if (change.pressed && !change.previousPressed) {
                            accumulatedDownY = 0f
                            accumulatedUpY = 0f
                            accumulatedAbsX = 0f
                            swipeStartX = change.position.x
                            swipeStartY = change.position.y
                        } else if (change.pressed && change.previousPressed) {
                            val deltaY = change.position.y - change.previousPosition.y
                            val deltaX = change.position.x - change.previousPosition.x
                            if (deltaY > 0) {
                                accumulatedDownY += deltaY
                                if (deltaY > 10f) accumulatedUpY = 0f
                            } else if (deltaY < 0) {
                                accumulatedUpY += -deltaY
                                if (deltaY < -10f) accumulatedDownY = 0f
                            }
                            accumulatedAbsX += kotlin.math.abs(deltaX)

                            // Swipe BAS (mostly vertical)
                            if (accumulatedDownY > 120f && accumulatedDownY > accumulatedAbsX * 2f) {
                                // Haut-GAUCHE → notifications Android ; ailleurs → Spotlight
                                if (swipeStartY < topZoneLimitPx && swipeStartX < size.width * 0.45f) {
                                    com.stanleycx.fruitos.ui.controlcenter.expandNotificationsPanel(context)
                                } else {
                                    spotlightState.open()
                                }
                                change.consume()
                                accumulatedDownY = 0f; accumulatedUpY = 0f; accumulatedAbsX = 0f
                            }
                            // Swipe HAUT démarré AU-DESSUS de la ligne médiane du dock → Control Center.
                            // Le départ peut être n'importe où entre cette médiane et le haut de l'écran ;
                            // seules la moitié basse du dock + le bord écran sont réservées au geste de
                            // nav système.
                            else if (accumulatedUpY > 120f && accumulatedUpY > accumulatedAbsX * 2f &&
                                swipeStartY <= dockMidPx
                            ) {
                                controlCenterState.open()
                                change.consume()
                                accumulatedDownY = 0f; accumulatedUpY = 0f; accumulatedAbsX = 0f
                            }
                        } else if (!change.pressed) {
                            accumulatedDownY = 0f
                            accumulatedUpY = 0f
                            accumulatedAbsX = 0f
                        }
                    }
                }
            }
            // SUIVI GLOBAL DU DRAG (survit aux changements de page)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)

                        if (dragState.awaitingGlobalTracking) {
                            dragState.awaitingGlobalTracking = false

                            var dragging = true
                            while (dragging) {
                                val event = awaitPointerEvent(pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                val change = event.changes.firstOrNull()

                                if (change == null) {
                                    dragging = false
                                } else if (change.pressed) {
                                    dragState.update(change.position)
                                    dragState.updateDockHover()
                                    change.consume()
                                } else {
                                    dragging = false
                                    handleDragEndRef.value()
                                }
                            }
                        }
                    }
                }
            }

    ) {
        // hazeSource LIMITÉ au wallpaper uniquement.
        // Règle Haze : un hazeEffect doit être SIBLING de son hazeSource, jamais
        // un descendant. Sinon il ne capture rien (= icône transparente avec
        // juste le border, comportement observé sur les dossiers en état normal).
        // En limitant le source au wallpaper, les FolderIcon de la grille
        // deviennent des siblings et leur hazeEffect peut enfin échantillonner.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
        ) {
            WallpaperBackground()
        }

        // Pages d'apps (maintenant en sibling du hazeSource → hazeEffect fonctionne
        // pour les FolderIcon en état normal, exactement comme pour le Dock).
        // NB : le padding status bar n'est PAS appliqué ici...
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                // Pas de changement de page au swipe UNIQUEMENT en édition SUR LA PAGE WIDGETS
                // (évite tout conflit avec déplacement/resize). Les pages d'accueil restent
                // swipables normalement, même en mode édition.
                userScrollEnabled = !dragState.isDragging && !(editMode.isEditing && onWidgetPage)
            ) { pageIndex ->
                when {
                    // ── Page widget dédiée (index 0) : VIDE, le vrai contenu est en OVERLAY ────
                    pageIndex == WIDGET_PAGE_INDEX -> {
                        Box(modifier = Modifier.fillMaxSize())
                    }

                    // ── Page App Library : VIDE, le vrai contenu est en OVERLAY ────────
                    pageIndex == libraryPageIndex -> {
                        Box(modifier = Modifier.fillMaxSize())
                    }

                    // ── Pages d'apps ──────────────────────────────────────────────────
                    else -> {
                        // Convertit l'index pager en index de page d'app (0-based)
                        val appPageIndex = pageIndex - appPagesStart
                        val pageSlots = currentState.pages.getOrNull(appPageIndex) ?: emptyMap()

                        // Slots couverts par les widgets posés sur cette page (pager index)
                        val pageWidgetBlockedSlots = remember(widgetLayout, pageIndex) {
                            widgetLayout.placements
                                .filter { it.pageIndex == pageIndex }
                                .flatMap { it.blockedSlots() }
                                .toSet()
                        }

                        // Dimensions approx des cellules (pour le preview de resize hors BoxWithConstraints)
                        val statusBarTopDp = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
                        val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
                        val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
                        val approxCellWidthPx = with(density) { ((screenWidthDp - 24.dp) / 4).toPx() }
                        val approxCellHeightPx = with(density) { ((screenHeightDp - statusBarTopDp - 184.dp - 16.dp) / 6).toPx() }

                        // Déclarés avant displaySlots pour que le derivedStateOf puisse les lire
                        val pageWidgets = remember(widgetLayout, pageIndex) {
                            widgetLayout.placements.filter { it.pageIndex == pageIndex }
                        }
                        var widgetDragId by remember { mutableStateOf<String?>(null) }
                        var widgetDragOffset by remember { mutableStateOf(Offset.Zero) }

                        // Clés remember : uniquement les données de layout (pageSlots, widgets).
                        // Les états drag (hoverPageIndex, mergeTarget, etc.) sont lus DANS
                        // le bloc derivedStateOf → snapshot les track automatiquement.
                        // Mettre du drag state en clé recrée un nouveau derivedStateOf à chaque
                        // hover change (plusieurs fois par frame) sans aucun bénéfice.
                        val displaySlots by remember(
                            pageSlots,
                            pageWidgetBlockedSlots
                        ) {
                            derivedStateOf {
                                val baseSlots: Map<Int, HomeItem> = when {
                                    // Pendant un drag de widget : simule le repack à la position snap
                                    // pour que les apps glissent vers leurs positions futures en temps réel.
                                    widgetDragId != null -> {
                                        val dragW = pageWidgets.find { it.widgetId == widgetDragId }
                                        if (dragW != null) {
                                            val snapCol = (dragW.col + widgetDragOffset.x / approxCellWidthPx)
                                                .roundToInt().coerceIn(0, 4 - dragW.colSpan).toFloat()
                                            val snapRow = (dragW.row + widgetDragOffset.y / approxCellHeightPx)
                                                .roundToInt().coerceIn(0, 6 - dragW.rowSpan).toFloat()
                                            val movedWidget = dragW.copy(col = snapCol, row = snapRow)
                                            // Tous les bloqués : autres widgets + widget à la position snap
                                            val allBlockedForPreview = widgetLayout.placements
                                                .filter { it.pageIndex == pageIndex }
                                                .map { if (it.widgetId == dragW.widgetId) movedWidget else it }
                                                .flatMap { it.blockedSlots() }
                                                .toSet()
                                            // Bloqués AVANT le déplacement (widget à sa position d'origine).
                                            val oldBlockedForPreview = widgetLayout.placements
                                                .filter { it.pageIndex == pageIndex }
                                                .flatMap { it.blockedSlots() }
                                                .toSet()
                                            // Seules les apps des cellules NOUVELLEMENT recouvertes bougent ;
                                            // les apps à l'autre bout (non recouvertes) restent en place.
                                            displaceNewlyBlockedOnly(currentState, appPageIndex, allBlockedForPreview, oldBlockedForPreview)
                                                .pages.getOrNull(appPageIndex) ?: emptyMap()
                                        } else pageSlots.filterKeys { it !in pageWidgetBlockedSlots }
                                    }
                                    // Pendant un resize : simule le repack à la taille preview
                                    // pour que les apps glissent vers leurs positions futures en temps réel.
                                    else -> {
                                        val resizingId = widgetResizeState.resizingWidgetId
                                        val orig = widgetResizeState.originalRect
                                        val resizingWidget = if (resizingId != null) {
                                            widgetLayout.placements.firstOrNull {
                                                it.widgetId == resizingId && it.pageIndex == pageIndex
                                            }
                                        } else null
                                        if (resizingId != null && orig != null && resizingWidget != null) {
                                            val newCs = (orig.colSpan + (widgetResizeState.rawOffsetX / approxCellWidthPx).roundToInt()).coerceIn(1, 4)
                                            val newRs = (orig.rowSpan + (widgetResizeState.rawOffsetY / approxCellHeightPx).roundToInt()).coerceIn(1, 6)
                                            val oldBlockedPreview = resizingWidget.blockedSlots()
                                            val newBlockedPreview = resizingWidget.copy(colSpan = newCs, rowSpan = newRs).blockedSlots()
                                            displaceNewlyBlockedOnly(currentState, appPageIndex, newBlockedPreview, oldBlockedPreview)
                                                .pages.getOrNull(appPageIndex) ?: emptyMap()
                                        } else {
                                            if (pageWidgetBlockedSlots.isEmpty()) pageSlots
                                            else pageSlots.filterKeys { it !in pageWidgetBlockedSlots }
                                        }
                                    }
                                }
                                computeDragPreview(baseSlots, appPageIndex, dragState)
                            }
                        }

                        val gridDensity = density
                        val horizontalPaddingPx = with(gridDensity) { 12.dp.toPx() }
                        val rowHeightPx = with(gridDensity) { 92.dp.toPx() }
                        val verticalSpacingPx = with(gridDensity) { 4.dp.toPx() }

                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                // Status bar système visible → on réserve son inset.
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .pointerInput(appPageIndex, displaySlots, editMode.isEditing) {
                                    if (editMode.isEditing) {
                                        detectTapGestures(onTap = { editMode.exit() })
                                    } else {
                                        detectTapGestures(
                                            onLongPress = { offset ->
                                                if (contextMenuItem != null) return@detectTapGestures
                                                val gridStartXPx = horizontalPaddingPx
                                                val gridWidthPx = size.width - (horizontalPaddingPx * 2)
                                                val cellWidthPx = gridWidthPx / 4f
                                                val cellHeightPx = rowHeightPx + verticalSpacingPx
                                                val relativeX = offset.x - gridStartXPx
                                                val relativeY = offset.y
                                                if (relativeX < 0f || relativeY < 0f || relativeX > gridWidthPx) return@detectTapGestures
                                                val col = (relativeX / cellWidthPx).toInt().coerceIn(0, 3)
                                                val row = (relativeY / cellHeightPx).toInt().coerceIn(0, 5)
                                                val slot = row * 4 + col
                                                // Slot calculé avec les MÊMES métriques que le placement des
                                                // widgets (cellHeight = (h-16dp)/6) — le calcul "icône" ci-dessus
                                                // utilise 96dp/ligne et ratait le bas des widgets → le long-press
                                                // sur un widget ouvrait aussi le menu rapide.
                                                val wCellW = ((size.width - 24.dp.toPx()) / 4f).coerceAtLeast(1f)
                                                val wCellH = ((size.height - 16.dp.toPx()) / 6f).coerceAtLeast(1f)
                                                val wSlot = (offset.y / wCellH).toInt().coerceIn(0, 5) * 4 +
                                                        (offset.x / wCellW).toInt().coerceIn(0, 3)
                                                // Cellule vide ET non recouverte par un widget (les 2 grilles).
                                                if (displaySlots[slot] == null &&
                                                    slot !in pageWidgetBlockedSlots &&
                                                    wSlot !in pageWidgetBlockedSlots
                                                ) {
                                                    showHomeQuickMenu = true
                                                }
                                            }
                                        )
                                    }
                                }
                        ) {
                            // Grille d'icônes — zIndex(1) + graphicsLayer : les icônes/dossiers se
                            // dessinent TOUJOURS au-dessus de l'overlay widgets.
                            // ⚠️ Le zIndex SEUL ne suffit pas face à une AppWidgetHostView interop :
                            // une AndroidView interop est dessinée dans sa propre couche de vue
                            // NATIVE, posée AU-DESSUS du contenu Compose par défaut. Tant que la
                            // grille restait dessinée dans le canvas racine (zIndex ne crée pas de
                            // couche « possédée »), les widgets qui forcent une couche matérielle
                            // (horloge AnalogClock/TextClock animée, météo rafraîchie) repassaient
                            // DEVANT les icônes (la photo, ImageView statique, ne le faisait pas).
                            // `graphicsLayer { }` matérialise la grille en couche de vue propre,
                            // ordonnée au-dessus des couches interop → icônes toujours devant.
                            // Les slots vides sont des Box transparents sans pointerInput, donc
                            // les touches au-dessus d'un widget tombent sur le widget (interactif).
                            HomePage(
                                modifier = Modifier
                                    .zIndex(1f)
                                    .graphicsLayer { },
                                slots = displaySlots,
                                realSlots = pageSlots,   // état réel (non décalé) pour la détection de fusion
                                blockedSlots = pageWidgetBlockedSlots,  // cellules widget = drop interdit
                                appRepository = appRepository,
                                editMode = editMode,
                                dragState = dragState,
                                pageIndex = appPageIndex,
                                glassLevel = currentGlassLevel,
                                glassTint = currentGlassTint,
                                customTintColor = customGlassTintColor,
                                loupeLevel = currentLoupeLevel,
                                glossLevel = currentGlossLevel,
                                onLongPress = { editMode.enter() },
                                onRemoveApp = { app ->
                                    updateState(removeAppFromHome(currentState, app.packageName))
                                },
                                onDragStart = { app, position -> dragState.start(HomeItem.App(app), position) },
                                onDragDelta = { delta -> dragState.update(dragState.position + delta) },
                                onDragEnd = handleDragEnd,
                                onOpenFolder = { slot -> openFolderLoc = appPageIndex to slot },
                                onFolderDragStart = { folder, position -> dragState.start(folder, position) },
                                onFolderDrag = { delta -> dragState.update(dragState.position + delta) },
                                onFolderDragEnd = handleDragEnd,
                                hazeState = hazeState,
                                onRequestContextMenu = { item, rect ->
                                    contextMenuItem = item
                                    contextMenuSourceRect = rect
                                },
                                menuTarget = contextMenuItem,
                                notificationCounts = notificationCounts
                            )

                            // ── Overlay widgets sur la page d'app ────────────────────
                            val cellWidthDp = (maxWidth - 24.dp) / 4
                            val cellHeightDp = (maxHeight - 16.dp) / 6
                            // Alignement vertical des widgets sur les tuiles d'icônes voisines :
                            // inset FIXE (constantes) → bord haut widget = haut tuile (68dp,
                            // centrée dans une colonne ≈97dp), bord bas = bas tuile. Pas de mesure
                            // par frame (60 fps).
                            //
                            // ┌── RÉGLAGE FIN (ajuste juste ces 2 valeurs en dp) ──────────────┐
                            // │ widgetTopTweak    : + descend le BORD HAUT, − le remonte        │
                            // │ widgetBottomTweak : + remonte le BORD BAS,  − le descend        │
                            // └────────────────────────────────────────────────────────────────┘
                            val widgetTopTweak = -8.dp
                            val widgetBottomTweak = -10.dp
                            val widgetBaseTop = (cellHeightDp - 97.dp) / 2 + 5.dp
                            val widgetTopInset = (widgetBaseTop + widgetTopTweak).coerceAtLeast(0.dp)
                            val widgetBottomReserve = (cellHeightDp - widgetBaseTop - 68.dp + widgetBottomTweak).coerceAtLeast(0.dp)
                            // pageWidgets, widgetDragId et widgetDragOffset sont déclarés avant BoxWithConstraints
                            LaunchedEffect(editMode.isEditing) {
                                if (!editMode.isEditing) { widgetDragId = null; widgetDragOffset = Offset.Zero }
                            }

                            if (pageWidgets.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp)
                                        .padding(top = 8.dp, bottom = 8.dp)
                                ) {
                                    // Indicateur de snap (position cible en tirets blancs)
                                    widgetDragId?.let { dragId ->
                                        val w = pageWidgets.find { it.widgetId == dragId }
                                        w?.let {
                                            val cWPx = with(density) { cellWidthDp.toPx() }
                                            val cHPx = with(density) { cellHeightDp.toPx() }
                                            val snapCol = (it.col + widgetDragOffset.x / cWPx).roundToInt().toFloat().coerceIn(0f, (4 - it.colSpan).toFloat())
                                            val snapRow = (it.row + widgetDragOffset.y / cHPx).roundToInt().toFloat().coerceIn(0f, (6 - it.rowSpan).toFloat())
                                            // Plus de feedback rouge : le chevauchement est désormais résolu en
                                            // poussant les widgets voisins (et débordement page suivante).
                                            val snapColor = Color.White
                                            Box(
                                                modifier = Modifier
                                                    .offset(x = cellWidthDp * snapCol, y = cellHeightDp * snapRow)
                                                    .size(cellWidthDp * it.colSpan, cellHeightDp * it.rowSpan)
                                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
                                                    .background(snapColor.copy(alpha = 0.12f))
                                                    .border(2.dp, snapColor.copy(alpha = 0.35f), androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
                                            )
                                        }
                                    }

                                    pageWidgets.forEach { widget ->
                                      // key() par widgetId : lie chaque WidgetCard (et son
                                      // AppWidgetHostView) à SON widget. Sans ça, supprimer un
                                      // widget recyclerait les vues par position → un autre widget
                                      // s'afficherait / serait supprimé à la place.
                                      key(widget.widgetId) {
                                        val isBeingDragged = widgetDragId == widget.widgetId
                                        val dxDp = if (isBeingDragged) with(density) { widgetDragOffset.x.toDp() } else 0.dp
                                        val dyDp = if (isBeingDragged) with(density) { widgetDragOffset.y.toDp() } else 0.dp

                                        val baseX = cellWidthDp * widget.col
                                        val baseY = cellHeightDp * widget.row

                                        val providerLabel = remember(widget.appWidgetId) {
                                            runCatching {
                                                awm.getAppWidgetInfo(widget.appWidgetId)?.loadLabel(context.packageManager)
                                            }.getOrNull() ?: "Widget"
                                        }

                                        WidgetCard(
                                            placement = widget,
                                            editMode = editMode,
                                            resizeState = widgetResizeState,
                                            cellWidthDp = cellWidthDp,
                                            cellHeightDp = cellHeightDp,
                                            topInsetDp = widgetTopInset,
                                            bottomReserveDp = widgetBottomReserve,
                                            showLabel = !widget.labelHidden,
                                            label = widget.label ?: providerLabel,
                                            isDragged = isBeingDragged,
                                            onRemove = { removeWidget(widget.widgetId) },
                                            onResizeCommit = { rect ->
                                                val freshPlacement = widgetLayout.placements
                                                    .find { it.widgetId == widget.widgetId }
                                                if (freshPlacement != null) {
                                                    val oldBlocked = freshPlacement.blockedSlots()
                                                    val resizedCandidate = freshPlacement.copy(
                                                        col = rect.col, row = rect.row,
                                                        colSpan = rect.colSpan, rowSpan = rect.rowSpan
                                                    )
                                                    val newBlocked = resizedCandidate.blockedSlots()
                                                    // Anti-superposition : si la nouvelle taille chevauche
                                                    // un autre widget → snap-back (on ignore le resize).
                                                    if (widgetLayout.collidesOnPage(widget.widgetId, resizedCandidate)) {
                                                        com.stanleycx.fruitos.ui.components.Haptics.light(context)
                                                    } else {
                                                        updateWidgetLayout(widgetLayout.copy(
                                                            placements = widgetLayout.placements.map {
                                                                if (it.widgetId == widget.widgetId) it.copy(
                                                                    col = rect.col, row = rect.row,
                                                                    colSpan = rect.colSpan, rowSpan = rect.rowSpan
                                                                ) else it
                                                            }
                                                        ))
                                                        updateState(displaceNewlyBlockedOnly(currentStateRef.value, appPageIndex, newBlocked, oldBlocked))
                                                    }
                                                }
                                            },
                                            onLongPress = { touchPos ->
                                                widgetContextMenuId = widget.widgetId
                                                widgetContextMenuPos = touchPos
                                            },
                                            onWidgetDragStart = {
                                                widgetDragId = widget.widgetId
                                                widgetDragOffset = Offset.Zero
                                            },
                                            onWidgetDrag = { delta ->
                                                if (widgetDragId == widget.widgetId) widgetDragOffset += delta
                                            },
                                            onWidgetDragEnd = {
                                                if (widgetDragId == widget.widgetId) {
                                                    val cWPx = with(density) { cellWidthDp.toPx() }
                                                    val cHPx = with(density) { cellHeightDp.toPx() }
                                                    // Lit la position fraîche depuis widgetLayout pour éviter le stale closure :
                                                    // widget.col/row peut être obsolète si widgetLayout a changé
                                                    // depuis la dernière composition du WidgetCard (drag précédent).
                                                    val freshPlacement = widgetLayout.placements
                                                        .find { it.widgetId == widget.widgetId }
                                                    if (freshPlacement != null) {
                                                        // Centre horizontal du widget (en colonnes) après le drag.
                                                        val rawCol = freshPlacement.col + widgetDragOffset.x / cWPx
                                                        val centerCol = rawCol + freshPlacement.colSpan / 2f
                                                        // Relâché au-delà du bord G/D → déplacement vers la page voisine
                                                        // (page suivante créée au besoin ; page pleine ⇒ débordement auto).
                                                        val pageDir = when {
                                                            centerCol > 4f -> 1
                                                            centerCol < 0f && (freshPlacement.pageIndex - 1) >= appPagesStart -> -1
                                                            else -> 0
                                                        }
                                                      if (pageDir != 0) {
                                                        val targetPager = freshPlacement.pageIndex + pageDir
                                                        var st = currentStateRef.value
                                                        val moved = widgetLayout.moveWidgetToPage(widget.widgetId, targetPager, maxRows = 6)
                                                        val landed = moved.placements.find { it.widgetId == widget.widgetId }
                                                        if (landed != null) {
                                                            val landedAppIdx = landed.pageIndex - appPagesStart
                                                            while (st.pages.size <= landedAppIdx) st = st.copy(pages = st.pages + emptyMap())
                                                            // Reflow des apps de la page d'arrivée (le widget y bloque
                                                            // de nouveaux slots) ; la page de départ libère, rien à pousser.
                                                            st = displaceNewlyBlockedOnly(st, landedAppIdx, landed.blockedSlots(), emptySet())
                                                        }
                                                        updateWidgetLayout(moved)
                                                        updateState(st)
                                                        com.stanleycx.fruitos.ui.components.Haptics.light(context)
                                                        landed?.let { scope.launch { pagerState.animateScrollToPage(it.pageIndex) } }
                                                      } else {
                                                        val newCol = (freshPlacement.col + widgetDragOffset.x / cWPx).roundToInt().toFloat()
                                                            .coerceIn(0f, (4 - freshPlacement.colSpan).toFloat())
                                                        val newRow = (freshPlacement.row + widgetDragOffset.y / cHPx).roundToInt().toFloat()
                                                            .coerceIn(0f, (6 - freshPlacement.rowSpan).toFloat())
                                                        if (newCol != freshPlacement.col || newRow != freshPlacement.row) {
                                                            // PUSH au lieu d'interdire : on pose le widget à la cible
                                                            // en décalant les widgets voisins (et en faisant déborder
                                                            // sur la page suivante ceux qui ne tiennent plus).
                                                            val pushedLayout = widgetLayout.dropWidgetWithPush(
                                                                widget.widgetId, newCol.toInt(), newRow.toInt(), maxRows = 6
                                                            )
                                                            // Garantit que les pages d'apps existent pour les widgets
                                                            // ayant débordé (pageIndex pager → state.pages).
                                                            val maxWidgetPager = pushedLayout.placements
                                                                .filter { it.pageIndex >= appPagesStart }
                                                                .maxOfOrNull { it.pageIndex } ?: appPagesStart
                                                            val neededPages = maxWidgetPager - appPagesStart + 1
                                                            var st = currentStateRef.value
                                                            while (st.pages.size < neededPages) st = st.copy(pages = st.pages + emptyMap())
                                                            // Reflow des icônes autour des slots bloqués, de la page
                                                            // draguée jusqu'à la dernière page affectée (ordre ascendant).
                                                            for (ap in appPageIndex until neededPages) {
                                                                val pagerIdx = ap + appPagesStart
                                                                val newBlocked = pushedLayout.placements
                                                                    .filter { it.pageIndex == pagerIdx }
                                                                    .flatMap { it.blockedSlots() }
                                                                    .toSet()
                                                                // Slots bloqués AVANT le déplacement/push sur cette page.
                                                                val oldBlocked = widgetLayout.placements
                                                                    .filter { it.pageIndex == pagerIdx }
                                                                    .flatMap { it.blockedSlots() }
                                                                    .toSet()
                                                                // Ne déplace que les apps des cellules nouvellement
                                                                // recouvertes (par le widget déplacé OU un widget poussé
                                                                // qui arrive sur cette page) ; les autres restent en place.
                                                                st = displaceNewlyBlockedOnly(st, ap, newBlocked, oldBlocked)
                                                            }
                                                            updateWidgetLayout(pushedLayout)
                                                            updateState(st)
                                                        }
                                                      }
                                                    }
                                                    widgetDragId = null
                                                    widgetDragOffset = Offset.Zero
                                                }
                                            },
                                            modifier = Modifier.offset(
                                                x = baseX + dxDp,
                                                y = baseY + dyDp
                                            )
                                        )
                                      }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Réserve l'espace exact pour la COUCHE 2 (Dock + indicateur) qui est alignée BottomCenter.
            // Doit rester synchronisé avec la hauteur réelle du dock + page indicator.
            Spacer(modifier = Modifier.height(184.dp))
        }

        // (Status bar : c'est désormais la barre SYSTÈME du téléphone — plus de barre custom.)

        // COUCHE 2 : dock + indicateur. Masqué sur App Library ET page widget.
        androidx.compose.animation.AnimatedVisibility(
            visible = !onLibraryPage && !onWidgetPage,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Nav bar masquée : on ne dépend plus de son inset. Marge basse fixe
                    // pour que le dock reste un peu au-dessus du bord (style Fruit OS).
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // === Zone indicateur / bouton Recherche ===
                // Le cadre en verre s'adapte en largeur (dots → bouton Recherche).
                // Seuls les contenus internes (points ou loupe + texte) ont une animation d'apparition.
                // Toujours affiché même avec une seule page.
                if (!showHomeQuickMenu) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .graphicsLayer { clip = false }   // autorise le pop à dépasser
                            // Hitbox confortable : un appui long dans le vide AUTOUR / sous le
                            // champ de recherche ouvre aussi les boutons d'édition. Le bouton
                            // Recherche consomme son propre tap (Spotlight) ; seul le vide retombe ici.
                            .pointerInput(Unit) {
                                detectTapGestures(onLongPress = { offset ->
                                    if (contextMenuItem != null) return@detectTapGestures
                                    // Garde widget : un long-press sur le BAS d'un widget qui
                                    // déborde sur la zone du bouton Recherche ne doit pas ouvrir
                                    // le menu rapide (sinon : 2 boutons + menu contextuel).
                                    val col = (offset.x / (size.width / 4f)).toInt().coerceIn(0, 3)
                                    val cur = pagerState.currentPage
                                    val blocked = widgetLayout.placements
                                        .filter { it.pageIndex == cur }
                                        .flatMap { it.blockedSlots() }
                                    if ((20 + col) !in blocked && (16 + col) !in blocked) {
                                        showHomeQuickMenu = true
                                    }
                                })
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Une seule page d'accueil → JAMAIS de dots, on force le bouton Recherche
                        // (y compris pendant un swipe vers la page widgets/library).
                        val showSearchOnly = showSearchButton || appPageCount <= 1
                        AnimatedContent(
                            targetState = showSearchOnly,
                            modifier = Modifier.graphicsLayer { clip = false },
                            transitionSpec = {
                                // On anime principalement la largeur du cadre (pill) via SizeTransform.
                                // Pas de disparition / gros scale du bouton lui-même.
                                // Seuls le contenu (dots ou loupe + texte) ont une animation d'apparition douce.
                                val sizeAnim = SizeTransform(clip = false) { _, _ ->
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                }

                                // Crossfade léger sur le contenu seulement.
                                // La largeur du cadre en verre s'adapte de façon fluide grâce à sizeAnim.
                                fadeIn(animationSpec = tween(160)) togetherWith
                                        fadeOut(animationSpec = tween(120)) using sizeAnim
                            },
                            label = "dotsToSearchButton"
                        ) { isSearchButtonVisible ->
                            if (isSearchButtonVisible) {
                                SpotlightTriggerButton(
                                    onClick = { spotlightState.open() },
                                    hazeState = hazeState,
                                    glassLevel = currentGlassLevel,
                                    glassTint = currentGlassTint,
                                    customTintColor = customGlassTintColor,
                                    loupeLevel = currentLoupeLevel,
                                    glossLevel = currentGlossLevel
                                )
                            } else {
                                // Dots are now inside their own adaptive glass pill (same style as search button).
                                // The pill width automatically matches the number of pages.
                                // Les dots représentent les pages d'apps uniquement (sans widget ni library)
                                // currentPage doit être converti : pager index → app page index (0-based)
                                val dotCurrentPage = (pagerState.currentPage - appPagesStart).coerceIn(0, appPageCount - 1)
                                // Offset lu en lambda (phase de dessin) → la pilule en verre ne
                                // recompose plus à chaque frame pendant le swipe (cf. PageIndicator).
                                PageIndicatorPill(
                                    pageCount = appPageCount,
                                    currentPage = dotCurrentPage,
                                    pageOffsetFraction = {
                                        if (pagerState.currentPage < appPagesStart ||
                                            pagerState.currentPage >= libraryPageIndex) 0f
                                        else pagerState.currentPageOffsetFraction
                                    },
                                    onPageClick = { appIdx ->
                                        scope.launch {
                                            pagerState.scrollToPage(appIdx + appPagesStart)
                                        }
                                    },
                                    hazeState = hazeState,
                                    glassLevel = currentGlassLevel,
                                    glassTint = currentGlassTint,
                                    customTintColor = customGlassTintColor,
                                    loupeLevel = currentLoupeLevel,
                                    glossLevel = currentGlossLevel
                                )
                            }
                        }
                    }
                }

                Dock(
                    apps = currentState.dock,
                    appRepository = appRepository,
                    editMode = editMode,
                    dragState = dragState,
                    hazeState = hazeState,
                    glassLevel = currentGlassLevel,
                    glassTint = currentGlassTint,
                    customTintColor = customGlassTintColor,
                    loupeLevel = currentLoupeLevel,
                                    glossLevel = currentGlossLevel,
                    onLongPress = { editMode.enter() },
                    onRemoveApp = { app ->
                        updateState(removeAppFromHome(currentState, app.packageName))
                    },
                    onDragStart = { app, position -> dragState.start(HomeItem.App(app), position) },
                    onDragDelta = { delta -> dragState.update(dragState.position + delta) },
                    onDragEnd = handleDragEnd,
                    notificationCounts = notificationCounts
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // === Full-screen dim overlay for context menu (covers status bar + dock + tout l'écran) ===
        // Fade doux (plus long) pour éviter l'apparition brutale
        androidx.compose.animation.AnimatedVisibility(
            visible = contextMenuItem != null || showHomeQuickMenu || (widgetContextMenuId != null && !onWidgetPage),
            enter = androidx.compose.animation.fadeIn(
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 280)
            ),
            exit = androidx.compose.animation.fadeOut(
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 200)
            ),
            modifier = Modifier.zIndex(100f)  // Très haut pour être au-dessus du FolderOverlay et de tout le reste (le menu contextuel doit couvrir le dossier quand on long-press dedans)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.38f))
                    .clickable {
                        // Dismiss global (toutes les variantes de menus)
                        if (editMode.isEditing) {
                            editMode.exit()
                        } else {
                            contextMenuItem = null
                            contextMenuSourceRect = null
                            widgetContextMenuId = null
                            widgetContextMenuPos = null
                            showHomeQuickMenu = false
                        }
                    }
            ) {
                // Icône source agrandie qui "passe devant" le calque noir
                // (apps + dossiers, pour que ni l'un ni l'autre ne soit affecté par l'assombrissement)
                contextMenuItem?.let { item ->
                    contextMenuSourceRect?.let { rect ->
                        val density = LocalDensity.current
                        val highlightScale = 1.13f
                        val highlightIconSize = 68.dp * highlightScale

                        // Positionnement strictement centré sur le centre de l'icône source.
                        // L'icône grossit symétriquement depuis son centre d'origine (aucun shift latéral).
                        // On calcule le top-left du nouveau tile plus grand de sorte que son centre
                        // coïncide exactement avec le centre du rect source capturé.
                        val iconCenterX = rect.left + rect.width / 2f
                        val iconCenterY = rect.top + rect.height / 2f
                        val hlIconSizePx = with(density) { highlightIconSize.toPx() }
                        val hlLeft = iconCenterX - hlIconSizePx / 2f
                        val hlTop = iconCenterY - hlIconSizePx / 2f
                        val hlX = with(density) { hlLeft.toDp() }
                        val hlY = with(density) { hlTop.toDp() }

                        // Largeur autorisée pour le label en mode zoom : suffisamment généreuse
                        // pour "Divertissement" et noms similaires, sans que cela n'affecte
                        // la position horizontale de l'icône (géré dans AppIcon / FolderIcon).
                        val labelMaxWidth = with(density) { (rect.width * 2.6f).toDp() }

                        when (item) {
                            is HomeItem.App -> {
                                AppIcon(
                                    app = item.app,
                                    onClick = {},
                                    onLongClick = {},
                                    onLongClickForMenu = {},
                                    iconSize = highlightIconSize,
                                    isMenuHighlight = true,
                                    showLabel = false,
                                    labelMaxWidth = labelMaxWidth,
                                    badgeCount = notificationCounts[item.app.packageName] ?: 0,
                                    modifier = Modifier.offset(x = hlX, y = hlY)
                                )
                            }
                            is HomeItem.Folder -> {
                                // Toujours récupérer la version la plus fraîche du dossier
                                // depuis l'état courant (renommage via dialogue ou overlay).
                                val freshFolder = currentState.pages.asSequence()
                                    .flatMap { it.values.asSequence() }
                                    .filterIsInstance<HomeItem.Folder>()
                                    .firstOrNull { it.id == item.id } ?: item
                                val folderBadgeCount = freshFolder.apps
                                    .sumOf { app -> notificationCounts[app.packageName] ?: 0 }

                                FolderIcon(
                                    folder = freshFolder,
                                    onOpen = {},
                                    onLongClickForMenu = {},
                                    iconSize = highlightIconSize,
                                    hazeState = hazeState,
                                    glassLevel = currentGlassLevel,
                                    glassTint = currentGlassTint,
                                    customTintColor = customGlassTintColor,
                                    loupeLevel = currentLoupeLevel,
                                    glossLevel = currentGlossLevel,
                                    isMenuHighlight = true,
                                    showLabel = false,
                                    labelMaxWidth = labelMaxWidth,
                                    badgeCount = folderBadgeCount,
                                    modifier = Modifier.offset(x = hlX, y = hlY)
                                )
                            }
                        }

                        // Label manuel positionné indépendamment de l'icône.
                        // Centré horizontalement sur le centre de la tuile highlight.
                        // Sa largeur n'affecte *jamais* la position de l'icône (sibling séparé).
                        val labelName = when (item) {
                            is HomeItem.App -> item.app.label
                            is HomeItem.Folder -> {
                                val fresh = currentState.pages.asSequence()
                                    .flatMap { it.values.asSequence() }
                                    .filterIsInstance<HomeItem.Folder>()
                                    .firstOrNull { it.id == item.id } ?: item
                                fresh.name
                            }
                        }
                        // Calculs en Dp (on passe par .value pour l'arithmétique centre/largeur)
                        val hlIconSizeValue = highlightIconSize.value
                        val labelCenterXDp = hlX.value + (hlIconSizeValue / 2f)
                        val labelBoxWidthValue = labelMaxWidth.value
                        val labelBoxLeftDp = labelCenterXDp - (labelBoxWidthValue / 2f)
                        val labelY = hlY + highlightIconSize + 8.dp

                        Box(
                            modifier = Modifier
                                .offset(x = labelBoxLeftDp.dp, y = labelY)
                                .width(labelBoxWidthValue.dp)
                                .wrapContentHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = labelName,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 4.dp),
                                style = androidx.compose.ui.text.TextStyle(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                                        blurRadius = 4f
                                    )
                                )
                            )
                        }
                    }
                }

                // Le menu blanc positionné intelligemment (au-dessus ou en-dessous de l'icône source)
                contextMenuItem?.let { item ->
                    val sourceRect = contextMenuSourceRect ?: Rect(Offset.Zero, Size(80f, 80f))

                    // Calcul position (on préfère en dessous de l'icône, sinon au-dessus)
                    val density = LocalDensity.current
                    val menuWidthDp = 260.dp
                    val menuHeightDp = 150.dp   // un peu de marge

                    var menuXDp = with(density) { sourceRect.left.toDp() }

                    // Dessous : le label zoomed finit ~22dp sous le bas de l'icône source,
                    // on ajoute 8dp de marge visuelle → 30dp au total.
                    var menuYDp = with(density) { sourceRect.bottom.toDp() } + 30.dp

                    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                    menuXDp = menuXDp.coerceIn(16.dp, screenWidth - menuWidthDp - 16.dp)

                    // Si pas de place en dessous → au-dessus avec 4dp de marge
                    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                    if (menuYDp + menuHeightDp > screenHeight - 60.dp) {
                        menuYDp = with(density) { sourceRect.top.toDp() } - 4.dp - menuHeightDp
                    }
                    menuYDp = menuYDp.coerceIn(40.dp, screenHeight - menuHeightDp - 50.dp)

                    Box(
                        modifier = Modifier
                            .offset(x = menuXDp, y = menuYDp)
                            // Léger pop d'entrée pour le menu lui-même (pas brutal)
                            .graphicsLayer {
                                alpha = 1f
                                scaleX = 1f
                                scaleY = 1f
                            }
                    ) {
                        ContextMenu(
                            actions = buildContextMenuActions(
                                item = item,
                                context = context,
                                onEditHome = {
                                    contextMenuItem = null
                                    contextMenuSourceRect = null
                                    editMode.enter()
                                },
                                onRemove = {
                                    contextMenuItem = null
                                    contextMenuSourceRect = null
                                    when (item) {
                                        is HomeItem.App -> {
                                            val pkg = item.app.packageName
                                            // Si le menu a été ouvert depuis un dossier (folder overlay ouvert et l'app y est présente),
                                            // "Supprimer l'app" doit la retirer du dossier, pas de tout le home.
                                            val currentOpen = openFolder
                                            val isInOpenFolder = currentOpen?.apps?.any { it.packageName == pkg } == true
                                            if (isInOpenFolder && openFolderLoc != null) {
                                                removeFromOpenFolder(item.app)
                                            } else {
                                                updateState(removeAppFromHome(currentState, pkg))
                                            }
                                            com.stanleycx.fruitos.ui.components.Haptics.light(context)
                                        }
                                        is HomeItem.Folder -> {
                                            openFolderLoc = null
                                            updateState(removeFolderFromHome(currentState, item))
                                            com.stanleycx.fruitos.ui.components.Haptics.light(context)
                                        }
                                    }
                                },
                                onRenameFolder = { folder ->
                                    contextMenuItem = null
                                    contextMenuSourceRect = null
                                    renamingFolderId = folder.id
                                },
                                onUninstall = { app ->
                                    contextMenuItem = null
                                    contextMenuSourceRect = null
                                    openFolderLoc = null  // Ferme le dossier avant de lancer la désinstallation système
                                    com.stanleycx.fruitos.ui.components.Haptics.light(context)
                                    val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                },
                                onCustomizeIcon = { app ->
                                    contextMenuItem = null
                                    contextMenuSourceRect = null
                                    openFolderLoc = null  // Ferme le dossier pour que l'écran de personnalisation soit visible immédiatement (plein écran dédié)
                                    customizingApp = app
                                },
                                closeCurrentFolder = { openFolderLoc = null }
                            ),
                            onDismiss = {
                                contextMenuItem = null
                                contextMenuSourceRect = null
                            }
                        )
                    }
                }

                // === Menu contextuel widget ===
                widgetContextMenuId?.let { widgetId ->
                    val placement = widgetLayout.placements.find { it.widgetId == widgetId }
                    val info = placement?.let { awm.getAppWidgetInfo(it.appWidgetId) }
                    val hasConfigure = info?.configure != null

                    val dismiss = {
                        widgetContextMenuId = null
                        widgetContextMenuPos = null
                    }

                    val actions = buildList {
                        add(ContextMenuAction(
                            title = "Éditer l'écran d'accueil",
                            icon = Icons.Default.Edit,
                            onClick = { dismiss(); editMode.enter() }
                        ))
                        add(ContextMenuAction(
                            title = "Renommer",
                            icon = Icons.Default.Edit,
                            onClick = { dismiss(); renamingWidgetId = widgetId }
                        ))
                        if (hasConfigure) {
                            add(ContextMenuAction(
                                title = "Paramètres du widget",
                                icon = Icons.Default.Settings,
                                onClick = {
                                    dismiss()
                                    info?.configure?.let { cfg ->
                                        reconfigureLauncher.launch(ConfigureWidgetInput(placement!!.appWidgetId, cfg))
                                    }
                                }
                            ))
                        }
                        add(ContextMenuAction(
                            title = "Supprimer",
                            icon = Icons.Default.RemoveCircle,
                            isDestructive = true,
                            onClick = {
                                dismiss()
                                removeWidget(widgetId)
                            }
                        ))
                    }

                    val density = LocalDensity.current
                    val menuWidthDp = 260.dp
                    val menuHeightDp = (actions.size * 46 + 16).dp
                    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                    val touchPos = widgetContextMenuPos
                    val touchYDp = if (touchPos != null) with(density) { touchPos.y.toDp() }
                                   else screenHeight / 2f

                    var menuXDp = if (touchPos != null) with(density) { touchPos.x.toDp() }
                                  else screenWidth / 2f
                    menuXDp = menuXDp.coerceIn(16.dp, screenWidth - menuWidthDp - 16.dp)
                    var menuYDp = touchYDp + 12.dp
                    if (menuYDp + menuHeightDp > screenHeight - 16.dp) {
                        menuYDp = touchYDp - menuHeightDp - 12.dp
                    }
                    menuYDp = menuYDp.coerceIn(40.dp, screenHeight - menuHeightDp - 16.dp)

                    Box(modifier = Modifier.offset(x = menuXDp, y = menuYDp)) {
                        ContextMenu(
                            actions = actions,
                            onDismiss = dismiss
                        )
                    }
                }

                // === Actions rapides sur long press zone vide ===
                // Deux boutons au-dessus du Dock (style similaire au bouton Recherche),
                // avec le même assombrissement plein écran que le menu contextuel.
                if (showHomeQuickMenu) {
                    // Positionnés dans la zone des points / bouton recherche (au-dessus du Dock)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 130.dp),  // au-dessus du Dock
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Bouton Paramètres - même style verre Fruity Glass que le bouton Recherche
                            GlassQuickActionButton(
                                icon = Icons.Filled.Settings,
                                text = "Paramètres",
                                hazeState = hazeState,
                                glassLevel = currentGlassLevel,
                                glassTint = currentGlassTint,
                                customTintColor = customGlassTintColor,
                                loupeLevel = currentLoupeLevel,
                                    glossLevel = currentGlossLevel,
                                onClick = {
                                    showHomeQuickMenu = false
                                    showVisualSettings = true
                                },
                                modifier = Modifier.weight(1f)
                            )

                            // Bouton Widgets → ouvre le picker pour ajouter un widget sur cette page
                            GlassQuickActionButton(
                                icon = Icons.Filled.GridView,
                                text = "Widgets",
                                hazeState = hazeState,
                                glassLevel = currentGlassLevel,
                                glassTint = currentGlassTint,
                                customTintColor = customGlassTintColor,
                                loupeLevel = currentLoupeLevel,
                                glossLevel = currentGlossLevel,
                                onClick = {
                                    showHomeQuickMenu = false
                                    widgetPickerDestination = "app_page:${pagerState.currentPage}"
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // ── Transition FLUIDE des overlays plein écran (WidgetPage / App Library) ──────────
        // Les pages 0 (widgets) et libraryPageIndex (App Library) du pager sont des Box VIDES :
        // le vrai contenu est rendu en overlay ici. Avant, un AnimatedVisibility (fondu + zoom)
        // déclenché seulement APRÈS le settle de la page faisait « pop » le contenu de façon
        // saccadée et déconnectée du doigt. Désormais l'overlay SUIT la position du pager via une
        // simple translation GPU (translationX) — pas de scale → aucune re-mesure de
        // l'AppWidgetHostView ni du contenu lourd → glisse en parfaite synchro avec le swipe.
        val overlayScreenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }

        // PERF : la position live du pager (currentPageOffsetFraction) change à CHAQUE frame
        // pendant un swipe. La lire ici, dans le corps du composable, recomposerait tout
        // HomeScreenContent 60×/s → jank. On ne la lit donc QUE dans le graphicsLayer (phase de
        // dessin : seul le layer se ré-exécute, pas de recomposition). La visibilité est gardée
        // par un booléen dérivé qui ne bascule qu'au franchissement du seuil (entrée/sortie).

        // Overlay page Widget (page 0, à gauche) — translationX = (0 - position)·largeur.
        // Composition COLLANTE : on pré-chauffe quand on est posé sur la page adjacente
        // (|rel| < 1.5 → inflation des AppWidgetHostView à l'arrêt, ouverture fluide), PUIS on ne
        // détruit JAMAIS l'overlay. Sinon, swiper entre pages d'accueil franchirait le seuil et
        // composerait/détruirait l'overlay EN PLEIN SWIPE (teardown des host views = jank).
        // Une fois chaud, il reste hors écran (layer culé/repositionné, coût quasi nul).
        val widgetOverlayNear by remember {
            derivedStateOf {
                kotlin.math.abs(WIDGET_PAGE_INDEX - (pagerState.currentPage + pagerState.currentPageOffsetFraction)) < 1.5f
            }
        }
        var widgetOverlayWarmed by remember { mutableStateOf(false) }
        LaunchedEffect(widgetOverlayNear) { if (widgetOverlayNear) widgetOverlayWarmed = true }
        if (widgetOverlayWarmed) {
          Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val rel = WIDGET_PAGE_INDEX - (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                    translationX = rel * overlayScreenWidthPx
                    // Hors écran (|rel| >= 1) : alpha 0 → le RenderThread NE COMPOSITE PAS le layer
                    // (zéro coût GPU) tout en gardant le contenu composé. Repasse à 1 dès qu'il
                    // entre à l'écran. Lu en phase de dessin → pas de recomposition.
                    alpha = if (kotlin.math.abs(rel) < 1f) 1f else 0f
                }
          ) {
            WidgetPage(
                widgetLayout = widgetLayout,
                editMode = editMode,
                resizeState = widgetResizeState,
                hazeState = hazeState,
                glassLevel = currentGlassLevel,
                glassTint = currentGlassTint,
                customTintColor = customGlassTintColor,
                loupeLevel = currentLoupeLevel,
                glossLevel = currentGlossLevel,
                onAddWidget = { widgetPickerDestination = "widget_page" },
                onRemoveWidget = { id -> removeWidget(id) },
                onResizeWidget = { id, rect ->
                    updateWidgetLayout(widgetLayout.copy(
                        placements = widgetLayout.placements.map {
                            if (it.widgetId == id) it.copy(
                                col = rect.col, row = rect.row,
                                colSpan = rect.colSpan, rowSpan = rect.rowSpan
                            ) else it
                        }
                    ))
                },
                onReorderWidgets = { from, to ->
                    if (from != to) {
                        val pagePlacements = widgetLayout.placements
                            .filter { it.pageIndex == WIDGET_PAGE_INDEX }
                            .sortedBy { it.row }
                        val others = widgetLayout.placements.filter { it.pageIndex != WIDGET_PAGE_INDEX }
                        val reordered = pagePlacements.toMutableList().also {
                            val item = it.removeAt(from)
                            it.add(to, item)
                        }
                        val reassigned = reordered.mapIndexed { idx, p -> p.copy(row = idx.toFloat()) }
                        updateWidgetLayout(widgetLayout.copy(placements = others + reassigned))
                    }
                },
                onSwipeBack = {
                    scope.launch { pagerState.animateScrollToPage(appPagesStart) }
                },
                onWidgetLongPress = { widgetId, touchPos ->
                    widgetContextMenuId = widgetId
                    widgetContextMenuPos = touchPos
                },
                navEnabled = onWidgetPage
            )
          }
        }

        // Dim + menu contextuel widget page — rendu APRÈS WidgetPage pour être au-dessus.
        androidx.compose.animation.AnimatedVisibility(
            visible = onWidgetPage && widgetContextMenuId != null,
            enter = androidx.compose.animation.fadeIn(
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 280)
            ),
            exit = androidx.compose.animation.fadeOut(
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 200)
            )
        ) {
            val widgetId = widgetContextMenuId ?: ""
            val placement = widgetLayout.placements.find { it.widgetId == widgetId }
            val info = placement?.let { awm.getAppWidgetInfo(it.appWidgetId) }
            val hasConfigure = info?.configure != null

            val dismissWidgetPageMenu = {
                widgetContextMenuId = null
                widgetContextMenuPos = null
            }

            val widgetPageMenuActions = buildList {
                if (hasConfigure) {
                    add(ContextMenuAction(
                        title = "Paramètres du widget",
                        icon = Icons.Default.Settings,
                        onClick = {
                            dismissWidgetPageMenu()
                            info?.configure?.let { cfg ->
                                reconfigureLauncher.launch(ConfigureWidgetInput(placement!!.appWidgetId, cfg))
                            }
                        }
                    ))
                }
                add(ContextMenuAction(
                    title = "Éditer la page des widgets",
                    icon = Icons.Default.Edit,
                    onClick = { dismissWidgetPageMenu(); editMode.enter() }
                ))
                add(ContextMenuAction(
                    title = "Supprimer",
                    icon = Icons.Default.RemoveCircle,
                    isDestructive = true,
                    onClick = {
                        dismissWidgetPageMenu()
                        removeWidget(widgetId)
                    }
                ))
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.38f))
                    .clickable { dismissWidgetPageMenu() }
            ) {
                val densityWp = LocalDensity.current
                val menuWidthWp = 260.dp
                val menuHeightWp = (widgetPageMenuActions.size * 46 + 16).dp
                val screenWidthWp = LocalConfiguration.current.screenWidthDp.dp
                val screenHeightWp = LocalConfiguration.current.screenHeightDp.dp
                val touchPosWp = widgetContextMenuPos
                val touchYWp = if (touchPosWp != null) with(densityWp) { touchPosWp.y.toDp() }
                                else screenHeightWp / 2f

                var menuXWp = if (touchPosWp != null) with(densityWp) { touchPosWp.x.toDp() }
                               else screenWidthWp / 2f
                menuXWp = menuXWp.coerceIn(16.dp, screenWidthWp - menuWidthWp - 16.dp)
                var menuYWp = touchYWp + 12.dp
                if (menuYWp + menuHeightWp > screenHeightWp - 16.dp) {
                    menuYWp = touchYWp - menuHeightWp - 12.dp
                }
                menuYWp = menuYWp.coerceIn(40.dp, screenHeightWp - menuHeightWp - 16.dp)

                Box(modifier = Modifier.offset(x = menuXWp, y = menuYWp)) {
                    ContextMenu(actions = widgetPageMenuActions, onDismiss = dismissWidgetPageMenu)
                }
            }
        }

        // Overlay App Library (page libraryPageIndex, à droite) — même transition fluide par
        // translation que la page widgets : translationX = (libraryPageIndex - position)·largeur.
        // Composition COLLANTE (cf. overlay widgets) : pré-chauffe sur la page adjacente puis ne
        // détruit jamais → les sections/grille/vignettes restent vivantes (réouverture instantanée)
        // et aucun compose/dispose pendant les swipes d'accueil.
        val libraryOverlayNear by remember(libraryPageIndex) {
            derivedStateOf {
                kotlin.math.abs(libraryPageIndex - (pagerState.currentPage + pagerState.currentPageOffsetFraction)) < 1.5f
            }
        }
        var libraryOverlayWarmed by remember { mutableStateOf(false) }
        LaunchedEffect(libraryOverlayNear) { if (libraryOverlayNear) libraryOverlayWarmed = true }
        if (libraryOverlayWarmed) {
          Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val rel = libraryPageIndex - (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                    translationX = rel * overlayScreenWidthPx
                    // Hors écran : alpha 0 → pas de compositing GPU (cf. overlay widgets).
                    alpha = if (kotlin.math.abs(rel) < 1f) 1f else 0f
                }
          ) {
            AppLibraryScreen(
                allApps = allInstalledApps,
                suggestedApps = suggestedApps,
                state = appLibraryState,
                hazeState = hazeState,
                glassLevel = currentGlassLevel,
                glassTint = currentGlassTint,
                customTintColor = customGlassTintColor,
                loupeLevel = currentLoupeLevel,
                                    glossLevel = currentGlossLevel,
                onLaunchApp = { app -> appRepository.launchApp(app.packageName) },
                onSwipeBack = {
                    // Retour à la dernière page d'apps (juste avant l'App Library).
                    scope.launch {
                        pagerState.animateScrollToPage((libraryPageIndex - 1).coerceAtLeast(appPagesStart))
                    }
                },
                isEditing = editMode.isEditing,
                onLibraryAppDragStart = { app, startPos ->
                    dragState.start(HomeItem.App(app), startPos)
                },
                onRequestEditMode = { editMode.enter() },
                onDeleteApp = { app ->
                    val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            )
          }
        }

        // Overlay dossier ouvert (plein écran, par-dessus la grille)
        // zIndex bas pour que les menus contextuels (déclarés avant mais avec zIndex plus haut) passent devant.
        androidx.compose.animation.AnimatedVisibility(
            visible = openFolder != null,
            enter = androidx.compose.animation.fadeIn() +
                    androidx.compose.animation.scaleIn(initialScale = 0.92f),
            exit = androidx.compose.animation.fadeOut() +
                    androidx.compose.animation.scaleOut(targetScale = 0.92f),
            modifier = Modifier.zIndex(10f)
        ) {
            openFolder?.let { folder ->
                FolderOverlay(
                    folder = folder,
                    editMode = editMode,
                    hazeState = hazeState,
                    glassLevel = currentGlassLevel,
                    glassTint = currentGlassTint,
                    customTintColor = customGlassTintColor,
                    loupeLevel = currentLoupeLevel,
                                    glossLevel = currentGlossLevel,
                    onLaunchApp = { app -> appRepository.launchApp(app.packageName) },
                    onRename = renameOpenFolder,
                    onReorder = reorderOpenFolder,
                    onPullOut = pullOutOfFolder,
                    onRemove = removeFromOpenFolder,
                    onClose = { openFolderLoc = null },
                    onRequestContextMenu = { app, rect ->
                        contextMenuItem = HomeItem.App(app)
                        contextMenuSourceRect = rect
                    },
                    notificationCounts = notificationCounts
                )
            }
        }

        // Overlay Spotlight (apparaît par-dessus tout quand activé)
        // Control Center Fruit OS (swipe-bas depuis le coin haut-droite)
        com.stanleycx.fruitos.ui.controlcenter.ControlCenterOverlay(
            state = controlCenterState,
            hazeState = hazeState,
            glassLevel = currentGlassLevel,
            glassTint = currentGlassTint,
            customTintColor = customGlassTintColor,
            loupeLevel = currentLoupeLevel,
            glossLevel = currentGlossLevel
        )

        com.stanleycx.fruitos.ui.spotlight.SpotlightOverlay(
            allApps = allInstalledApps,
            suggestedApps = suggestedApps,
            state = spotlightState,
            appRepository = appRepository,
            hazeState = hazeState,
            glassLevel = currentGlassLevel,
            glassTint = currentGlassTint,
            customTintColor = customGlassTintColor,
            loupeLevel = currentLoupeLevel,
                                    glossLevel = currentGlossLevel,
            hasUsagePermission = hasUsagePermission,
            onRequestUsagePermission = {
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS
                )
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        )

        // Le fantôme de drag par-dessus tout le reste
        DragGhost(
            dragState = dragState,
            hazeState = hazeState,
            glassLevel = currentGlassLevel,
            glassTint = currentGlassTint,
            customTintColor = customGlassTintColor,
            loupeLevel = currentLoupeLevel,
            glossLevel = currentGlossLevel
        )

        // Petit dialogue de renommage de dossier (depuis le menu contextuel)
        // On recherche toujours le dossier dans le currentState le plus récent pour éviter tout conflit
        // avec le renommage classique (depuis l'overlay).
        val folderBeingRenamed = remember(renamingFolderId, currentState) {
            renamingFolderId?.let { id ->
                currentState.pages.asSequence()
                    .flatMap { it.values.asSequence() }
                    .filterIsInstance<HomeItem.Folder>()
                    .firstOrNull { it.id == id }
            }
        }

        folderBeingRenamed?.let { folder ->
            FolderRenameDialog(
                currentName = folder.name,
                onDismiss = { renamingFolderId = null },
                onConfirm = { newName ->
                    val idToRename = renamingFolderId
                    if (idToRename != null && newName.isNotBlank()) {
                        // On recherche et met à jour en utilisant l'état le plus récent possible
                        val latestFolder = currentState.pages.asSequence()
                            .flatMap { it.values.asSequence() }
                            .filterIsInstance<HomeItem.Folder>()
                            .firstOrNull { it.id == idToRename }

                        if (latestFolder != null && newName != latestFolder.name) {
                            val newPages = currentState.pages.map { page ->
                                page.mapValues { (_, item) ->
                                    if (item is HomeItem.Folder && item.id == idToRename) {
                                        item.copy(name = newName.trim())
                                    } else item
                                }
                            }
                            updateState(currentState.copy(pages = newPages))
                        }
                    }
                    renamingFolderId = null
                }
            )
        }

        // === Renommage de widget (nom + option « Masquer le nom ») ===
        val widgetBeingRenamed = remember(renamingWidgetId, widgetLayout) {
            renamingWidgetId?.let { id -> widgetLayout.placements.find { it.widgetId == id } }
        }
        widgetBeingRenamed?.let { placement ->
            val providerName = remember(placement.appWidgetId) {
                runCatching { awm.getAppWidgetInfo(placement.appWidgetId)?.loadLabel(context.packageManager) }
                    .getOrNull() ?: "Widget"
            }
            WidgetRenameDialog(
                currentName = placement.label ?: providerName,
                hidden = placement.labelHidden,
                onDismiss = { renamingWidgetId = null },
                onConfirm = { newName, hide ->
                    val id = renamingWidgetId
                    if (id != null) {
                        val trimmed = newName.trim()
                        // Vide ou identique au nom du provider → on stocke null (= nom par défaut).
                        val newLabel = if (trimmed.isBlank() || trimmed == providerName) null else trimmed
                        updateWidgetLayout(widgetLayout.copy(placements = widgetLayout.placements.map {
                            if (it.widgetId == id) it.copy(label = newLabel, labelHidden = hide) else it
                        }))
                    }
                    renamingWidgetId = null
                }
            )
        }

        // === Widget picker (overlay plein écran) ===
        WidgetPickerSheet(
            visible = widgetPickerDestination != null,
            onDismiss = { widgetPickerDestination = null },
            onPickWidget = { info ->
                startAddWidget(info, widgetPickerDestination ?: "widget_page")
            },
            hazeState = hazeState,
            glassLevel = currentGlassLevel,
            glassTint = currentGlassTint,
            customTintColor = customGlassTintColor,
            loupeLevel = currentLoupeLevel,
            glossLevel = currentGlossLevel
        )

        // === Écrans de réglages plein écran (overlays) ===
        // Enveloppés dans un Box plein écran qui :
        //  1. détecte un SWIPE depuis le bord gauche → ferme l'overlay du dessus
        //     (geste de navigation "retour", car le BackHandler système ne nous
        //      parvient pas : on est le launcher par défaut, le back garde le home).
        //  2. CONSOMME les touches résiduelles → les zones "vides" ne laissent plus
        //     passer les taps/scrolls vers l'écran d'accueil en dessous.
        // Tout se passe en passe Main (par défaut) : les enfants (LazyColumn, lignes
        // cliquables) traitent l'event en premier (bubbling enfant→parent), donc le
        // scroll interne et les clics fonctionnent ; ce Box ne fait qu'avaler le reste.
        if (anyFullScreenOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(showVisualSettings, showGlassPreview, showGlassTintScreen, showLoupeLevelScreen, showGlossinessScreen, showIconStyleScreen) {
                        val edgeWidthPx = 36.dp.toPx()       // largeur de la zone "bord gauche"
                        val dismissThresholdPx = 90.dp.toPx() // distance horizontale pour valider le retour
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val fromLeftEdge = down.position.x <= edgeWidthPx
                                // childGrabbed = un enfant (scroll, slider, ligne cliquable…) a
                                // pris le geste → on ne fait plus RIEN (ni retour, ni consume).
                                var childGrabbed = down.isConsumed
                                // On consomme uniquement le DOWN (pas les mouvements) → bloque la
                                // fuite des taps vers le home derrière l'overlay SANS gêner le
                                // scroll (le scroll enfant lit les MOUVEMENTS, déjà obtenus avant
                                // nous en passe Main). Plus de consume systématique des moves =
                                // le scroll se grab partout désormais.
                                if (!childGrabbed) down.consume()
                                var accDx = 0f
                                var accDy = 0f
                                var handled = false
                                var pointerUp = false
                                while (!pointerUp) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change == null) {
                                        pointerUp = true
                                    } else {
                                        if (!change.pressed) pointerUp = true
                                        if (change.isConsumed) childGrabbed = true
                                        accDx += change.position.x - change.previousPosition.x
                                        accDy += change.position.y - change.previousPosition.y
                                        // Swipe droite depuis le bord gauche, tant qu'aucun enfant
                                        // n'a pris le geste = retour (ferme l'overlay le + profond).
                                        if (fromLeftEdge && !handled && !childGrabbed &&
                                            accDx > dismissThresholdPx &&
                                            accDx > kotlin.math.abs(accDy) * 1.5f
                                        ) {
                                            when {
                                                showIconStyleScreen -> showIconStyleScreen = false
                                                showGlossinessScreen -> showGlossinessScreen = false
                                                showLoupeLevelScreen -> showLoupeLevelScreen = false
                                                showGlassTintScreen -> showGlassTintScreen = false
                                                showGlassPreview -> showGlassPreview = false
                                                showVisualSettings -> showVisualSettings = false
                                            }
                                            handled = true
                                        }
                                        // On n'avale les mouvements QUE si on exécute le retour.
                                        if (handled) change.consume()
                                    }
                                }
                            }
                        }
                    }
            ) {
                // === Page Réglages complète style Fruit OS ===
                if (showVisualSettings) {
                    // Back système = revenir au home (ferme les réglages).
                    // Désactivé si un sous-écran est ouvert (il a son propre BackHandler prioritaire).
                    BackHandler(enabled = !showGlassPreview && !showGlassTintScreen && !showLoupeLevelScreen && !showGlossinessScreen && !showIconStyleScreen) {
                        showVisualSettings = false
                    }
                    com.stanleycx.fruitos.ui.settings.LauncherSettingsScreen(
                        currentGlassLevel = currentGlassLevel,
                        onGlassLevelChange = { currentGlassLevel = it },
                        currentGlassTint = currentGlassTint,
                        onGlassTintChange = { currentGlassTint = it },
                        customGlassTintColor = customGlassTintColor,
                        onCustomGlassTintColorChange = { customGlassTintColor = it },
                        currentLoupeLevel = currentLoupeLevel,
                        onLoupeLevelChange = { currentLoupeLevel = it },
                        currentGlossLevel = currentGlossLevel,
                        onGlossLevelChange = { currentGlossLevel = it },
                        darkModeEnabled = darkModeEnabled,
                        onDarkModeChange = { darkModeEnabled = it },
                        onOpenGlassLevel = { showGlassPreview = true },
                        onOpenGlassTint = { showGlassTintScreen = true },
                        onOpenLoupeLevel = { showLoupeLevelScreen = true },
                        onOpenGlossiness = { showGlossinessScreen = true },
                        iconStyleSubtitle = when (iconStyleMode) {
                            com.stanleycx.fruitos.ui.components.IconStyleMode.Default -> "Défaut"
                            com.stanleycx.fruitos.ui.components.IconStyleMode.Dark    -> "Sombre"
                            com.stanleycx.fruitos.ui.components.IconStyleMode.Tinted  -> "Teinté"
                            com.stanleycx.fruitos.ui.components.IconStyleMode.Glass   -> "Verre"
                        },
                        onOpenIconStyle = { showIconStyleScreen = true },
                        onClose = { showVisualSettings = false }
                    )
                }

                if (showGlassPreview) {
                    BackHandler { showGlassPreview = false }
                    com.stanleycx.fruitos.ui.settings.GlassPreviewScreen(
                        currentLevel = currentGlassLevel,
                        onLevelSelected = { currentGlassLevel = it },
                        onBack = { showGlassPreview = false },
                        hazeState = hazeState,
                        glassTint = currentGlassTint,
                        customTintColor = customGlassTintColor,
                        loupeLevel = currentLoupeLevel,
                        glossLevel = currentGlossLevel
                    )
                }

                if (showGlassTintScreen) {
                    BackHandler { showGlassTintScreen = false }
                    com.stanleycx.fruitos.ui.settings.GlassTintScreen(
                        currentTint = currentGlassTint,
                        onTintSelected = { currentGlassTint = it },
                        customTintColor = customGlassTintColor,
                        onCustomTintColorSelected = { color ->
                            customGlassTintColor = color
                            currentGlassTint = GlassTint.Custom
                        },
                        currentLevel = currentGlassLevel,
                        onBack = { showGlassTintScreen = false },
                        hazeState = hazeState
                    )
                }

                if (showLoupeLevelScreen) {
                    BackHandler { showLoupeLevelScreen = false }
                    com.stanleycx.fruitos.ui.settings.LoupeLevelScreen(
                        currentLevel = currentLoupeLevel,
                        onLevelSelected = { currentLoupeLevel = it },
                        onBack = { showLoupeLevelScreen = false },
                        hazeState = hazeState,
                        glassLevel = currentGlassLevel,
                        glassTint = currentGlassTint,
                        customTintColor = customGlassTintColor,
                        glossLevel = currentGlossLevel
                    )
                }

                if (showGlossinessScreen) {
                    BackHandler { showGlossinessScreen = false }
                    com.stanleycx.fruitos.ui.settings.GlossinessScreen(
                        currentGloss = currentGlossLevel,
                        onGlossSelected = { currentGlossLevel = it },
                        onBack = { showGlossinessScreen = false },
                        hazeState = hazeState,
                        glassLevel = currentGlassLevel,
                        glassTint = currentGlassTint,
                        customTintColor = customGlassTintColor,
                        loupeLevel = currentLoupeLevel
                    )
                }

                if (showIconStyleScreen) {
                    BackHandler { showIconStyleScreen = false }
                    com.stanleycx.fruitos.ui.settings.IconStyleScreen(
                        current = iconStyle,
                        onChange = {
                            iconStyleMode = it.mode
                            iconTintColor = it.tintColor
                            iconGlassGlyph = it.glassGlyph
                            iconGlassOpacity = it.glassGlyphOpacity
                            iconGlassBrightness = it.glassGlyphBrightness
                            iconGlassGlyphTint = it.glassGlyphTintColor
                            iconGlassTintSource = it.glassTintSource
                            iconGlassCustomTint = it.glassCustomTint
                            iconLightBorder = it.lightBorder
                        },
                        onBack = { showIconStyleScreen = false },
                        hazeState = hazeState,
                        previewApps = allInstalledApps.take(4)
                    )
                }
            }
        }

        // === Personnalisation d'icône PAR APP (overlay plein écran, par-dessus tout) ===
        customizingApp?.let { app ->
            BackHandler { customizingApp = null }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(300f)  // Au-dessus de tout, y compris le FolderOverlay si jamais il n'était pas encore fermé
                    // Bloque la fuite des taps vers le home derrière (consomme le down seulement,
                    // les enfants l'ont déjà reçu → scroll/sliders intacts).
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                if (!down.isConsumed) down.consume()
                            }
                        }
                    }
            ) {
                com.stanleycx.fruitos.ui.settings.IconCustomizeScreen(
                    app = app,
                    current = iconOverrides[app.packageName] ?: com.stanleycx.fruitos.ui.components.IconOverride(),
                    onChange = { setOverride(app.packageName, it) },
                    onReset = { setOverride(app.packageName, com.stanleycx.fruitos.ui.components.IconOverride()) },
                    onBack = { customizingApp = null },
                    hazeState = hazeState
                )
            }
        }
    }
    }
}
