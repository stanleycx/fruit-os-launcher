package com.stanleycx.fruitos.ui.controlcenter

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import com.stanleycx.fruitos.data.NotificationService
import org.json.JSONArray
import org.json.JSONObject

// ─────────────────────────────────────────────────────────────────────────────
// Réglages système (luminosité + rotation) — nécessitent WRITE_SETTINGS
// ─────────────────────────────────────────────────────────────────────────────

fun canWriteSettings(context: Context): Boolean = Settings.System.canWrite(context)

/** Ouvre l'écran d'octroi de WRITE_SETTINGS pour notre app. */
fun requestWriteSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_WRITE_SETTINGS,
        "package:${context.packageName}".toUri()
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

/** Luminosité écran 0f..1f (lecture toujours possible). */
fun getBrightness(context: Context): Float = runCatching {
    Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
}.getOrDefault(0.5f).coerceIn(0f, 1f)

/** Écrit la luminosité (passe en mode manuel). Nécessite WRITE_SETTINGS. */
fun setBrightness(context: Context, value: Float) {
    if (!canWriteSettings(context)) return
    runCatching {
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            (value * 255f).toInt().coerceIn(1, 255)
        )
    }
}

/** true = rotation verrouillée (auto-rotation OFF). */
fun isRotationLocked(context: Context): Boolean = runCatching {
    Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION) == 0
}.getOrDefault(true)

fun setRotationLocked(context: Context, locked: Boolean) {
    if (!canWriteSettings(context)) return
    runCatching {
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            if (locked) 0 else 1
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Volume (AudioManager) — aucune permission spéciale
// ─────────────────────────────────────────────────────────────────────────────

fun audioMaxVolume(context: Context): Int =
    (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager)
        .getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

fun audioVolume(context: Context): Int =
    (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager)
        .getStreamVolume(AudioManager.STREAM_MUSIC)

fun setAudioVolume(context: Context, value: Int) {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    runCatching {
        am.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            value.coerceIn(0, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)),
            0
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Lampe torche (CameraManager) — aucune permission spéciale
// ─────────────────────────────────────────────────────────────────────────────

/** Contrôleur de torche : suit l'état réel via TorchCallback. */
class TorchController(context: Context) {
    private val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val cameraId: String? = runCatching {
        cm.cameraIdList.firstOrNull { id ->
            cm.getCameraCharacteristics(id)
                .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }.getOrNull()

    val available: Boolean get() = cameraId != null

    fun set(on: Boolean) {
        val id = cameraId ?: return
        runCatching { cm.setTorchMode(id, on) }
    }
}

/** État live de la torche (observe les changements système). */
@Composable
fun rememberTorchState(context: Context): Pair<Boolean, (Boolean) -> Unit> {
    val controller = remember { TorchController(context) }
    var on by remember { mutableStateOf(false) }
    DisposableEffect(controller) {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cb = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(id: String, enabled: Boolean) { on = enabled }
        }
        cm.registerTorchCallback(cb, Handler(Looper.getMainLooper()))
        onDispose { cm.unregisterTorchCallback(cb) }
    }
    return on to { value -> controller.set(value) }
}

// ─────────────────────────────────────────────────────────────────────────────
// Connectivité : on ne peut PAS toggler directement (Android 10+) → on ouvre les panneaux
// ─────────────────────────────────────────────────────────────────────────────

private fun launch(context: Context, intent: Intent) {
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

// ── État on/off (pour afficher les toggles allumés/transparents) ─────────────

data class ConnStates(
    val airplane: Boolean = false,
    val cellular: Boolean = false,
    val wifi: Boolean = false,
    val bluetooth: Boolean = false,
)

private fun readConnStates(context: Context): ConnStates {
    val airplane = runCatching {
        Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
    }.getOrDefault(false)

    val wifi = runCatching {
        (context.applicationContext.getSystemService(Context.WIFI_SERVICE)
                as android.net.wifi.WifiManager).isWifiEnabled
    }.getOrDefault(false)

    val bluetooth = runCatching {
        (context.getSystemService(Context.BLUETOOTH_SERVICE)
                as android.bluetooth.BluetoothManager).adapter?.isEnabled == true
    }.getOrDefault(false)

    // Données mobiles activées (sinon repli : actif si pas en mode avion).
    val cellular = runCatching {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
        tm.isDataEnabled
    }.getOrDefault(!airplane)

    return ConnStates(airplane, cellular && !airplane, wifi, bluetooth)
}

/**
 * États connectivité live (mis à jour via broadcasts système).
 * CONNECTIVITY_ACTION est déprécié mais reste le broadcast le plus simple pour rafraîchir
 * l'état cellulaire ici ; le remplacement (NetworkCallback) serait plus lourd sans gain visible.
 */
@Suppress("DEPRECATION")
@Composable
fun rememberConnStates(context: Context): ConnStates {
    var states by remember { mutableStateOf(readConnStates(context)) }
    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { states = readConnStates(context) }
        }
        val filter = android.content.IntentFilter().apply {
            addAction("android.intent.action.AIRPLANE_MODE")
            addAction(android.net.wifi.WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION)
        }
        runCatching { context.registerReceiver(receiver, filter) }
        states = readConnStates(context)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return states
}

fun openWifiPanel(context: Context) =
    launch(context, Intent(Settings.Panel.ACTION_WIFI))

fun openInternetPanel(context: Context) =
    launch(context, Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))

fun openBluetoothSettings(context: Context) =
    launch(context, Intent(Settings.ACTION_BLUETOOTH_SETTINGS))

fun openAirplaneSettings(context: Context) =
    launch(context, Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))

// ─────────────────────────────────────────────────────────────────────────────
// Raccourcis apps
// ─────────────────────────────────────────────────────────────────────────────

fun launchCalculator(context: Context) =
    launch(context, Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR))

