package com.lbs.schoolhelper.data.profile

import com.lbs.schoolhelper.data.repository.StudentInfo

const val PROFILE_SCHEMA_VERSION = 1
const val MAX_PROFILE_NAME_LENGTH = 10

data class StudentProfile(
    val id: String,
    val displayName: String,
    val studentInfo: StudentInfo
)

data class StudentProfilesData(
    val schemaVersion: Int = PROFILE_SCHEMA_VERSION,
    val activeProfileId: String,
    val profiles: List<StudentProfile>
)
