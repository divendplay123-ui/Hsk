package com.example

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainActivitySmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainActivityLaunchesAndExercisesCoreFlows() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("draw_canvas").assertExists()

        // Tap manual stroke and open candidate detail sheet with animation
        composeRule.onNodeWithTag("manual_stroke_1").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("candidates_row").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("candidates_row").performScrollTo()
        composeRule.onNodeWithTag("candidate_card_一").performClick()
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            composeRule.onAllNodesWithTag("detail_animated_hanzi").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("detail_animated_hanzi").assertExists()
        composeRule.onNodeWithTag("detail_close_button").performClick()
        composeRule.waitForIdle()

        // Navigate to Study tab ("Учить") and exercise Flashcards, Quiz, and Write
        composeRule.onNodeWithTag("nav_study").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("flashcard_surface").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("flashcard_know_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("study_mode_quiz").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("quiz_option_0").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("quiz_next_button").performScrollTo().assertExists()

        composeRule.onNodeWithTag("study_mode_write").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("write_practice_canvas").assertExists()
        composeRule.onNodeWithTag("write_hint_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Navigate to Dictionary tab ("Словарь")
        composeRule.onNodeWithTag("nav_dictionary").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dictionary_search_input").assertExists()

        // Navigate to Settings tab ("Настройки"), switch HSK 2.0 <-> HSK 3.0, and toggle HSK2
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("standard_hsk20_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("standard_hsk30_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hsk2_switch").performScrollTo().performClick()
        composeRule.waitForIdle()
    }
}