fun launchCamera(context: Context) =
    launch(context, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))

fun launchTimer(context: Context) =
    launch(context, Intent(AlarmClock.ACTION_SHOW_TIMERS))

fun launchApp(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ?: return
    runCatching { context.startActivity(intent) }
}

/** Pour une tuile TileService : lance l'app ou ouvre le volet QS en fallback. */
fun launchTileOrApp(context: Context, pkg: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (intent != null) runCatching { context.startActivity(intent) }
    else expandNotificationsPanel(context)
}

// ─────────────────────────────────────────────────────────────────────────────
// TileService — listage des tuiles Quick Settings des apps installées
// ─────────────────────────────────────────────────────────────────────────────

data class TileServiceInfo(
    val pkg: String,
    val componentName: String,
    val label: String,
)

fun loadTileServices(context: Context): List<TileServiceInfo> = runCatching {
    val pm = context.packageManager
    val intent = Intent("android.service.quicksettings.action.QS_TILE")
    pm.queryIntentServices(intent, 0).mapNotNull { ri ->
        runCatching {
            TileServiceInfo(
                pkg = ri.serviceInfo.packageName,
                componentName = ri.serviceInfo.name,
                label = ri.serviceInfo.loadLabel(pm).toString()
            )
        }.getOrNull()
    }
}.getOrDefault(emptyList())

/**
 * Ouvre le volet de notifications Android (la status bar système étant cachée, c'est notre
 * seul accès). Réflexion sur StatusBarManager.expandNotificationsPanel() — best-effort.
 */
