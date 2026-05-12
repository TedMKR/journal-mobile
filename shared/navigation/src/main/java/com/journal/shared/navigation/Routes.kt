package com.journal.shared.navigation

object Routes {
    const val AUTH = "auth"
    const val TEACHER_HOME = "teacher_home"
    const val TEACHER_DASHBOARD = "teacher_dashboard"
    const val TEACHER_JOURNAL = "teacher_journal/{groupId}/{disciplineId}/{periodId}/{lessonType}"
    const val TEACHER_STUDENT_CARD = "teacher_student_card/{groupId}/{disciplineId}/{periodId}/{studentId}"
    const val METHODIST_JOURNALS = "methodist_journals"
    const val METHODIST_TEMPLATES = "methodist_templates"
    const val METHODIST_JOURNAL_CREATE = "methodist_journal_create"

    fun teacherJournal(groupId: String, disciplineId: String, periodId: String, lessonType: String): String =
        "teacher_journal/$groupId/$disciplineId/$periodId/$lessonType"

    fun teacherStudentCard(groupId: String, disciplineId: String, periodId: String, studentId: String): String =
        "teacher_student_card/$groupId/$disciplineId/$periodId/$studentId"
}
