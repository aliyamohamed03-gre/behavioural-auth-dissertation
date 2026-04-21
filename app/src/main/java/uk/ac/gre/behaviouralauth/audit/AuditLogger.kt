package uk.ac.gre.behaviouralauth.audit

import android.content.Context
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.format.DateTimeFormatter
import org.json.JSONArray
import org.json.JSONObject

private const val GENESIS_HASH = "GENESIS"
private const val AUDIT_LOG_FILENAME = "audit_log.json"

/**
 * Maintains a SHA-256 hash-chained audit log.
 *
 * The forensic value comes from linking every entry to the hash of the previous
 * entry. If someone edits or removes a past entry, verification fails from that
 * point onward.
 */
class AuditLogger private constructor(
    private val logDirectoryProvider: () -> File
) {
    constructor(context: Context) : this({ context.applicationContext.filesDir })

    internal constructor(logDirectory: File) : this({ logDirectory })

    private val logFile: File
        get() = File(logDirectoryProvider(), AUDIT_LOG_FILENAME)

    private var entries = mutableListOf<AuditLogEntry>()
    private var lastHash: String = GENESIS_HASH

    init {
        loadExistingLog()
    }

    @Synchronized
    fun log(event: AuditEvent) {
        val eventData = eventToMap(event)
        val timestamp = eventTimestamp(event)
        val entryWithoutHash = AuditLogEntry(
            sequenceNumber = entries.size,
            timestamp = timestamp,
            timestampIso = formatUtc(timestamp),
            eventType = event.eventType,
            eventData = eventData,
            previousHash = lastHash,
            entryHash = ""
        )
        val entryHash = computeEntryHash(entryWithoutHash)
        val finalEntry = entryWithoutHash.copy(entryHash = entryHash)

        entries.add(finalEntry)
        lastHash = entryHash
        persistLog()
    }

    fun getEntries(): List<AuditLogEntry> = entries.toList()

    fun entryCount(): Int = entries.size

    /**
     * Recomputes every entry hash and checks every link in the chain.
     *
     * This is the forensic verification function used to detect tampering.
     */
    fun verifyChain(): ChainVerificationResult {
        var expectedPreviousHash = GENESIS_HASH

        entries.forEach { entry ->
            val recomputedHash = computeEntryHash(entry.copy(entryHash = ""))
            if (entry.previousHash != expectedPreviousHash) {
                return ChainVerificationResult(
                    isValid = false,
                    entriesChecked = entry.sequenceNumber + 1,
                    brokenAtSequence = entry.sequenceNumber,
                    reason = "previousHash does not match the prior entry hash"
                )
            }

            if (entry.entryHash != recomputedHash) {
                return ChainVerificationResult(
                    isValid = false,
                    entriesChecked = entry.sequenceNumber + 1,
                    brokenAtSequence = entry.sequenceNumber,
                    reason = "entryHash does not match the recomputed hash"
                )
            }

            expectedPreviousHash = entry.entryHash
        }

        return ChainVerificationResult(
            isValid = true,
            entriesChecked = entries.size
        )
    }

    private fun loadExistingLog() {
        if (!logFile.exists()) {
            entries = mutableListOf()
            lastHash = GENESIS_HASH
            return
        }

        val rawJson = logFile.readText(Charsets.UTF_8)
        if (rawJson.isBlank()) {
            entries = mutableListOf()
            lastHash = GENESIS_HASH
            return
        }

        val parsedEntries = mutableListOf<AuditLogEntry>()
        val array = JSONArray(rawJson)
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            parsedEntries.add(jsonToEntry(item))
        }

        entries = parsedEntries
        lastHash = entries.lastOrNull()?.entryHash ?: GENESIS_HASH
    }

    private fun persistLog() {
        val parent = logFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        val array = JSONArray()
        entries.forEach { entry ->
            array.put(entryToJsonObject(entry))
        }
        logFile.writeText(array.toString(2), Charsets.UTF_8)
    }

    private fun sha256(input: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun eventToMap(event: AuditEvent): Map<String, String> {
        return when (event) {
            is AuditEvent.ConsentGranted -> mapOf(
                "timestamp" to event.timestamp.toString()
            )

            is AuditEvent.EnrollmentStarted -> mapOf(
                "timestamp" to event.timestamp.toString(),
                "isAccessibilityMode" to event.isAccessibilityMode.toString()
            )

            is AuditEvent.EnrollmentCompleted -> mapOf(
                "timestamp" to event.timestamp.toString(),
                "sessionCount" to event.sessionCount.toString(),
                "isAccessibilityMode" to event.isAccessibilityMode.toString()
            )

            is AuditEvent.TestSessionStarted -> mapOf(
                "timestamp" to event.timestamp.toString()
            )

            is AuditEvent.TestSessionEnded -> mapOf(
                "timestamp" to event.timestamp.toString()
            )

            is AuditEvent.WindowScored -> mapOf(
                "timestamp" to event.timestamp.toString(),
                "trustScore" to event.trustScore.toString(),
                "confidence" to event.confidence.toString(),
                "isSuspicious" to event.isSuspicious.toString()
            )

            is AuditEvent.StateChanged -> mapOf(
                "timestamp" to event.timestamp.toString(),
                "fromState" to event.fromState,
                "toState" to event.toState,
                "reason" to event.reason
            )

            is AuditEvent.PinAttempted -> mapOf(
                "timestamp" to event.timestamp.toString(),
                "success" to event.success.toString(),
                "attemptsRemaining" to event.attemptsRemaining.toString()
            )

            is AuditEvent.ProfileReset -> mapOf(
                "timestamp" to event.timestamp.toString()
            )

            is AuditEvent.DataExported -> mapOf(
                "timestamp" to event.timestamp.toString(),
                "recordCount" to event.recordCount.toString()
            )

            is AuditEvent.SensitivityChanged -> mapOf(
                "timestamp" to event.timestamp.toString(),
                "newLevel" to event.newLevel
            )
        }
    }

    private fun eventTimestamp(event: AuditEvent): Long {
        return when (event) {
            is AuditEvent.ConsentGranted -> event.timestamp
            is AuditEvent.EnrollmentStarted -> event.timestamp
            is AuditEvent.EnrollmentCompleted -> event.timestamp
            is AuditEvent.TestSessionStarted -> event.timestamp
            is AuditEvent.TestSessionEnded -> event.timestamp
            is AuditEvent.WindowScored -> event.timestamp
            is AuditEvent.StateChanged -> event.timestamp
            is AuditEvent.PinAttempted -> event.timestamp
            is AuditEvent.ProfileReset -> event.timestamp
            is AuditEvent.DataExported -> event.timestamp
            is AuditEvent.SensitivityChanged -> event.timestamp
        }
    }

    private fun formatUtc(timestamp: Long): String {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(timestamp))
    }

    private fun computeEntryHash(entry: AuditLogEntry): String {
        return sha256(entryJsonWithoutHash(entry))
    }

    private fun entryJsonWithoutHash(entry: AuditLogEntry): String {
        val fields = listOf(
            "eventData" to stableStringMapJson(entry.eventData),
            "eventType" to JSONObject.quote(entry.eventType),
            "previousHash" to JSONObject.quote(entry.previousHash),
            "sequenceNumber" to entry.sequenceNumber.toString(),
            "timestamp" to entry.timestamp.toString(),
            "timestampIso" to JSONObject.quote(entry.timestampIso)
        ).sortedBy { it.first }

        return fields.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            "${JSONObject.quote(key)}:$value"
        }
    }

    private fun stableStringMapJson(values: Map<String, String>): String {
        return values.toSortedMap().entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            "${JSONObject.quote(key)}:${JSONObject.quote(value)}"
        }
    }

    private fun entryToJsonObject(entry: AuditLogEntry): JSONObject {
        return JSONObject().apply {
            put("sequenceNumber", entry.sequenceNumber)
            put("timestamp", entry.timestamp)
            put("timestampIso", entry.timestampIso)
            put("eventType", entry.eventType)
            put("eventData", JSONObject().apply {
                entry.eventData.toSortedMap().forEach { (key, value) ->
                    put(key, value)
                }
            })
            put("previousHash", entry.previousHash)
            put("entryHash", entry.entryHash)
        }
    }

    private fun jsonToEntry(json: JSONObject): AuditLogEntry {
        val eventDataJson = json.optJSONObject("eventData") ?: JSONObject()
        val eventData = buildMap {
            val keys = eventDataJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                put(key, eventDataJson.optString(key, ""))
            }
        }

        return AuditLogEntry(
            sequenceNumber = json.getInt("sequenceNumber"),
            timestamp = json.getLong("timestamp"),
            timestampIso = json.getString("timestampIso"),
            eventType = json.getString("eventType"),
            eventData = eventData,
            previousHash = json.getString("previousHash"),
            entryHash = json.getString("entryHash")
        )
    }

    internal fun replaceEntryForTesting(index: Int, entry: AuditLogEntry) {
        entries[index] = entry
    }
}

data class ChainVerificationResult(
    val isValid: Boolean,
    val entriesChecked: Int,
    val brokenAtSequence: Int? = null,
    val reason: String? = null
)
