package com.lbs.schoolhelper.data.profile

interface StudentProfileStore {
    fun read(): StudentProfilesData?
    fun write(data: StudentProfilesData): Boolean
    fun clear(): Boolean
}
