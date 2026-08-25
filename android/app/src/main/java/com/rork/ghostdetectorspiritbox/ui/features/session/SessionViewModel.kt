package com.rork.ghostdetectorspiritbox.ui.features.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rork.ghostdetectorspiritbox.config.Limits
import com.rork.ghostdetectorspiritbox.data.RecordRepository
import com.rork.ghostdetectorspiritbox.domain.EventKind
import com.rork.ghostdetectorspiritbox.domain.RadarBlip
import com.rork.ghostdetectorspiritbox.domain.RadarModel
import com.rork.ghostdetectorspiritbox.domain.SessionEvent
import com.rork.ghostdetectorspiritbox.domain.SessionRecord
import com.rork.ghostdetectorspiritbox.domain.WordEngine
import com.rork.ghostdetectorspiritbox.services.MagnetometerSource
import com.rork.ghostdetectorspiritbox.services.SensorStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

/** The three ways of looking at one running investigation session. */
enum class SessionMode { RADAR, BOX, EMF }

/** Lifecycle of the instrument. */
enum class SessionPhase { IDLE, CALIBRATING, RUNNING, ENDED }

data class SessionUiState(
    val phase: SessionPhase = SessionPhase.IDLE,
    val mode: SessionMode = SessionMode.RADAR,
    val sessionNumber: Int = 1,
    val elapsedMillis: Long = 0L,
    val remainingMillis: Long = Limits.FREE_SESSION_MILLIS,
    val calibrationProgress: Float = 0f,
    val sensorStatus: SensorStatus = SensorStatus.UNAVAILABLE,
    val liveMicroTesla: Double? = null,
    val baselineMicroTesla: Double? = null,
    val thresholdMicroTesla: Double? = null,
    val peakMicroTesla: Double = 0.0,
    val spikeCount: Int = 0,
    val lastSpikeAtMillis: Long? = null,
    val spikeActive: Boolean = false,
    val history: List<Float> = emptyList(),
    val currentWord: String? = null,
    val log: List<SessionEvent> = emptyList(),
    val blips: List<RadarBlip> = emptyList(),
    val archiveCount: Int = 0,
    val archiveCapacity: Int = Limits.FREE_ARCHIVE_CAPACITY,
    val finishedRecordId: String? = null,
    val endedByTimeLimit: Boolean = false
) {
    val isRunning: Boolean get() = phase == SessionPhase.CALIBRATING || phase == SessionPhase.RUNNING
    val archiveFull: Boolean get() = archiveCount >= archiveCapacity
}

/**
 * Owns one investigation session. The magnetometer is real; the radar is a
 * visualization model and the spirit box is a local word engine — neither ever claims
 * to receive a broadcast or to hear anything through a microphone.
 */
class SessionViewModel(application: Application) : AndroidViewModel(application) {

    private val magnetometer = MagnetometerSource(application)
    private val repository = RecordRepository.get(application)
    private val wordEngine = WordEngine()
    private val radar = RadarModel()

    private val _state = MutableStateFlow(
        SessionUiState(
            sensorStatus = magnetometer.status,
            sessionNumber = repository.nextSessionNumber()
        )
    )
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    private var loopJob: Job? = null
    private var sensorJob: Job? = null

    private var startWallClock: Long = 0L
    private var rawField: Double? = null
    private var smoothedField: Double? = null
    private val calibrationSamples = mutableListOf<Double>()
    private val events = mutableListOf<SessionEvent>()
    private val historyBuffer = ArrayDeque<Float>()

    private var nextWordAt: Long = Long.MAX_VALUE
    private var answeringQuestion = false
    private var currentWordUntil: Long = 0L
    private var lastRadarTick: Long = 0L
    private var lastHistorySampleAt: Long = 0L
    private var activeSpikeIndex: Int? = null
    private var backgroundedAtWallClock: Long? = null

    init {
        viewModelScope.launch {
            repository.records.collect { records ->
                _state.value = _state.value.copy(
                    archiveCount = records.size,
                    sessionNumber = if (_state.value.isRunning) {
                        _state.value.sessionNumber
                    } else {
                        repository.nextSessionNumber()
                    }
                )
            }
        }
    }

    fun setMode(mode: SessionMode) {
        _state.value = _state.value.copy(mode = mode)
    }

