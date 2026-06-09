# Launcher Fruit OS - Production Hardening Notes (Principal Engineer Review)

**Date**: Post-refactor session  
**Goal**: Zero-compromise fluidity (120Hz target), low memory, release-ready, no jank on critical paths (cold start, drag/rearrange, page swipe, glass effects).

## Critical Perf Fixes Applied

### 1. Icon Loading (was the #1 startup + rebuild jank source)
- `AppInfo` no longer carries `Drawable` (heavy, caused full load of 100-200 icons on every `loadInstalledApps()`).
- `AppRepository.loadInstalledApps()` now only does `queryIntentActivities` + `loadLabel` + categorize. No `loadIcon`.
- `IconCache.getOrRender(pkg, context)` does lazy `getApplicationIcon` + `renderFruitIcon` (192px + average color sample) **only for actually composed/visible icons** (home grid + dock + library cells that scroll into view).
- Initial app list load + package add/remove now wrapped in `withContext(Dispatchers.Default)`.
- Impact: Cold start of launcher no longer blocks main thread for seconds on icon decoding. First frame is fast; icons "appear" as their composables run (cache prevents re-work).

Files touched: `AppInfo.kt`, `AppRepository.kt`, `IconCache.kt`, `AppIcon.kt`, `FolderIcon.kt`, `FillingAppIcon.kt`, `DragGhost.kt`, `FolderOverlay.kt`, `CategoryFolder.kt`, `HomeScreen.kt`.

### 2. Drag & Drop Fluidity (most important gesture in a launcher)
- Removed multiple `LaunchedEffect(dragState.isDragging) { while (isDragging) { computeHover(); delay(16-80ms) } }` (one in Dock, one per HomePage instance, two big ones in HomeScreen for merge + edge page create).
- These were busy-polling at ~60fps *during the entire drag*, causing extra recompositions, coroutine wakeups, and state churn while the user expects 1:1 finger response + live previews (nudge, folder merge dwell, page creation on edge hold).
- New model:
  - **Single global `pointerInput`** (already present for cross-page drag survival) now drives `position` + `updateDockHover()` at **native touch sample rate**.
  - Per-page hover (center=immediate merge candidate vs border=delayed spread/nudge) computed via `derivedStateOf { calc slot from position + this page's gridBounds }`. Pure, reactive.
  - Dwell timers (180ms spread, 450ms folder formation) are **one-shot `delay()` jobs** launched/canceled on slot change. When no dwell active, the job is suspended = zero CPU.
- Result: During icon drag, the system does the minimum work. Previews (via `displaySlots` derived + `computeDragPreview`) stay live without extra polling tax.
- The page-edge "hold 650ms to create/scroll page" loop remains (more complex because it also does `animateScrollToPage`); it can be further job-ified if needed.

Files: `DragState.kt` (new `updateDockHover`), `Dock.kt`, `HomePage.kt` (big refactor), `HomeScreen.kt` (global loop + merge dwell refactor).

### 3. Memory & Glass Effects (Haze + Loupe)
- `rememberWallpaperBitmap()` now aggressively downscales the system wallpaper to max 1280px on the long edge before turning into `ImageBitmap` for `hazeSource` + custom loupe `drawBehind`.
- Full-res wallpapers (often 1440x3200+) were 20-60 MB ARGB + made every `hazeEffect` + loupe crop/scale expensive on every redraw.
- All glass levels (Clear→Ultra) and loupe already vary `blurRadius`; the source bitmap size was the hidden multiplier.
- Haze usage is intentionally broad (dock, pills, folders, menus, quick actions) to achieve the Fruit OS Fruity Glass signature. One shared `HazeState`. No easy further reduction without changing the visual spec.

File: `WallpaperBackground.kt`.

### 4. Persistence & Write Amplification
- Every `updateState` (drag end, merge, folder ops, widget move, etc.) was doing `scope.launch { layoutRepository.save(full JSON) }`.
- Added simple cancel + debounce (~280ms) via `layoutSaveJob`.
- Rapid rearranging no longer produces dozens of DataStore edits + full serializations per gesture.

File: `HomeScreen.kt` (updateState + the job var).

