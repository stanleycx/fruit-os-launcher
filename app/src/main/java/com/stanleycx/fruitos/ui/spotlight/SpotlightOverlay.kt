package com.stanleycx.fruitos.ui.spotlight

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stanleycx.fruitos.data.AppInfo
import com.stanleycx.fruitos.data.AppRepository
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import com.stanleycx.fruitos.ui.components.GlassLevel
import com.stanleycx.fruitos.ui.components.GlassTint
import com.stanleycx.fruitos.ui.components.backgroundBlurFor
import com.stanleycx.fruitos.ui.components.glass
import com.stanleycx.fruitos.ui.components.LoupeLevel
import com.stanleycx.fruitos.ui.components.GlossLevel
import androidx.compose.foundation.layout.width

/**
 * Spotlight : recherche d'apps style Fruit OS (barre pilule + suggestions + résultats live)
 */
@Composable
fun SpotlightOverlay(
    allApps: List<AppInfo>,
    suggestedApps: List<AppInfo>,
    state: SpotlightState,
    appRepository: AppRepository,
    hazeState: HazeState,
    glassLevel: GlassLevel = GlassLevel.Thick,
    glassTint: GlassTint = GlassTint.None,
    customTintColor: Color? = null,
    onRequestUsagePermission: () -> Unit,
    hasUsagePermission: Boolean,
    loupeLevel: LoupeLevel = LoupeLevel.None,
    glossLevel: GlossLevel = GlossLevel.None,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var showMore by remember { mutableStateOf(false) }

    LaunchedEffect(state.isOpen) {
        if (state.isOpen) {
            kotlinx.coroutines.delay(100)
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
            showMore = false
        }
    }

    val isSearching = state.query.isNotBlank()
    val searchResults = if (isSearching) {
        allApps.filter { it.label.contains(state.query, ignoreCase = true) }
    } else emptyList()

    AnimatedVisibility(
        visible = state.isOpen,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Fond : blur pur + assombrissement léger, SANS teinte glass
                .hazeEffect(state = hazeState) {
                    blurRadius = backgroundBlurFor(glassLevel)
                    tints = emptyList()
                    noiseFactor = 0.01f
                }
                .background(Color.Black.copy(alpha = 0.28f))   // assombrissement neutre, sans teinte couleur
                .pointerInput(Unit) { detectTapGestures(onTap = { state.close() }) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .imePadding()
            ) {
                Spacer(modifier = Modifier.height(80.dp))

                if (isSearching && searchResults.isNotEmpty()) {
                    SearchResultsSection(
                        results = searchResults,
                        onAppClick = { app ->
                            appRepository.launchApp(app.packageName)
                            state.close()
                        },
                        hazeState = hazeState,
                        glassLevel = glassLevel,
                        glassTint = glassTint,
                        customTintColor = customTintColor,
                        loupeLevel = loupeLevel, glossLevel = glossLevel
                    )
                } else if (!isSearching) {
                    if (!hasUsagePermission) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .pointerInput(Unit) { detectTapGestures(onTap = { onRequestUsagePermission() }) }
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Activer les suggestions d'apps\n(autoriser l'accès aux données d'usage)",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else if (suggestedApps.isNotEmpty()) {
                        SuggestionsSection(
                            apps = suggestedApps,
                            showMore = showMore,
                            onToggleShowMore = { showMore = !showMore },
                            onAppClick = { app ->
                                appRepository.launchApp(app.packageName)
                                state.close()
                            },
                            hazeState = hazeState,
                            glassLevel = glassLevel,
                            glassTint = glassTint,
                            customTintColor = customTintColor,
                            loupeLevel = loupeLevel, glossLevel = glossLevel
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                SearchBar(
                    query = state.query,
                    onQueryChange = { state.query = it },
                    focusRequester = focusRequester,
                    hazeState = hazeState,
                    glassLevel = glassLevel,
                    glassTint = glassTint,
                    customTintColor = customTintColor,
                    loupeLevel = loupeLevel, glossLevel = glossLevel
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

// ==================== SECTIONS (fix du onToggleShowMore ici) ====================

/** Titre commun (hauteur fixe pour éviter tout saut visuel) */
@Composable
private fun SectionTitle(
    title: String,
    showMoreButton: Boolean = false,
    showMore: Boolean = false,
    onToggleShowMore: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, bottom = 10.dp)
            .height(24.dp),           // hauteur fixe = plus jamais de saut
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )

        if (showMoreButton) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .pointerInput(Unit) { detectTapGestures(onTap = { onToggleShowMore() }) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (showMore) "Afficher moins" else "Afficher plus",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            // Espace vide de la même largeur que le bouton → pas de saut
            Spacer(modifier = Modifier.width(80.dp))
        }
    }
}

/** Résultats de recherche : toujours 4 apps max, sans bouton "Afficher plus" */
@Composable
private fun SearchResultsSection(
    results: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    hazeState: HazeState,
    glassLevel: GlassLevel,
    glassTint: GlassTint,
    customTintColor: Color? = null,
    loupeLevel: LoupeLevel = LoupeLevel.None,
    glossLevel: GlossLevel = GlossLevel.None
) {
    val displayed = results.take(4)

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(title = "Résultats")   // ← même titre que les suggestions

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .glass(
                    hazeState = hazeState,
                    level = glassLevel,
                    glassTint = glassTint,
                    customTintColor = customTintColor,
                    shape = RoundedCornerShape(24.dp),
                    loupeLevel = loupeLevel, glossLevel = glossLevel
                )
                .padding(16.dp)
        ) {
            AppIconGrid(apps = displayed, onAppClick = onAppClick, transparent = true)
        }
    }
}

/** Suggestions (garde le bouton "Afficher plus") */
@Composable
private fun SuggestionsSection(
    apps: List<AppInfo>,
    showMore: Boolean,
    onToggleShowMore: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    hazeState: HazeState,
    glassLevel: GlassLevel,
    glassTint: GlassTint,
    customTintColor: Color? = null,
    loupeLevel: LoupeLevel = LoupeLevel.None,
    glossLevel: GlossLevel = GlossLevel.None
) {
    val displayed = if (showMore) apps.take(8) else apps.take(4)

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(
            title = "Suggestions",
            showMoreButton = true,
            showMore = showMore,
            onToggleShowMore = onToggleShowMore
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .glass(
                    hazeState = hazeState,
                    level = glassLevel,
                    glassTint = glassTint,
                    customTintColor = customTintColor,
                    shape = RoundedCornerShape(24.dp),
                    loupeLevel = loupeLevel, glossLevel = glossLevel
                )
                .padding(16.dp)
        ) {
            AppIconGrid(apps = displayed, onAppClick = onAppClick, transparent = true)
        }
    }
}

