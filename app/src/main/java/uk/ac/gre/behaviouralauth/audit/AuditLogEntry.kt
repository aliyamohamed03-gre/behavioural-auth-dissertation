package uk.ac.gre.behaviouralauth.audit

//Represents one saved record in the audit log.
data class AuditLogEntry(
    val sequenceNumber: Int,
    val timestamp: Long,
    val timestampIso: String,
    val eventType: String,
    val eventData: Map<String, String>,

    //Links this record to the one before it, helping detect missing or changed entries.
    val previousHash: String,

    //Stores the hash of this record so the log can be checked for tampering.
    val entryHash: String
)