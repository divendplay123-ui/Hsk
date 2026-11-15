package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.CharacterDetailSheet
import com.example.ui.screens.DictionaryScreen
import com.example.ui.screens.DrawRecognitionScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudyScreen

private enum class AppDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DRAW("draw", "Черты", Icons.Filled.Brush, Icons.Outlined.Brush, "nav_draw"),
    STUDY("study", "Учить", Icons.Filled.School, Icons.Outlined.School, "nav_study"),
    DICTIONARY("dictionary", "Словарь", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "nav_dictionary"),
    SETTINGS("settings", "Настройки", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
}

@Composable
fun MainScreen(
    viewModel: HanziViewModel = viewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppDestination.DRAW.route

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val drawnStrokes by viewModel.drawnStrokes.collectAsStateWithLifecycle()
    val localCandidates by viewModel.localCandidates.collectAsStateWithLifecycle()
    val selectedEntry by viewModel.selectedEntry.collectAsStateWithLifecycle()
    val selectedWord by viewModel.selectedWord.collectAsStateWithLifecycle()
    val dictionaryQuery by viewModel.dictionaryQuery.collectAsStateWithLifecycle()
    val dictionaryLevelFilter by viewModel.dictionaryLevelFilter.collectAsStateWithLifecycle()
    val dictionaryEntries by viewModel.dictionaryEntries.collectAsStateWithLifecycle()
    val dictionaryWords by viewModel.dictionaryWords.collectAsStateWithLifecycle()
    val dictionaryShowWords by viewModel.dictionaryShowWords.collectAsStateWithLifecycle()

    val studyMode by viewModel.studyMode.collectAsStateWithLifecycle()
    val onlyUnmastered by viewModel.onlyUnmastered.collectAsStateWithLifecycle()
    val flashcardDeckMode by viewModel.flashcardDeckMode.collectAsStateWithLifecycle()
    val flashcardDirection by viewModel.flashcardDirection.collectAsStateWithLifecycle()
    val studyDeck by viewModel.studyDeck.collectAsStateWithLifecycle()
    val flashcardIndex by viewModel.flashcardIndex.collectAsStateWithLifecycle()
    val isFlashcardFlipped by viewModel.isFlashcardFlipped.collectAsStateWithLifecycle()
    val recallState by viewModel.recallState.collectAsStateWithLifecycle()
    val matchState by viewModel.matchState.collectAsStateWithLifecycle()
    val quizState by viewModel.quizState.collectAsStateWithLifecycle()
    val writePracticeState by viewModel.writePracticeState.collectAsStateWithLifecycle()

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        AppDestination.entries.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        AppDestination.entries.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = AppDestination.DRAW.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(AppDestination.DRAW.route) {
                        DrawRecognitionScreen(
                            settings = settings,
                            drawnStrokes = drawnStrokes,
                            localCandidates = localCandidates,
                            onStrokeCompleted = viewModel::addDrawnStroke,
                            onManualStrokeTap = viewModel::addManualStroke,
                            onUndo = viewModel::undoLastStroke,
                            onClear = viewModel::clearCanvas,
                            onSelectCharacter = viewModel::openCharacterDetail,
                            onSelectHskStandard = viewModel::setHskStandard,
                            onOpenSettings = { navigateTo(AppDestination.SETTINGS.route) }
                        )
                    }

                    composable(AppDestination.STUDY.route) {
                        StudyScreen(
                            settings = settings,
                            studyMode = studyMode,
                            onlyUnmastered = onlyUnmastered,
                            flashcardDeckMode = flashcardDeckMode,
                            flashcardDirection = flashcardDirection,
                            studyDeck = studyDeck,
                            flashcardIndex = flashcardIndex,
                            isFlashcardFlipped = isFlashcardFlipped,
                            recallState = recallState,
                            matchState = matchState,
                            quizState = quizState,
                            writePracticeState = writePracticeState,
                            onSelectHskStandard = viewModel::setHskStandard,
                            onSelectStudyMode = viewModel::setStudyMode,
                            onToggleOnlyUnmastered = viewModel::toggleOnlyUnmastered,
                            onSelectDeckMode = viewModel::setFlashcardDeckMode,
                            onToggleDirection = viewModel::setFlashcardDirection,
                            onRefreshSrsSession = viewModel::refreshSrsSession,
                            onFlipFlashcard = viewModel::flipFlashcard,
                            onNextFlashcard = viewModel::nextFlashcard,
                            onPrevFlashcard = viewModel::prevFlashcard,
                            onShuffleFlashcards = viewModel::shuffleFlashcards,
                            onRateFlashcardSrs = viewModel::rateFlashcardSrs,
                            onAnswerToneRecall = viewModel::answerToneRecall,
                            onTapAssembleTile = viewModel::tapAssembleTile,
                            onBackspaceAssemble = viewModel::backspaceAssemble,
                            onClearAssemble = viewModel::clearAssemble,
                            onSubmitAssembleRecall = viewModel::submitAssembleRecall,
                            onNextRecallQuestion = viewModel::nextRecallQuestion,
                            onStartNewRecallRound = viewModel::startNewRecallRound,
                            onSelectMatchLeft = viewModel::selectMatchLeft,
                            onSelectMatchRight = viewModel::selectMatchRight,
                            onStartNewMatchRound = viewModel::startNewMatchRound,
                            onAnswerQuiz = viewModel::answerQuiz,
                            onNextQuizQuestion = viewModel::nextQuizQuestion,
                            onStartNewQuizRound = viewModel::startNewQuizRound,
                            onAddWriteStroke = viewModel::addWritePracticeStroke,
                            onUndoWriteStroke = viewModel::undoWritePracticeStroke,
                            onClearWriteCanvas = viewModel::clearWritePracticeCanvas,
                            onCheckWritePractice = viewModel::checkWritePractice,
                            onToggleWriteHint = viewModel::toggleWriteHint,
                            onSelectWriteCharIndex = viewModel::selectWritePracticeCharIndex,
                            onNextWriteTarget = viewModel::nextWritePracticeTarget,
                            onOpenCharacterDetail = viewModel::openCharacterDetail,
                            onOpenWordDetail = viewModel::openWordDetail
                        )
                    }

                    composable(AppDestination.DICTIONARY.route) {
                        DictionaryScreen(
                            settings = settings,
                            query = dictionaryQuery,
                            selectedLevelFilter = dictionaryLevelFilter,
                            entries = dictionaryEntries,
                            words = dictionaryWords,
                            showWords = dictionaryShowWords,
                            onSelectHskStandard = viewModel::setHskStandard,
                            onToggleShowWords = viewModel::setDictionaryShowWords,
                            onQueryChange = viewModel::updateDictionaryQuery,
                            onSelectLevelFilter = viewModel::selectDictionaryLevelFilter,
                            onSelectCharacter = viewModel::openCharacterDetail,
                            onSelectWord = viewModel::openWordDetail,
                            onOpenSettings = { navigateTo(AppDestination.SETTINGS.route) }
                        )
                    }

                    composable(AppDestination.SETTINGS.route) {
                        SettingsScreen(
                            settings = settings,
                            onSelectHskStandard = viewModel::setHskStandard,
                            onToggleHsk2 = viewModel::setIncludeHsk2,
                            onToggleHsk3 = viewModel::setIncludeHsk3,
                            onToggleHsk4 = viewModel::setIncludeHsk4,
                            onToggleUni = viewModel::setIncludeUni,
                            onToggleGridGuide = viewModel::setShowGridGuide,
                            onToggleStrictStrokeCount = viewModel::setStrictStrokeCount
                        )
                    }
                }
            }
        }
    }

    selectedEntry?.let { entry ->
        CharacterDetailSheet(
            entry = entry,
            onDismiss = viewModel::closeCharacterDetail,
            word = selectedWord,
            standard = settings.hskStandard,
            onSelectCharacterInWord = viewModel::selectCharacterInWord,
            onSelectExampleWord = viewModel::openExampleWordDetail
        )
    }
}
