package uk.ac.gre.behaviouralauth.audit

/**
 * One row in the tamper-evident audit chain.
 *
 * previousHash links this entry to the prior entry. entryHash proves the
 * current entry has not been changed since it was written.
 */
data class AuditLogEntry(
    val sequenceNumber: Int,
    val timestamp: Long,
    val timestampIso: String,
    val eventType: String,
    val eventData: Map<String, String>,
    val previousHash: String,
    val entryHash: String
)