// ==================== GRILLE (déjà corrigée précédemment) ====================

@Composable
private fun AppIconGrid(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    transparent: Boolean = false,
    hazeState: HazeState? = null,
    glassLevel: GlassLevel = GlassLevel.Regular,
    glassTint: GlassTint = GlassTint.None,
    customTintColor: Color? = null
) {
    val columns = 4
    val rows = apps.chunked(columns)

    val content: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            for (row in rows) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (app in row) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            com.stanleycx.fruitos.ui.components.AppIcon(
                                app = app,
                                onClick = { onAppClick(app) },
                                onDragStart = { _: Offset -> },
                                showLabel = true,
                                iconSize = 56.dp
                            )
                        }
                    }
                    repeat(columns - row.size) { Box(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }

    if (transparent) content() else {
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) { content() }
    }
}

// ==================== Barre de recherche pilule ====================

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    hazeState: HazeState,
    glassLevel: GlassLevel,
    glassTint: GlassTint,
    customTintColor: Color? = null,
    loupeLevel: LoupeLevel = LoupeLevel.None,
    glossLevel: GlossLevel = GlossLevel.None
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .glass(
                hazeState = hazeState,
                level = glassLevel,
                glassTint = glassTint,
                customTintColor = customTintColor,
                shape = CircleShape,
                loupeLevel = loupeLevel, glossLevel = glossLevel
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
            cursorBrush = SolidColor(Color.White),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text("Recherche", color = Color.White.copy(alpha = 0.6f), fontSize = 17.sp)
                }
                innerTextField()
            }
        )
    }
}