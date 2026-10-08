package com.lbs.schoolhelper

object AppVersionLabel {
    fun format(versionName: String, versionCode: Int): String = "v$versionName($versionCode)"
}