@Suppress("WrongConstant")
fun expandNotificationsPanel(context: Context) {
    runCatching {
        val sb = context.getSystemService("statusbar")
        val cls = Class.forName("android.app.StatusBarManager")
        cls.getMethod("expandNotificationsPanel").invoke(sb)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Lecteur média réel (MediaSessionManager) — utilise le NotificationListener déjà actif
// ─────────────────────────────────────────────────────────────────────────────

data class MediaUiState(
    val active: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val art: Bitmap? = null,
    val isPlaying: Boolean = false,
    val duration: Long = 0L,
    val controller: MediaController? = null
) {
    fun playPause() {
        val c = controller ?: return
        if (isPlaying) c.transportControls.pause() else c.transportControls.play()
    }
    fun next() { controller?.transportControls?.skipToNext() }
    fun prev() { controller?.transportControls?.skipToPrevious() }
    fun seekTo(pos: Long) { controller?.transportControls?.seekTo(pos.coerceIn(0L, duration)) }

    /** Position courante extrapolée (ms) à partir du dernier playbackState. */
    fun livePosition(): Long {
        val ps = controller?.playbackState ?: return 0L
        var pos = ps.position
        if (ps.state == PlaybackState.STATE_PLAYING) {
            val delta = android.os.SystemClock.elapsedRealtime() - ps.lastPositionUpdateTime
            pos += (delta * ps.playbackSpeed).toLong()
        }
        return pos.coerceIn(0L, if (duration > 0) duration else Long.MAX_VALUE)
    }
}

/** Couleur dominante (moyenne) d'une pochette, pour le dégradé ambiant Fruit OS. */
fun dominantColorOf(bitmap: Bitmap?): androidx.compose.ui.graphics.Color? {
    bitmap ?: return null
    return runCatching {
        val small = android.graphics.Bitmap.createScaledBitmap(bitmap, 16, 16, true)
        var r = 0L; var g = 0L; var b = 0L; var n = 0
        for (y in 0 until small.height) for (x in 0 until small.width) {
            val p = small.getPixel(x, y)
            r += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; b += p and 0xFF; n++
        }
        if (n == 0) return null
        androidx.compose.ui.graphics.Color(
            red = (r / n).toInt(), green = (g / n).toInt(), blue = (b / n).toInt()
        )
    }.getOrNull()
}

/**
 * Observe la session média active (now-playing) en temps réel.
 * Nécessite que le NotificationListener soit activé (déjà le cas pour les badges).
 */
@Composable
fun rememberActiveMedia(context: Context): MediaUiState {
    var state by remember { mutableStateOf(MediaUiState()) }

    DisposableEffect(Unit) {
        val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        val component = ComponentName(context, NotificationService::class.java)
        val handler = Handler(Looper.getMainLooper())

        var current: MediaController? = null
        var controllerCb: MediaController.Callback? = null

        fun publish(c: MediaController?) {
            if (c == null) { state = MediaUiState(); return }
            val md = c.metadata
            val ps = c.playbackState
            val title = md?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE).orEmpty()
            val artist = md?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)
                ?: md?.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty()
            val art = md?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: md?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
            val album = md?.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM).orEmpty()
            val duration = md?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION) ?: 0L
            val playing = ps?.state == PlaybackState.STATE_PLAYING
            state = MediaUiState(
                active = title.isNotBlank() || ps != null,
                title = title.ifBlank { "Lecture en cours" },
                artist = artist,
                album = album,
                art = art,
                isPlaying = playing,
                duration = duration,
                controller = c
            )
        }

        fun bind(c: MediaController?) {
            if (current === c) { publish(c); return }
            controllerCb?.let { cb -> current?.unregisterCallback(cb) }
            current = c
            if (c != null) {
                val cb = object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(s: PlaybackState?) { publish(c) }
                    override fun onMetadataChanged(m: android.media.MediaMetadata?) { publish(c) }
                    override fun onSessionDestroyed() { bind(null) }
                }
                controllerCb = cb
                c.registerCallback(cb, handler)
            }
            publish(c)
        }

        val listener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
            bind(list?.firstOrNull())
        }

        var registered = false
        runCatching {
            msm?.addOnActiveSessionsChangedListener(listener, component, handler)
            registered = true
            bind(msm?.getActiveSessions(component)?.firstOrNull())
        }

        onDispose {
            controllerCb?.let { cb -> current?.unregisterCallback(cb) }
            if (registered) runCatching { msm?.removeOnActiveSessionsChangedListener(listener) }
        }
    }

    return state
}

// ─────────────────────────────────────────────────────────────────────────────
// Focus / DND (ACCESS_NOTIFICATION_POLICY requis pour modifier)
// ─────────────────────────────────────────────────────────────────────────────

data class DndUiState(
    val active: Boolean = false,
    val mode: Int = NotificationManager.INTERRUPTION_FILTER_PRIORITY
)

private fun readDndState(context: Context): DndUiState {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val filter = runCatching { nm.currentInterruptionFilter }
        .getOrDefault(NotificationManager.INTERRUPTION_FILTER_ALL)
    val active = filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
            filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    return DndUiState(
        active = active,
        mode = if (active) filter else NotificationManager.INTERRUPTION_FILTER_PRIORITY
    )
}

fun applyDnd(context: Context, newState: DndUiState) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (!nm.isNotificationPolicyAccessGranted) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        return
    }
    runCatching {
        nm.setInterruptionFilter(
            if (newState.active) newState.mode
            else NotificationManager.INTERRUPTION_FILTER_ALL
        )
    }
}

@Composable
fun rememberDndState(context: Context): Pair<DndUiState, (DndUiState) -> Unit> {
    var state by remember { mutableStateOf(readDndState(context)) }
    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { state = readDndState(context) }
        }
        val filter = android.content.IntentFilter(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        runCatching { context.registerReceiver(receiver, filter) }
        state = readDndState(context)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return state to { newState ->
        applyDnd(context, newState)
        state = readDndState(context)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Volume média live (se met à jour quand les boutons physiques changent le volume)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun rememberLiveVolume(context: Context): Pair<Float, (Float) -> Unit> {
    val max = remember { audioMaxVolume(context) }
    var vol by remember { mutableFloatStateOf(audioVolume(context).toFloat() / max) }
    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                val stream = i?.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1) ?: -1
                if (stream == AudioManager.STREAM_MUSIC || stream == -1) {
                    vol = audioVolume(context).toFloat() / max
                }
            }
        }
        val filter = android.content.IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        runCatching { context.registerReceiver(receiver, filter) }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return vol to { v -> vol = v; setAudioVolume(context, (v * max).toInt()) }
}

