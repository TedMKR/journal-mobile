package com.journal.shared.navigation

object Routes {
    const val AUTH = "auth"
    const val TEACHER_HOME = "teacher_home"
    const val TEACHER_JOURNAL = "teacher_journal/{groupId}/{disciplineId}/{periodId}/{lessonType}"
    const val TEACHER_STUDENT_CARD = "teacher_student_card/{groupId}/{disciplineId}/{periodId}/{studentId}"

    fun teacherJournal(
        groupId: String,
        disciplineId: String,
        periodId: String,
        lessonType: String
    ): String = "teacher_journal/$groupId/$disciplineId/$periodId/$lessonType"

    fun teacherStudentCard(
        groupId: String,
        disciplineId: String,
        periodId: String,
        studentId: String
    ): String = "teacher_student_card/$groupId/$disciplineId/$periodId/$studentId"
}
