package com.journal.shared.navigation

object Routes {
    const val AUTH = "auth"
    const val TEACHER_HOME = "teacher_home"
    const val TEACHER_JOURNAL = "teacher_journal/{groupId}/{disciplineId}/{periodId}/{lessonId}"
    const val TEACHER_DASHBOARD = "teacher_dashboard"
    const val TEACHER_VED = "teacher_ved"
    const val TEACHER_STUDENT_CARD = "teacher_student_card"

    fun teacherJournal(groupId: String, disciplineId: String, periodId: String, lessonId: String): String =
        "teacher_journal/$groupId/$disciplineId/$periodId/$lessonId"
}
