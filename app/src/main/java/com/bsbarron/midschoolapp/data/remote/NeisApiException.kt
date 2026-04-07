package com.bsbarron.midschoolapp.data.remote

class NeisApiException(
    val code: String,
    override val message: String
) : IllegalStateException(message)
