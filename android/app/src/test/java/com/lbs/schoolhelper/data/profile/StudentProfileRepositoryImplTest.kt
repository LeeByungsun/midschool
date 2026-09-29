package com.lbs.schoolhelper.data.profile

import com.lbs.schoolhelper.data.repository.StudentInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudentProfileRepositoryImplTest {

    @Test
    fun addProfile_firstProfileBecomesActive() {
        val store = FakeStudentProfileStore()
        val repository = repository(store = store, ids = listOf("profile-1"))

        val result = repository.addProfile("민준", completeStudent())

        assertEquals("profile-1", successValue(result).id)
        assertEquals(listOf("profile-1"), repository.profiles.value.map(StudentProfile::id))
        assertEquals("profile-1", repository.activeProfile.value?.id)
        assertEquals(repository.activeProfile.value, repository.getProfile("profile-1"))
    }

    @Test
    fun addProfile_trimsDisplayName() {
        val repository = repository(ids = listOf("profile-1"))

        val result = repository.addProfile("  민준  ", completeStudent())

        assertEquals("민준", successValue(result).displayName)
    }

    @Test
    fun addProfile_blankNameReturnsBlankNameFailure() {
        val repository = repository(ids = listOf("profile-1"))

        val result = repository.addProfile("   ", completeStudent())

        assertFailure(ProfileMutationFailure.BLANK_NAME, result)
        assertTrue(repository.profiles.value.isEmpty())
    }

    @Test
    fun addProfile_elevenCharactersReturnsTooLongFailure() {
        val repository = repository(ids = listOf("profile-1"))

        val result = repository.addProfile("12345678901", completeStudent())

        assertFailure(ProfileMutationFailure.NAME_TOO_LONG, result)
    }

    @Test
    fun addProfile_sameNameAfterTrimmingReturnsDuplicateFailure() {
        val repository = repository(ids = listOf("profile-1", "profile-2"))
        successValue(repository.addProfile("민준", completeStudent()))

        val result = repository.addProfile("  민준  ", completeStudent(schoolCode = "7654321"))

        assertFailure(ProfileMutationFailure.DUPLICATE_NAME, result)
        assertEquals(1, repository.profiles.value.size)
    }

    @Test
    fun addProfile_incompleteStudentInfoReturnsFailure() {
        val repository = repository(ids = listOf("profile-1"))

        val result = repository.addProfile("민준", completeStudent().copy(classroom = ""))

        assertFailure(ProfileMutationFailure.INCOMPLETE_STUDENT_INFO, result)
    }

    @Test
    fun updateProfile_preservesIdAndRegistrationOrder() {
        val first = profile("profile-1", "민준")
        val second = profile("profile-2", "서연")
        val repository = repository(
            data = StudentProfilesData(activeProfileId = first.id, profiles = listOf(first, second))
        )

        val result = repository.updateProfile(
            second.copy(displayName = "  둘째  ", studentInfo = second.studentInfo.copy(classroom = "4"))
        )

        successValue(result)
        assertEquals(listOf("profile-1", "profile-2"), repository.profiles.value.map(StudentProfile::id))
        assertEquals("둘째", repository.profiles.value[1].displayName)
        assertEquals("4", repository.profiles.value[1].studentInfo.classroom)
        assertEquals("profile-1", repository.activeProfile.value?.id)
    }

    @Test
    fun selectProfile_unknownIdReturnsNotFoundWithoutChangingActiveProfile() {
        val first = profile("profile-1", "민준")
        val repository = repository(
            data = StudentProfilesData(activeProfileId = first.id, profiles = listOf(first))
        )

        val result = repository.selectProfile("unknown")

        assertFailure(ProfileMutationFailure.PROFILE_NOT_FOUND, result)
        assertEquals(first, repository.activeProfile.value)
    }

    @Test
    fun deleteProfile_onlyProfileReturnsLastProfileFailure() {
        val first = profile("profile-1", "민준")
        val repository = repository(
            data = StudentProfilesData(activeProfileId = first.id, profiles = listOf(first))
        )

        val result = repository.deleteProfile(first.id)

        assertFailure(ProfileMutationFailure.LAST_PROFILE, result)
        assertEquals(listOf(first), repository.profiles.value)
    }

    @Test
    fun deleteProfile_activeProfileSelectsFirstRemainingProfile() {
        val first = profile("profile-1", "민준")
        val second = profile("profile-2", "서연")
        val repository = repository(
            data = StudentProfilesData(activeProfileId = first.id, profiles = listOf(first, second))
        )

        val result = repository.deleteProfile(first.id)

        successValue(result)
        assertEquals(listOf(second), repository.profiles.value)
        assertEquals(second, repository.activeProfile.value)
    }

    @Test
    fun initialize_missingActiveIdRepairsToFirstValidProfile() {
        val first = profile("profile-1", "민준")
        val second = profile("profile-2", "서연")
        val store = FakeStudentProfileStore(
            data = StudentProfilesData(activeProfileId = "missing", profiles = listOf(first, second))
        )

        val repository = repository(store = store)

        assertEquals(first, repository.activeProfile.value)
        assertEquals(first.id, store.data?.activeProfileId)
    }

    @Test
    fun initialize_filtersMalformedEntriesButKeepsValidProfiles() {
        val valid = profile("profile-1", "민준")
        val blankId = profile("", "서연")
        val duplicateId = profile("profile-1", "다른이름")
        val store = FakeStudentProfileStore(
            data = StudentProfilesData(
                activeProfileId = duplicateId.id,
                profiles = listOf(blankId, valid, duplicateId)
            )
        )

        val repository = repository(store = store)

        assertEquals(listOf(valid), repository.profiles.value)
        assertEquals(valid, repository.activeProfile.value)
        assertEquals(listOf(valid), store.data?.profiles)
    }

    @Test
    fun initialize_removesAccidentalEmptyProfiles() {
        val valid = profile("profile-1", "민준")
        val empty = StudentProfile(
            id = "profile-empty",
            displayName = "",
            studentInfo = StudentInfo()
        )
        val store = FakeStudentProfileStore(
            data = StudentProfilesData(
                activeProfileId = empty.id,
                profiles = listOf(valid, empty)
            )
        )

        val repository = repository(store = store)

        assertEquals(listOf(valid), repository.profiles.value)
        assertEquals(valid, repository.activeProfile.value)
        assertEquals(listOf(valid), store.data?.profiles)
    }

    @Test
    fun initialize_withLegacyStudentInfo_createsUnnamedPendingProfile() {
        val legacy = completeStudent().copy(classroom = "")
        val store = FakeStudentProfileStore()

        val repository = repository(
            store = store,
            legacyInfo = legacy,
            hasLegacy = true,
            ids = listOf("legacy-profile")
        )

        val pending = repository.activeProfile.value
        assertEquals("legacy-profile", pending?.id)
        assertEquals("", pending?.displayName)
        assertEquals(legacy, pending?.studentInfo)
        assertEquals(pending, store.data?.profiles?.single())
    }

    @Test
    fun initialize_pendingProfileRequiresInitialSetup() {
        val repository = repository(
            legacyInfo = completeStudent(),
            hasLegacy = true,
            ids = listOf("legacy-profile")
        )

        assertTrue(repository.requiresInitialSetup())
    }

    @Test
    fun updateMigratedProfile_afterSuccessfulWriteClearsLegacyStudentKeys() {
        var legacyCleared = false
        val repository = repository(
            legacyInfo = completeStudent(),
            hasLegacy = true,
            clearLegacy = {
                legacyCleared = true
                true
            },
            ids = listOf("legacy-profile")
        )
        val pending = requireNotNull(repository.activeProfile.value)

        val result = repository.updateProfile(pending.copy(displayName = "민준"))

        successValue(result)
        assertTrue(legacyCleared)
        assertFalse(repository.requiresInitialSetup())
    }

    @Test
    fun updateMigratedProfile_whenWriteFailsKeepsLegacyStudentKeys() {
        var legacyCleared = false
        val store = FakeStudentProfileStore(writeSucceeds = true)
        val repository = repository(
            store = store,
            legacyInfo = completeStudent(),
            hasLegacy = true,
            clearLegacy = {
                legacyCleared = true
                true
            },
            ids = listOf("legacy-profile")
        )
        val pending = requireNotNull(repository.activeProfile.value)
        store.writeSucceeds = false

        val result = repository.updateProfile(pending.copy(displayName = "민준"))

        assertFailure(ProfileMutationFailure.PERSISTENCE_FAILED, result)
        assertFalse(legacyCleared)
        assertEquals("", repository.activeProfile.value?.displayName)
    }

    @Test
    fun initialize_withoutNewOrLegacyDataStartsEmpty() {
        val repository = repository()

        assertTrue(repository.profiles.value.isEmpty())
        assertNull(repository.activeProfile.value)
        assertTrue(repository.requiresInitialSetup())
    }

    private fun repository(
        store: FakeStudentProfileStore = FakeStudentProfileStore(),
        data: StudentProfilesData? = null,
        legacyInfo: StudentInfo = StudentInfo(),
        hasLegacy: Boolean = false,
        clearLegacy: () -> Boolean = { true },
        ids: List<String> = listOf("generated-profile")
    ): StudentProfileRepositoryImpl {
        if (data != null) store.data = data
        val idIterator = ids.iterator()
        return StudentProfileRepositoryImpl.createForTest(
            store = store,
            legacyStudentInfo = { legacyInfo },
            hasLegacyStudentInfo = { hasLegacy },
            clearLegacyStudentInfo = clearLegacy,
            idFactory = { idIterator.next() }
        )
    }

    private fun profile(id: String, displayName: String): StudentProfile {
        return StudentProfile(id, displayName, completeStudent(schoolCode = "7${id.hashCode().toString().replace('-', '0').take(6)}"))
    }

    private fun completeStudent(schoolCode: String = "7010000"): StudentInfo {
        return StudentInfo(
            grade = "2",
            classroom = "3",
            schoolName = "한빛중학교",
            officeCode = "B10",
            schoolCode = schoolCode,
            schoolKind = "중학교"
        )
    }

    private fun <T> successValue(result: ProfileMutationResult<T>): T {
        assertTrue(result is ProfileMutationResult.Success)
        return (result as ProfileMutationResult.Success).value
    }

    private fun assertFailure(
        expected: ProfileMutationFailure,
        result: ProfileMutationResult<*>
    ) {
        assertTrue(result is ProfileMutationResult.Failure)
        assertEquals(expected, (result as ProfileMutationResult.Failure).reason)
    }

    private class FakeStudentProfileStore(
        var data: StudentProfilesData? = null,
        var writeSucceeds: Boolean = true
    ) : StudentProfileStore {
        override fun read(): StudentProfilesData? = data

        override fun write(data: StudentProfilesData): Boolean {
            if (!writeSucceeds) return false
            this.data = data
            return true
        }

        override fun clear(): Boolean {
            data = null
            return true
        }
    }
}
