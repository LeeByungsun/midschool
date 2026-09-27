package com.lbs.schoolhelper.test

import com.lbs.schoolhelper.data.profile.ProfileMutationFailure
import com.lbs.schoolhelper.data.profile.ProfileMutationResult
import com.lbs.schoolhelper.data.profile.MAX_PROFILE_NAME_LENGTH
import com.lbs.schoolhelper.data.profile.StudentProfile
import com.lbs.schoolhelper.data.profile.StudentProfileRepository
import com.lbs.schoolhelper.data.repository.StudentInfo
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeStudentProfileRepository(
    initialProfiles: List<StudentProfile> = emptyList(),
    initialActiveProfileId: String? = initialProfiles.firstOrNull()?.id
) : StudentProfileRepository {
    private val mutableProfiles = MutableStateFlow(initialProfiles)
    override val profiles: StateFlow<List<StudentProfile>> = mutableProfiles.asStateFlow()

    private val mutableActiveProfile = MutableStateFlow(
        initialProfiles.firstOrNull { it.id == initialActiveProfileId }
    )
    override val activeProfile: StateFlow<StudentProfile?> = mutableActiveProfile.asStateFlow()

    val selectedProfileIds = mutableListOf<String>()

    override fun getProfile(id: String): StudentProfile? = profiles.value.firstOrNull { it.id == id }

    override fun addProfile(
        displayName: String,
        studentInfo: StudentInfo
    ): ProfileMutationResult<StudentProfile> {
        val normalizedName = displayName.trim()
        validate(normalizedName, studentInfo)?.let { return ProfileMutationResult.Failure(it) }
        val profile = StudentProfile(UUID.randomUUID().toString(), normalizedName, studentInfo)
        mutableProfiles.value = profiles.value + profile
        mutableActiveProfile.value = profile
        return ProfileMutationResult.Success(profile)
    }

    override fun updateProfile(profile: StudentProfile): ProfileMutationResult<Unit> {
        val index = profiles.value.indexOfFirst { it.id == profile.id }
        if (index < 0) return ProfileMutationResult.Failure(ProfileMutationFailure.PROFILE_NOT_FOUND)
        val normalizedName = profile.displayName.trim()
        validate(normalizedName, profile.studentInfo, profile.id)?.let {
            return ProfileMutationResult.Failure(it)
        }
        val normalizedProfile = profile.copy(displayName = normalizedName)
        mutableProfiles.value = profiles.value.toMutableList().apply { this[index] = normalizedProfile }
        if (activeProfile.value?.id == profile.id) mutableActiveProfile.value = normalizedProfile
        return ProfileMutationResult.Success(Unit)
    }

    override fun selectProfile(id: String): ProfileMutationResult<Unit> {
        val profile = getProfile(id)
            ?: return ProfileMutationResult.Failure(ProfileMutationFailure.PROFILE_NOT_FOUND)
        selectedProfileIds += id
        mutableActiveProfile.value = profile
        return ProfileMutationResult.Success(Unit)
    }

    override fun deleteProfile(id: String): ProfileMutationResult<Unit> {
        if (profiles.value.size <= 1) {
            return ProfileMutationResult.Failure(ProfileMutationFailure.LAST_PROFILE)
        }
        val remaining = profiles.value.filterNot { it.id == id }
        mutableProfiles.value = remaining
        if (activeProfile.value?.id == id) mutableActiveProfile.value = remaining.first()
        return ProfileMutationResult.Success(Unit)
    }

    override fun requiresInitialSetup(): Boolean {
        val active = activeProfile.value ?: return true
        return active.displayName.isBlank() || !active.studentInfo.isComplete()
    }

    private fun validate(
        displayName: String,
        studentInfo: StudentInfo,
        excludingId: String? = null
    ): ProfileMutationFailure? = when {
        displayName.isBlank() -> ProfileMutationFailure.BLANK_NAME
        displayName.length > MAX_PROFILE_NAME_LENGTH -> ProfileMutationFailure.NAME_TOO_LONG
        profiles.value.any { it.id != excludingId && it.displayName.trim() == displayName } -> {
            ProfileMutationFailure.DUPLICATE_NAME
        }
        !studentInfo.isComplete() -> ProfileMutationFailure.INCOMPLETE_STUDENT_INFO
        else -> null
    }
}
