package com.lbs.schoolhelper.ui.common

import com.lbs.schoolhelper.data.model.SchoolInfo
import com.lbs.schoolhelper.data.repository.StudentInfo

internal fun StudentInfo.mergeInputDraft(
    schoolQuery: String,
    selectedSchool: SchoolInfo?,
    grade: String,
    classroom: String
): StudentInfo {
    val confirmedSchool = selectedSchool?.takeIf {
        it.schoolName == schoolQuery.trim()
    }
    return copy(
        grade = grade.trim(),
        classroom = classroom.trim(),
        schoolName = confirmedSchool?.schoolName ?: schoolName,
        officeCode = confirmedSchool?.officeCode ?: officeCode,
        schoolCode = confirmedSchool?.schoolCode ?: schoolCode,
        schoolKind = confirmedSchool?.schoolKind ?: schoolKind
    )
}
