package com.lbs.schoolhelper.data.profile

import com.lbs.schoolhelper.data.repository.StudentInfo
import kotlinx.coroutines.flow.StateFlow

interface StudentProfileRepository {
    val profiles: StateFlow<List<StudentProfile>>
    val activeProfile: StateFlow<StudentProfile?>

    fun getProfile(id: String): StudentProfile?
    fun addProfile(displayName: String, studentInfo: StudentInfo): ProfileMutationResult<StudentProfile>
    fun updateProfile(profile: StudentProfile): ProfileMutationResult<Unit>
    fun selectProfile(id: String): ProfileMutationResult<Unit>
    fun deleteProfile(id: String): ProfileMutationResult<Unit>
    fun requiresInitialSetup(): Boolean
}

sealed interface ProfileMutationResult<out T> {
    data class Success<T>(val value: T) : ProfileMutationResult<T>
    data class Failure(val reason: ProfileMutationFailure) : ProfileMutationResult<Nothing>
}

enum class ProfileMutationFailure {
    BLANK_NAME,
    NAME_TOO_LONG,
    DUPLICATE_NAME,
    INCOMPLETE_STUDENT_INFO,
    PROFILE_NOT_FOUND,
    LAST_PROFILE,
    PERSISTENCE_FAILED
}