// ─────────────────────────────────────────────────────────────────────────────
// Raccourcis Control Center — persistance SharedPreferences (org.json)
// ─────────────────────────────────────────────────────────────────────────────

/** extra = nom de classe du composant (pour type TILE : fully-qualified ServiceInfo.name). */
data class CcShortcut(val type: String, val pkg: String? = null, val label: String = "", val extra: String? = null)

private const val CC_PREFS_NAME = "cc_shortcuts"
// v2 : inclut les tuiles système (ROTATION, FOCUS, BRIGHTNESS, VOLUME) en plus des raccourcis.
private const val CC_PREFS_KEY = "tiles_v2"

// ─── Taille de chaque tuile dans la grille 4-colonnes ────────────────────────
fun CcShortcut.colSpan(): Int = when (type) { "FOCUS" -> 2; else -> 1 }
fun CcShortcut.rowSpan(): Int = when (type) { "BRIGHTNESS", "VOLUME" -> 2; else -> 1 }

data class TilePosition(val col: Int, val row: Int, val colSpan: Int, val rowSpan: Int)

/**
 * Packing glouton gauche→droite, haut→bas dans une grille de [gridCols] colonnes.
 * Les tuiles multi-lignes (rowSpan > 1) commencent à la ligne 1 pour éviter d'occuper
 * des cases libres dans la ligne 0 et s'intercaler avec des tuiles 1-ligne.
 */
fun packTiles(tiles: List<CcShortcut>, gridCols: Int = 4): List<TilePosition> {
    val occupied = mutableSetOf<Pair<Int, Int>>()
    val positions = mutableListOf<TilePosition>()
    for (tile in tiles) {
        val cs = tile.colSpan()
        val rs = tile.rowSpan()
        val startRow = if (rs > 1) 1 else 0
        var placed = false
        outer@ for (r in startRow..200) {
            for (c in 0..(gridCols - cs)) {
                val fits = (0 until rs).all { dr ->
                    (0 until cs).all { dc -> Pair(c + dc, r + dr) !in occupied }
                }
                if (fits) {
                    positions += TilePosition(c, r, cs, rs)
                    for (dr in 0 until rs) for (dc in 0 until cs) occupied += Pair(c + dc, r + dr)
                    placed = true; break@outer
                }
            }
        }
        if (!placed) {
            val fallbackRow = (occupied.maxOfOrNull { it.second } ?: 0) + 1
            positions += TilePosition(0, fallbackRow, cs, rs)
        }
    }
    return positions
}

fun defaultCcShortcuts(): List<CcShortcut> = listOf(
    CcShortcut("ROTATION", label = "Rotation"),
    CcShortcut("FOCUS",    label = "Focus"),
    CcShortcut("BRIGHTNESS", label = "Luminosité"),
    CcShortcut("VOLUME",   label = "Volume"),
    CcShortcut("TORCH",    label = "Lampe"),
    CcShortcut("TIMER",    label = "Minuteur"),
    CcShortcut("CALCULATOR", label = "Calculatrice"),
    CcShortcut("CAMERA",   label = "Photo"),
)

fun loadCcShortcuts(context: Context): List<CcShortcut> = runCatching {
    val json = context.getSharedPreferences(CC_PREFS_NAME, Context.MODE_PRIVATE)
        .getString(CC_PREFS_KEY, null) ?: return defaultCcShortcuts()
    val arr = JSONArray(json)
    (0 until arr.length()).map {
        val obj = arr.getJSONObject(it)
        CcShortcut(
            type = obj.getString("type"),
            pkg = obj.optString("pkg", "").ifEmpty { null },
            label = obj.optString("label", ""),
            extra = obj.optString("extra", "").ifEmpty { null }
        )
    }
}.getOrElse { defaultCcShortcuts() }

fun saveCcShortcuts(context: Context, list: List<CcShortcut>) {
    val arr = JSONArray()
    list.forEach { s ->
        arr.put(JSONObject().apply {
            put("type", s.type)
            if (s.pkg != null) put("pkg", s.pkg)
            put("label", s.label)
            if (s.extra != null) put("extra", s.extra)
        })
    }
    context.getSharedPreferences(CC_PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putString(CC_PREFS_KEY, arr.toString()).apply()
}
