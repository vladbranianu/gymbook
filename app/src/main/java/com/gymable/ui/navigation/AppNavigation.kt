package com.gymable.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gymable.ui.screens.AddExerciseScreen
import com.gymable.ui.screens.ColorSettingsScreen
import com.gymable.ui.screens.ContactScreen
import com.gymable.ui.screens.DonationScreen
import com.gymable.ui.screens.EditDataScreen
import com.gymable.ui.screens.ExercisesScreen
import com.gymable.ui.screens.MainScreen
import com.gymable.ui.screens.NewWorkoutScreen
import com.gymable.ui.screens.SettingsScreen
import com.gymable.ui.screens.SplashScreen
import com.gymable.ui.screens.StatsScreen
import com.gymable.ui.screens.WorkoutHistoryScreen
import com.gymable.ui.screens.WorkoutSummaryScreen
import com.gymable.ui.theme.ThemeViewModel
import com.gymable.ui.workout.WorkoutViewModel

object Routes {
    const val SPLASH                    = "splash"
    const val MAIN                      = "main"
    const val NEW_WORKOUT               = "new_workout"
    const val ADD_EXERCISE              = "add_exercise"
    const val ADD_EXERCISE_FROM_WORKOUT = "add_exercise_from_workout"
    const val EXERCISES                 = "exercises"
    const val STATS                     = "stats"
    const val WORKOUT_SUMMARY           = "workout_summary"
    const val HISTORY                   = "history"
    const val SETTINGS                  = "settings"
    const val COLOR_SETTINGS            = "color_settings"
    const val EDIT_DATA                 = "edit_data"
    const val DONATION                  = "donation"
    const val CONTACT                   = "contact"
}

private const val ANIM_MS = 280

@Composable
fun AppNavigation(themeViewModel: ThemeViewModel) {
    val workoutViewModel: WorkoutViewModel = hiltViewModel()
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = {
            slideInHorizontally(tween(ANIM_MS)) { it / 4 } + fadeIn(tween(ANIM_MS))
        },
        exitTransition = {
            slideOutHorizontally(tween(ANIM_MS)) { -it / 6 } + fadeOut(tween(ANIM_MS))
        },
        popEnterTransition = {
            slideInHorizontally(tween(ANIM_MS)) { -it / 6 } + fadeIn(tween(ANIM_MS))
        },
        popExitTransition = {
            slideOutHorizontally(tween(ANIM_MS)) { it / 4 } + fadeOut(tween(ANIM_MS))
        }
    ) {
        composable(
            route = Routes.SPLASH,
            enterTransition = { EnterTransition.None },
            exitTransition  = { fadeOut(tween(1)) }
        ) {
            SplashScreen(
                onReady = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.MAIN,
            enterTransition = {
                if (initialState.destination.route == Routes.SPLASH) EnterTransition.None
                else slideInHorizontally(tween(ANIM_MS)) { it / 4 } + fadeIn(tween(ANIM_MS))
            }
        ) {
            val workoutState by workoutViewModel.uiState.collectAsStateWithLifecycle()
            MainScreen(
                onNewWorkout      = { navController.navigate(Routes.NEW_WORKOUT) },
                onContinueWorkout = { navController.navigate(Routes.NEW_WORKOUT) },
                isWorkoutActive   = workoutState.sessionLog.isNotEmpty() || workoutState.draftGymId != null,
                onAddExercise     = { navController.navigate(Routes.ADD_EXERCISE) },
                onMyExercises     = { navController.navigate(Routes.EXERCISES) },
                onStats           = { navController.navigate(Routes.STATS) },
                onHistory         = { navController.navigate(Routes.HISTORY) },
                onSettings        = { navController.navigate(Routes.SETTINGS) },
                onSupportDev      = { navController.navigate(Routes.DONATION) }
            )
        }

        composable(Routes.NEW_WORKOUT) {
            NewWorkoutScreen(
                onEndWorkout    = { navController.navigate(Routes.WORKOUT_SUMMARY) },
                onBack          = { navController.popBackStack() },
                onAddExercise   = { navController.navigate(Routes.ADD_EXERCISE_FROM_WORKOUT) },
                workoutViewModel = workoutViewModel
            )
        }

        composable(Routes.WORKOUT_SUMMARY) {
            WorkoutSummaryScreen(
                onConfirm = {
                    val showNudge = workoutViewModel.uiState.value.showDonationNudge
                    workoutViewModel.resetWorkout()
                    if (showNudge) {
                        navController.navigate(Routes.DONATION) {
                            popUpTo(Routes.MAIN) { inclusive = false }
                        }
                    } else {
                        navController.popBackStack(Routes.MAIN, inclusive = false)
                    }
                },
                viewModel = workoutViewModel
            )
        }

        composable(Routes.ADD_EXERCISE) {
            AddExerciseScreen(
                onClose = { navController.popBackStack(Routes.MAIN, inclusive = false) }
            )
        }

        composable(Routes.ADD_EXERCISE_FROM_WORKOUT) {
            AddExerciseScreen(
                onClose = { navController.popBackStack() }
            )
        }

        composable(Routes.EXERCISES) {
            ExercisesScreen(
                onBack = { navController.popBackStack(Routes.MAIN, inclusive = false) }
            )
        }

        composable(Routes.STATS) {
            StatsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.HISTORY) {
            WorkoutHistoryScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onColor      = { navController.navigate(Routes.COLOR_SETTINGS) },
                onSupportDev = { navController.navigate(Routes.DONATION) },
                onContact    = { navController.navigate(Routes.CONTACT) },
                onEditData   = { navController.navigate(Routes.EDIT_DATA) },
                onBack       = { navController.popBackStack() }
            )
        }

        composable(Routes.EDIT_DATA) {
            EditDataScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.COLOR_SETTINGS) {
            ColorSettingsScreen(
                themeViewModel = themeViewModel,
                onBack         = { navController.popBackStack() },
                onDonation     = { navController.navigate(Routes.DONATION) }
            )
        }

        composable(Routes.DONATION) {
            DonationScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.CONTACT) {
            ContactScreen(onBack = { navController.popBackStack() })
        }
    }
}
