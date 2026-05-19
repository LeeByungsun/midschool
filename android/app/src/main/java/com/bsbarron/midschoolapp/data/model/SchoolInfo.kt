package com.bsbarron.midschoolapp.data.model

data class SchoolInfo(
    val officeCode: String,
    val officeName: String = "",
    val schoolCode: String,
    val schoolName: String,
    val schoolKind: String,
    val location: String = "",
    val jurisdiction: String = "",
    val foundation: String = "",
    val roadAddress: String = "",
    val telephone: String = "",
    val homepage: String = ""
)
