package com.journal.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.journal.features.auth.AuthRoute
import com.journal.features.teacher.dashboard.TeacherDashboardRoute
import com.journal.features.teacher.home.TeacherHomeRoute
import com.journal.features.teacher.journal.TeacherJournalRoute
import com.journal.features.teacher.studentcard.TeacherStudentCardRoute
import com.journal.features.teacher.ved.TeacherVedRoute
import com.journal.shared.navigation.Routes

@Composable
fun JournalNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.AUTH) {
        composable(Routes.AUTH) {
            AuthRoute(onContinue = { navController.navigate(Routes.TEACHER_HOME) })
        }
        composable(Routes.TEACHER_HOME) {
            TeacherHomeRoute(
                onOpenLesson = { navController.navigate(Routes.TEACHER_JOURNAL) },
                onOpenDashboard = { navController.navigate(Routes.TEACHER_DASHBOARD) },
                onOpenVed = { navController.navigate(Routes.TEACHER_VED) }
            )
        }
        composable(Routes.TEACHER_JOURNAL) {
            TeacherJournalRoute(onOpenStudentCard = { navController.navigate(Routes.TEACHER_STUDENT_CARD) })
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
