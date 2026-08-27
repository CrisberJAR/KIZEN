package com.kizen.tasks.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kizen.tasks.ui.habit.HabitEditorScreen
import com.kizen.tasks.ui.home.HomeScreen
import com.kizen.tasks.ui.list.ListScreen
import com.kizen.tasks.ui.lists.ListsScreen
import com.kizen.tasks.ui.nudge.NudgeEditorScreen
import com.kizen.tasks.ui.settings.SettingsScreen
import com.kizen.tasks.ui.task.TaskEditorScreen

@Composable
fun KizenNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onOpenList = { nav.navigate("list/$it") },
                onOpenTask = { nav.navigate("editor?taskId=$it") },
                onCreateTask = { nav.navigate("editor") },
                onManageLists = { nav.navigate("lists") },
                onOpenSettings = { nav.navigate("settings") },
                onOpenHabit = { nav.navigate("habit?habitId=$it") },
                onCreateHabit = { nav.navigate("habit") },
                onOpenNudge = { nav.navigate("nudge?nudgeId=$it") },
                onCreateNudge = { nav.navigate("nudge") },
            )
        }
        composable(
            route = "list/{listId}",
            arguments = listOf(navArgument("listId") { type = NavType.StringType }),
        ) {
            ListScreen(
                onBack = { nav.popBackStack() },
                onOpenTask = { nav.navigate("editor?taskId=$it") },
                onCreateTask = { listId -> nav.navigate("editor?listId=$listId") },
            )
        }
        composable(
            route = "editor?taskId={taskId}&listId={listId}",
            arguments = listOf(
                navArgument("taskId") { type = NavType.StringType; defaultValue = "" },
                navArgument("listId") { type = NavType.StringType; defaultValue = "" },
            ),
        ) {
            TaskEditorScreen(onBack = { nav.popBackStack() })
        }
        composable(
            route = "habit?habitId={habitId}",
            arguments = listOf(
                navArgument("habitId") { type = NavType.StringType; defaultValue = "" },
            ),
        ) {
            HabitEditorScreen(onBack = { nav.popBackStack() })
        }
        composable(
            route = "nudge?nudgeId={nudgeId}",
            arguments = listOf(
                navArgument("nudgeId") { type = NavType.StringType; defaultValue = "" },
            ),
        ) {
            NudgeEditorScreen(onBack = { nav.popBackStack() })
        }
        composable("lists") {
            ListsScreen(onBack = { nav.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
