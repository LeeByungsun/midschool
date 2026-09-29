package com.lbs.schoolhelper.data.profile

interface StudentProfileStore {
    fun read(): StudentProfilesData?

    /** Distinguishes a missing document from a document unreadable after an update. */
    fun readResult(): StudentProfileReadResult =
        read()?.let(StudentProfileReadResult::Success) ?: StudentProfileReadResult.Missing

    fun write(data: StudentProfilesData): Boolean
    fun clear(): Boolean
}

sealed interface StudentProfileReadResult {
    data object Missing : StudentProfileReadResult
    data class Success(val data: StudentProfilesData) : StudentProfileReadResult
    data class Corrupt(val rawJson: String, val error: Throwable) : StudentProfileReadResult
}