    /** Start one investigation session. Every mode starts the same session. */
    fun startSession() {
        if (_state.value.isRunning) return
        events.clear()
        historyBuffer.clear()
        calibrationSamples.clear()
        radar.clear()
        activeSpikeIndex = null
        answeringQuestion = false
        currentWordUntil = 0L
        lastRadarTick = 0L
        lastHistorySampleAt = 0L
        nextWordAt = Long.MAX_VALUE
        smoothedField = null
        rawField = null
        startWallClock = System.currentTimeMillis()

        events += SessionEvent(EventKind.SESSION_START, 0L)
        _state.value = _state.value.copy(
            phase = SessionPhase.CALIBRATING,
            sessionNumber = repository.nextSessionNumber(),
            elapsedMillis = 0L,
            remainingMillis = Limits.FREE_SESSION_MILLIS,
            calibrationProgress = 0f,
            liveMicroTesla = null,
            baselineMicroTesla = null,
            thresholdMicroTesla = null,
            peakMicroTesla = 0.0,
            spikeCount = 0,
            lastSpikeAtMillis = null,
            spikeActive = false,
            history = emptyList(),
            currentWord = null,
            log = emptyList(),
            blips = emptyList(),
            finishedRecordId = null,
            endedByTimeLimit = false
        )

        sensorJob?.cancel()
        sensorJob = viewModelScope.launch {
            magnetometer.readings().collect { value -> rawField = value }
        }
        loopJob?.cancel()
        loopJob = viewModelScope.launch {
            while (isActive) {
                tick()
                delay(Limits.TICK_MILLIS)
            }
        }
    }

