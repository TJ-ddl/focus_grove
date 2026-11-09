package com.personal.forest.session

import com.personal.forest.data.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable

@Serializable
data class RunningSession(
    val id: String,
    val speciesId: String,
    val speciesName: String,
    val plannedMin: Int,
    val tag: String,
    val startWall: Long,
    val startElapsed: Long,
)

data class FinishedResult(
    val session: Session,
    val achievements: List<String>,
)

/** Single in-process source of truth for the running session and the last result. */
object Focus {
    val running = MutableStateFlow<RunningSession?>(null)
    val result = MutableStateFlow<FinishedResult?>(null)
}
