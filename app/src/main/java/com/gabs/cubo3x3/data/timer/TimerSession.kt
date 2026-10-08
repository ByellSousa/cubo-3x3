package com.gabs.cubo3x3.data.timer

data class TimerSession(
    val id: Long,
    val name: String,
    val createdAtEpochMillis: Long,
)

const val DEFAULT_TIMER_SESSION_ID = 1L
const val DEFAULT_TIMER_SESSION_NAME = "Principal"