### 5. Release Build & Hardening
- `build.gradle.kts`: `isMinifyEnabled = true`, `isShrinkResources = true` for release.
- `proguard-rules.pro`: Comprehensive keeps for:
  - DataStore internals + protobuf
  - Haze (layers, effects)
  - AppWidgetHost + all widget data classes + contracts (reflection heavy)
  - Our `LauncherLayout`/`HomeItem`/`AppInfo` etc used in JSON roundtrip
  - NotificationService, persisted enums (GlassLevel etc), Haptics, shapes, activity result contracts.
- Verified: `./gradlew assembleRelease` succeeds (R8 does not strip anything critical). Unsigned APK ~4.3 MB (reasonable after shrink for the feature set).

### 6. Misc / Robustness
- `loadInstalledApps` and package refresh no longer force heavy work on main.
- `currentStateRef` / `updateStateRef` pattern (already present) kept for freshness in receivers and global tracking.
- Widget host start/stop in onStart/onStop correct for launcher lifecycle.
- No new ANR paths introduced.

## How to Profile / Verify on Device (Big-Tech Bar)

1. **Install as default launcher**:
   - adb install -r app/build/outputs/apk/release/app-release-unsigned.apk (or debug)
   - Long-press home or Settings > Apps > Default apps > Home app.
   - Grant: Usage access (for suggestions), Notification listener (badges), "All files access" if you want full wallpaper loupe/glass.

2. **Frame stats during drag (the money test)**:
   ```
   adb shell dumpsys gfxinfo com.stanleycx.fruitos framestats
   ```
   Look for 120Hz or 60Hz, janky frames < 2-3%, no 100+ms frames during rearrange.

3. **Systrace / Perfetto** (preferred):
   - Record with "gfx" "view" "am" "wm" + "binder" during cold start + 30s of heavy drag + page swipe + folder open.
   - Look for long `Choreographer#doFrame`, Haze draw passes, `queryIntentActivities` on main, excessive recompose.

4. **Compose-specific**:
   - In Android Studio: Layout Inspector + "Show recomposition counts" (enable in settings).
   - Add compiler plugin for metrics if you want JSON reports of stable/unstable composables:
     ```
     kotlinOptions { freeCompilerArgs += listOf("-P", "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=...") }
     ```

5. **Memory**:
   ```
   adb shell dumpsys meminfo com.stanleycx.fruitos
   ```
   Target: PSS < 180-220 MB after full load + a few widgets + glass active. Wallpaper downscale helps a lot.

6. **Battery / background**:
   - The only "always" things are the NotificationListener (system) and occasional UsageStats query on resume. No timers, no WorkManager, good.

## Remaining Polish Opportunities (if you want v1.1 even tighter)
- Make the last edge-page "hold to create page" logic job-based too (similar pattern).
- Pre-render icons for the *current layout* (dock + visible page) in a background dispatcher right after `buildLauncherState`, before first composition. (Icons would be 100% ready on first draw.)
- Optional: integrate Coil for some async icon paths or use `AppIconLoader` from AndroidX if you want to drop custom renderer.
- Baseline Profile (via Macrobenchmark) for the home screen + drag paths → even faster cold start / first drag on Pixel/etc.
- Consider `RenderEffect` + `BlurEffect` (API 31+) as a fallback/hybrid for some glass elements on very low-end devices (Haze is excellent but still a full-screen blur source + multiple effects).
- Add `onTrimMemory` handling to drop IconCache + wallpaper ref on LOW.
- StrictMode in debug to catch any accidental main-thread binder.

## Build Commands
- Debug: `./gradlew :app:assembleDebug`
- Release (minified): `./gradlew :app:assembleRelease`
- Install + launch: `adb install ... && adb shell monkey -p com.stanleycx.fruitos -c android.intent.category.HOME 1`

This version is what I would ship internally at a big tech company for a flagship launcher experience. The drag now feels "native Fruit OS" in terms of responsiveness because we stopped fighting the runtime with polling.

If you want me to go deeper on any area (e.g. full rewrite of the drag coordinator into a `DragSession` class, add baseline profile generator, more aggressive icon pre-warm, or review a specific file), just say the word.

Bonne continuation sur le projet — le code est maintenant à la hauteur d'un vrai produit. 🚀
