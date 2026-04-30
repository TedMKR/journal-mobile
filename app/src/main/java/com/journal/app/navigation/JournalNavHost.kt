package com.journal.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.journal.core.network.api.JournalApi
import com.journal.features.auth.AuthRoute
import com.journal.features.teacher.dashboard.TeacherDashboardRoute
import com.journal.features.teacher.home.TeacherHomeRoute
import com.journal.features.teacher.journal.TeacherJournalRoute
import com.journal.features.teacher.studentcard.TeacherStudentCardRoute
import com.journal.features.teacher.ved.TeacherVedRoute
import com.journal.shared.navigation.Routes

@Composable
fun JournalNavHost(journalApi: JournalApi) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.AUTH) {
        composable(Routes.AUTH) {
            AuthRoute(onContinue = { navController.navigate(Routes.TEACHER_HOME) })
        }
        composable(Routes.TEACHER_HOME) {
            TeacherHomeRoute(
                onOpenLesson = { lesson ->
                    val groupId = lesson.groupId.orEmpty()
                    val disciplineId = lesson.disciplineId.orEmpty()
                    val periodId = lesson.periodId.orEmpty()
                    if (groupId.isNotBlank() && disciplineId.isNotBlank() && periodId.isNotBlank()) {
                        navController.navigate(Routes.teacherJournal(groupId, disciplineId, periodId))
                    }
                },
                onOpenDashboard = { navController.navigate(Routes.TEACHER_DASHBOARD) },
                onOpenVed = { navController.navigate(Routes.TEACHER_VED) }
            )
        }
        composable(
            route = Routes.TEACHER_JOURNAL,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
                navArgument("disciplineId") { type = NavType.StringType },
                navArgument("periodId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            TeacherJournalRoute(
                groupId = backStackEntry.arguments?.getString("groupId").orEmpty(),
                disciplineId = backStackEntry.arguments?.getString("disciplineId").orEmpty(),
                periodId = backStackEntry.arguments?.getString("periodId").orEmpty(),
                journalApi = journalApi,
                onOpenStudentCard = { navController.navigate(Routes.TEACHER_STUDENT_CARD) }
            )
        }
        composable(Routes.TEACHER_DASHBOARD) {
            TeacherDashboardRoute(onBack = { navController.popBackStack() })
        }
        composable(Routes.TEACHER_VED) {
            TeacherVedRoute(onBack = { navController.popBackStack() })
        }
        composable(Routes.TEACHER_STUDENT_CARD) {
            TeacherStudentCardRoute(onBack = { navController.popBackStack() })
        }
    }
}
