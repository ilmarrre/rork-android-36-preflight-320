package com.rork.ghostdetectorspiritbox.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rork.ghostdetectorspiritbox.R
import com.rork.ghostdetectorspiritbox.config.Tokens
import com.rork.ghostdetectorspiritbox.data.AppPreferences
import com.rork.ghostdetectorspiritbox.data.RecordRepository
import com.rork.ghostdetectorspiritbox.ui.features.BootstrapScreen
import com.rork.ghostdetectorspiritbox.ui.features.archive.ArchiveScreen
import com.rork.ghostdetectorspiritbox.ui.features.briefing.BriefingScreen
import com.rork.ghostdetectorspiritbox.ui.features.investigate.InvestigateScreen
import com.rork.ghostdetectorspiritbox.ui.features.record.MissingRecordScreen
import com.rork.ghostdetectorspiritbox.ui.features.record.RecordScreen
import com.rork.ghostdetectorspiritbox.ui.features.session.SessionScreen
import com.rork.ghostdetectorspiritbox.ui.features.session.SessionViewModel
import com.rork.ghostdetectorspiritbox.ui.instrument.InstrumentSurface
import com.rork.ghostdetectorspiritbox.ui.theme.Type

private const val ROUTE_BRIEFING = "briefing"
private const val ROUTE_INVESTIGATE = "investigate"
private const val ROUTE_ARCHIVE = "archive"
private const val ROUTE_SESSION = "session"
private const val ROUTE_RECORD = "record/{id}/{fresh}"

private val TAB_ROUTES = setOf(ROUTE_INVESTIGATE, ROUTE_ARCHIVE)

/** Thin route composition. Every screen body lives in its own feature package. */
@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val preferences = remember { AppPreferences.get(context) }
    val repository = remember { RecordRepository.get(context) }
    val sessionViewModel: SessionViewModel = viewModel()

    val archiveReady by repository.isLoaded.collectAsStateWithLifecycle()
    val briefingComplete by preferences.briefingComplete.collectAsStateWithLifecycle()
    val state by sessionViewModel.state.collectAsStateWithLifecycle()
    val records by repository.records.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showTabs = currentRoute in TAB_ROUTES

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> sessionViewModel.onAppBackgrounded()
                Lifecycle.Event.ON_START -> sessionViewModel.onAppForegrounded()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.finishedRecordId) {
        val id = state.finishedRecordId ?: return@LaunchedEffect
        navController.navigate("record/$id/true") { popUpTo(ROUTE_INVESTIGATE) }
        sessionViewModel.consumeFinishedRecord()
    }

    if (!archiveReady) {
        BootstrapScreen()
        return
    }

    Scaffold(
        containerColor = Tokens.case,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (showTabs) {
                InstrumentTabBar(
                    currentRoute = currentRoute,
                    onSelect = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        InstrumentSurface {
            NavHost(
                navController = navController,
                startDestination = if (briefingComplete) ROUTE_INVESTIGATE else ROUTE_BRIEFING,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(ROUTE_BRIEFING) {
                    BriefingScreen(
                        sensorStatus = state.sensorStatus,
                        onComplete = {
                            preferences.completeBriefing()
                            navController.navigate(ROUTE_INVESTIGATE) {
                                popUpTo(ROUTE_BRIEFING) { inclusive = true }
                            }
                        }
                    )
                }

                composable(ROUTE_INVESTIGATE) {
                    InvestigateScreen(
                        state = state,
                        onModeChange = sessionViewModel::setMode,
                        onStart = {
                            sessionViewModel.startSession()
                            navController.navigate(ROUTE_SESSION)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                composable(ROUTE_ARCHIVE) {
                    ArchiveScreen(
                        records = records,
                        capacity = state.archiveCapacity,
                        onOpen = { id -> navController.navigate("record/$id/false") },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                composable(ROUTE_SESSION) {
                    SessionScreen(
                        state = state,
                        onModeChange = sessionViewModel::setMode,
                        onAsk = sessionViewModel::askQuestion,
                        onMark = sessionViewModel::markMoment,
                        onStop = { sessionViewModel.endSession() }
                    )
                }

                composable(
                    route = ROUTE_RECORD,
                    arguments = listOf(
                        navArgument("id") { type = NavType.StringType },
                        navArgument("fresh") { type = NavType.BoolType }
                    )
                ) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    val fresh = entry.arguments?.getBoolean("fresh") ?: false
                    val record = records.firstOrNull { it.id == id }
                    if (record == null) {
                        MissingRecordScreen(onBack = { navController.popBackStack() })
                    } else {
                        RecordScreen(
                            record = record,
                            justFinished = fresh,
                            endedByTimeLimit = state.endedByTimeLimit,
                            onBack = {
                                if (!navController.popBackStack()) {
                                    navController.navigate(ROUTE_INVESTIGATE)
                                }
                            },
                            onNewSession = {
                                sessionViewModel.startSession()
                                navController.navigate(ROUTE_SESSION) {
                                    popUpTo(ROUTE_INVESTIGATE)
                                }
                            },
                            onOpenArchive = {
                                navController.navigate(ROUTE_ARCHIVE) {
                                    popUpTo(ROUTE_INVESTIGATE)
                                }
                            },
                            onDelete = {
                                repository.delete(id)
                                if (!navController.popBackStack()) {
                                    navController.navigate(ROUTE_INVESTIGATE)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstrumentTabBar(currentRoute: String?, onSelect: (String) -> Unit) {
    NavigationBar(
        containerColor = Tokens.ink,
        contentColor = Tokens.bone
    ) {
        val itemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = Tokens.phosphor,
            selectedTextColor = Tokens.phosphor,
            unselectedIconColor = Tokens.boneMute,
            unselectedTextColor = Tokens.boneMute,
            indicatorColor = Tokens.caseEdge
        )
        NavigationBarItem(
            selected = currentRoute == ROUTE_INVESTIGATE,
            onClick = { onSelect(ROUTE_INVESTIGATE) },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.TrackChanges,
                    contentDescription = stringResource(R.string.a11y_tab_investigate)
                )
            },
            label = { Text(text = stringResource(R.string.tab_investigate), style = Type.labelSmall) },
            colors = itemColors
        )
        NavigationBarItem(
            selected = currentRoute == ROUTE_ARCHIVE,
            onClick = { onSelect(ROUTE_ARCHIVE) },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = stringResource(R.string.a11y_tab_archive)
                )
            },
            label = { Text(text = stringResource(R.string.tab_archive), style = Type.labelSmall) },
            colors = itemColors
        )
    }
}
