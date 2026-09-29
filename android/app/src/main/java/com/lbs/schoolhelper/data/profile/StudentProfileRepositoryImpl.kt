package com.lbs.schoolhelper.data.profile

import android.util.Log
import com.lbs.schoolhelper.data.repository.StudentInfo
import com.lbs.schoolhelper.data.repository.UserPreferencesStore
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StudentProfileRepositoryImpl private constructor(
    private val store: StudentProfileStore,
    private val legacyStudentInfo: () -> StudentInfo,
    private val hasLegacyStudentInfo: () -> Boolean,
    private val clearLegacyStudentInfo: () -> Boolean,
    private val idFactory: () -> String
) : StudentProfileRepository {

    @Inject
    constructor(
        store: StudentProfileStore,
        userPreferencesStore: UserPreferencesStore
    ) : this(
        store = store,
        legacyStudentInfo = userPreferencesStore::getLegacyStudentInfo,
        hasLegacyStudentInfo = userPreferencesStore::hasLegacyStudentInfo,
        clearLegacyStudentInfo = userPreferencesStore::clearLegacyStudentInfo,
        idFactory = { UUID.randomUUID().toString() }
    )

    private val _profiles = MutableStateFlow<List<StudentProfile>>(emptyList())
    override val profiles: StateFlow<List<StudentProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow<StudentProfile?>(null)
    override val activeProfile: StateFlow<StudentProfile?> = _activeProfile.asStateFlow()

    private var currentData = StudentProfilesData(activeProfileId = "", profiles = emptyList())
    private var migratedProfileId: String? = null

    init {
        initialize()
    }

    override fun getProfile(id: String): StudentProfile? {
        return currentData.profiles.firstOrNull { it.id == id }
    }

    override fun addProfile(
        displayName: String,
        studentInfo: StudentInfo
    ): ProfileMutationResult<StudentProfile> {
        val normalizedName = displayName.trim()
        validateProfile(normalizedName, studentInfo)?.let {
            return ProfileMutationResult.Failure(it)
        }

        val profile = StudentProfile(
            id = idFactory(),
            displayName = normalizedName,
            studentInfo = studentInfo
        )
        val updated = currentData.copy(
            activeProfileId = profile.id,
            profiles = currentData.profiles + profile
        )
        if (!persist(updated)) return ProfileMutationResult.Failure(ProfileMutationFailure.PERSISTENCE_FAILED)
        return ProfileMutationResult.Success(profile)
    }

    override fun updateProfile(profile: StudentProfile): ProfileMutationResult<Unit> {
        val index = currentData.profiles.indexOfFirst { it.id == profile.id }
        if (index < 0) return ProfileMutationResult.Failure(ProfileMutationFailure.PROFILE_NOT_FOUND)

        val normalizedName = profile.displayName.trim()
        validateProfile(normalizedName, profile.studentInfo, excludingId = profile.id)?.let {
            return ProfileMutationResult.Failure(it)
        }

        val normalizedProfile = profile.copy(displayName = normalizedName)
        val updatedProfiles = currentData.profiles.toMutableList().apply {
            this[index] = normalizedProfile
        }
        val updated = currentData.copy(profiles = updatedProfiles)
        if (!persist(updated)) return ProfileMutationResult.Failure(ProfileMutationFailure.PERSISTENCE_FAILED)

        if (migratedProfileId == profile.id && clearLegacyStudentInfo()) {
            migratedProfileId = null
        }
        return ProfileMutationResult.Success(Unit)
    }

    override fun selectProfile(id: String): ProfileMutationResult<Unit> {
        if (getProfile(id) == null) {
            return ProfileMutationResult.Failure(ProfileMutationFailure.PROFILE_NOT_FOUND)
        }
        if (id == currentData.activeProfileId) return ProfileMutationResult.Success(Unit)

        val updated = currentData.copy(activeProfileId = id)
        if (!persist(updated)) return ProfileMutationResult.Failure(ProfileMutationFailure.PERSISTENCE_FAILED)
        return ProfileMutationResult.Success(Unit)
    }

    override fun deleteProfile(id: String): ProfileMutationResult<Unit> {
        if (getProfile(id) == null) {
            return ProfileMutationResult.Failure(ProfileMutationFailure.PROFILE_NOT_FOUND)
        }
        if (currentData.profiles.size == 1) {
            return ProfileMutationResult.Failure(ProfileMutationFailure.LAST_PROFILE)
        }

        val remaining = currentData.profiles.filterNot { it.id == id }
        val activeId = if (currentData.activeProfileId == id) {
            remaining.first().id
        } else {
            currentData.activeProfileId
        }
        val updated = currentData.copy(activeProfileId = activeId, profiles = remaining)
        if (!persist(updated)) return ProfileMutationResult.Failure(ProfileMutationFailure.PERSISTENCE_FAILED)
        if (migratedProfileId == id) migratedProfileId = null
        return ProfileMutationResult.Success(Unit)
    }

    override fun requiresInitialSetup(): Boolean {
        val active = activeProfile.value ?: return true
        return active.displayName.isBlank() || !active.studentInfo.isComplete()
    }

    private fun initialize() {
        val readResult = store.readResult()
        if (readResult is StudentProfileReadResult.Corrupt) {
            Log.w(TAG, "Stored student profiles could not be decoded; falling back to setup", readResult.error)
        }
        val stored = (readResult as? StudentProfileReadResult.Success)?.data
        if (stored != null && stored.schemaVersion == PROFILE_SCHEMA_VERSION) {
            val normalized = normalize(stored)
            currentData = normalized
            publish(normalized)
            if (normalized != stored) store.write(normalized)
            migratedProfileId = normalized.profiles
                .singleOrNull { it.displayName.isBlank() && hasLegacyStudentInfo() }
                ?.id
            return
        }

        if (!hasLegacyStudentInfo()) {
            publish(currentData)
            return
        }

        val pending = StudentProfile(
            id = idFactory(),
            displayName = "",
            studentInfo = legacyStudentInfo()
        )
        val migrated = StudentProfilesData(
            activeProfileId = pending.id,
            profiles = listOf(pending)
        )
        store.write(migrated)
        currentData = migrated
        migratedProfileId = pending.id
        publish(migrated)
    }

    private fun normalize(data: StudentProfilesData): StudentProfilesData {
        val seenIds = mutableSetOf<String>()
        val validProfiles = data.profiles.filter { profile ->
            profile.id.isNotBlank() &&
                // Keep a named profile or a complete legacy-migration profile,
                // but discard accidental empty rows created by an interrupted
                // setup flow.
                (profile.displayName.isNotBlank() || profile.studentInfo.isComplete()) &&
                seenIds.add(profile.id)
        }
        val activeId = data.activeProfileId
            .takeIf { id -> validProfiles.any { it.id == id } }
            ?: validProfiles.firstOrNull()?.id.orEmpty()
        return StudentProfilesData(
            schemaVersion = PROFILE_SCHEMA_VERSION,
            activeProfileId = activeId,
            profiles = validProfiles
        )
    }

    private fun validateProfile(
        displayName: String,
        studentInfo: StudentInfo,
        excludingId: String? = null
    ): ProfileMutationFailure? {
        return when {
            displayName.isBlank() -> ProfileMutationFailure.BLANK_NAME
            displayName.length > MAX_PROFILE_NAME_LENGTH -> ProfileMutationFailure.NAME_TOO_LONG
            currentData.profiles.any { it.id != excludingId && it.displayName.trim() == displayName } -> {
                ProfileMutationFailure.DUPLICATE_NAME
            }
            !studentInfo.isComplete() -> ProfileMutationFailure.INCOMPLETE_STUDENT_INFO
            else -> null
        }
    }

    private fun persist(data: StudentProfilesData): Boolean {
        if (!store.write(data)) return false
        currentData = data
        publish(data)
        return true
    }

    private fun publish(data: StudentProfilesData) {
        _profiles.value = data.profiles
        _activeProfile.value = data.profiles.firstOrNull { it.id == data.activeProfileId }
    }

    companion object {
        private const val TAG = "StudentProfileRepository"

        internal fun createForTest(
            store: StudentProfileStore,
            legacyStudentInfo: () -> StudentInfo,
            hasLegacyStudentInfo: () -> Boolean,
            clearLegacyStudentInfo: () -> Boolean,
            idFactory: () -> String
        ): StudentProfileRepositoryImpl {
            return StudentProfileRepositoryImpl(
                store = store,
                legacyStudentInfo = legacyStudentInfo,
                hasLegacyStudentInfo = hasLegacyStudentInfo,
                clearLegacyStudentInfo = clearLegacyStudentInfo,
                idFactory = idFactory
            )
        }
    }
}
