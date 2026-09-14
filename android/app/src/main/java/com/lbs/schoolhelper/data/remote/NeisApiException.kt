package com.lbs.schoolhelper.data.remote

class NeisApiException(
    val code: String,
    override val message: String
) : IllegalStateException(message)

class NeisApiKeyMissingException : IllegalStateException(
    "나이스 인증키가 설정되지 않았어요. 앱 설정을 확인해 주세요."
)