    /** Log a question and prime the engine for a prompt response. */
    fun askQuestion(question: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty() || _state.value.phase != SessionPhase.RUNNING) return
        val at = _state.value.elapsedMillis
        append(SessionEvent(EventKind.QUESTION, at, trimmed))
        answeringQuestion = true
        nextWordAt = at + wordEngine.nextDelayMillis(true, deviation())
    }

    /** Star the moment: attaches to the latest response when one is fresh. */
    fun markMoment() {
        if (_state.value.phase != SessionPhase.RUNNING) return
        val at = _state.value.elapsedMillis
        val lastWordIndex = events.indexOfLast { it.kind == EventKind.WORD }
        val fresh = lastWordIndex >= 0 &&
            at - events[lastWordIndex].atMillis <= Limits.MARK_ATTACH_WINDOW_MILLIS &&
            !events[lastWordIndex].marked
        if (fresh) {
            events[lastWordIndex] = events[lastWordIndex].copy(marked = true)
            publishLog()
        } else {
            append(SessionEvent(EventKind.MARKED, at))
        }
    }

    /** End the session deliberately and write the record. */
    fun endSession(byTimeLimit: Boolean = false) {
        val current = _state.value
        if (!current.isRunning) return
        loopJob?.cancel()
        sensorJob?.cancel()
        loopJob = null
        sensorJob = null

        val elapsed = current.elapsedMillis
        append(SessionEvent(EventKind.SESSION_END, elapsed))

        val record = SessionRecord(
            id = UUID.randomUUID().toString(),
            number = current.sessionNumber,
            startedAtEpochMillis = startWallClock,
            durationMillis = elapsed,
            baselineMicroTesla = current.baselineMicroTesla ?: 0.0,
            peakMicroTesla = current.peakMicroTesla,
            thresholdMicroTesla = current.thresholdMicroTesla ?: 0.0,
            events = events.toList()
        )
        repository.save(record)
        radar.clear()
        _state.value = current.copy(
            phase = SessionPhase.ENDED,
            finishedRecordId = record.id,
            endedByTimeLimit = byTimeLimit,
            currentWord = null,
            blips = emptyList(),
            spikeActive = false,
            log = events.toList()
        )
    }

    /** Called once the finished record has been opened. */
    fun consumeFinishedRecord() {
        _state.value = _state.value.copy(
            phase = SessionPhase.IDLE,
            finishedRecordId = null,
            elapsedMillis = 0L,
            liveMicroTesla = null,
            history = emptyList(),
            log = emptyList(),
            sessionNumber = repository.nextSessionNumber()
        )
    }

    /** Session keeps running in the background; the gap is logged on return. */
    fun onAppBackgrounded() {
        if (!_state.value.isRunning) return
        backgroundedAtWallClock = System.currentTimeMillis()
    }

    fun onAppForegrounded() {
        val since = backgroundedAtWallClock ?: return
        backgroundedAtWallClock = null
        if (!_state.value.isRunning) return
        val gap = System.currentTimeMillis() - since
        if (gap < Limits.MIN_PAUSE_MILLIS) return
        val at = _state.value.elapsedMillis
        append(SessionEvent(EventKind.PAUSE, at, value = gap.toDouble()))
        if (gap > Limits.GRACE_PERIOD_MILLIS) endSession()
    }

    private fun tick() {
        val current = _state.value
        val elapsed = System.currentTimeMillis() - startWallClock
        val raw = rawField

        if (raw != null) {
            val previous = smoothedField
            smoothedField = if (previous == null) raw else previous + (raw - previous) * 0.25
        }
        val live = smoothedField

        when (current.phase) {
            SessionPhase.CALIBRATING -> {
                if (raw != null) calibrationSamples += raw
                val progress = (elapsed.toFloat() / Limits.CALIBRATION_MILLIS).coerceIn(0f, 1f)
                if (elapsed >= Limits.CALIBRATION_MILLIS) {
                    finishCalibration(elapsed, live)
                } else {
                    _state.value = current.copy(
                        elapsedMillis = elapsed,
                        calibrationProgress = progress,
                        liveMicroTesla = live
                    )
                }
            }

            SessionPhase.RUNNING -> advanceRunning(current, elapsed, live)

            else -> Unit
        }
    }

    private fun finishCalibration(elapsed: Long, live: Double?) {
        val baseline = if (calibrationSamples.isNotEmpty()) {
            calibrationSamples.average()
        } else {
            live ?: 0.0
        }
        val threshold = ((baseline * Limits.THRESHOLD_FACTOR) * 10.0).roundToInt() / 10.0
        events += SessionEvent(EventKind.BASELINE, elapsed, value = baseline)
        nextWordAt = elapsed + wordEngine.nextDelayMillis(false, 0.0)
        lastRadarTick = elapsed
        _state.value = _state.value.copy(
            phase = SessionPhase.RUNNING,
            elapsedMillis = elapsed,
            calibrationProgress = 1f,
            baselineMicroTesla = baseline,
            thresholdMicroTesla = threshold,
            liveMicroTesla = live,
            log = events.toList()
        )
    }

    private fun advanceRunning(current: SessionUiState, elapsed: Long, live: Double?) {
        val baseline = current.baselineMicroTesla ?: 0.0
        val threshold = current.thresholdMicroTesla ?: Double.MAX_VALUE
        var peak = current.peakMicroTesla
        var spikeCount = current.spikeCount
        var lastSpikeAt = current.lastSpikeAtMillis
        var spikeActive = current.spikeActive

        if (live != null) {
            if (live > peak) peak = live
            if (live >= threshold) {
                if (!spikeActive) {
                    spikeActive = true
                    spikeCount += 1
                    lastSpikeAt = elapsed
                    events += SessionEvent(EventKind.EMF_SPIKE, elapsed, value = live)
                    activeSpikeIndex = events.lastIndex
                } else {
                    val index = activeSpikeIndex
                    if (index != null) {
                        val logged = events[index]
                        if ((logged.value ?: 0.0) < live) {
                            events[index] = logged.copy(value = live)
                        }
                    }
                }
            } else if (spikeActive && live < threshold - Limits.SPIKE_RELEASE_MARGIN) {
                spikeActive = false
                activeSpikeIndex = null
            }
        }

        if (elapsed - lastHistorySampleAt >= Limits.HISTORY_INTERVAL_MILLIS) {
            lastHistorySampleAt = elapsed
            historyBuffer.addLast((live ?: baseline).toFloat())
            while (historyBuffer.size > Limits.HISTORY_POINTS) historyBuffer.removeFirst()
        }

        if (elapsed - lastRadarTick >= Limits.RADAR_INTERVAL_MILLIS) {
            lastRadarTick = elapsed
            radar.tick(elapsed, deviation(live, baseline, threshold)).forEach { events += it }
        }

        var currentWord = current.currentWord
        if (elapsed >= nextWordAt) {
            val engineWord = wordEngine.next(answeringQuestion, deviation(live, baseline, threshold))
            events += SessionEvent(
                kind = EventKind.WORD,
                atMillis = elapsed,
                text = engineWord.word,
                category = engineWord.category
            )
            currentWord = engineWord.word
            currentWordUntil = elapsed + Limits.WORD_HOLD_MILLIS
            answeringQuestion = false
            nextWordAt = elapsed +
                wordEngine.nextDelayMillis(false, deviation(live, baseline, threshold))
        }
        if (currentWord != null && elapsed > currentWordUntil) currentWord = null

        val remaining = (Limits.FREE_SESSION_MILLIS - elapsed).coerceAtLeast(0L)

        _state.value = current.copy(
            elapsedMillis = elapsed,
            remainingMillis = remaining,
            liveMicroTesla = live,
            peakMicroTesla = peak,
            spikeCount = spikeCount,
            lastSpikeAtMillis = lastSpikeAt,
            spikeActive = spikeActive,
            history = historyBuffer.toList(),
            currentWord = currentWord,
            log = events.toList(),
            blips = radar.blips
        )

        if (remaining == 0L) endSession(byTimeLimit = true)
    }

    private fun append(event: SessionEvent) {
        events += event
        publishLog()
    }

    private fun publishLog() {
        _state.value = _state.value.copy(log = events.toList())
    }

    private fun deviation(): Double {
        val current = _state.value
        return deviation(
            current.liveMicroTesla,
            current.baselineMicroTesla ?: 0.0,
            current.thresholdMicroTesla ?: Double.MAX_VALUE
        )
    }

    private fun deviation(live: Double?, baseline: Double, threshold: Double): Double {
        if (live == null || threshold <= baseline) return 0.0
        return (abs(live - baseline) / (threshold - baseline)).coerceIn(0.0, 1.0)
    }

    override fun onCleared() {
        loopJob?.cancel()
        sensorJob?.cancel()
        super.onCleared()
    }
}
