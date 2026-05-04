package uk.ac.gre.behaviouralauth.audit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

//Tests that audit log entries are chained, persisted, and tamper-detectable.
class AuditLogTest {

    // Creates temporary folders for isolated audit log test files.
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    //Checks that the first audit entry starts from the GENESIS hash.
    @Test
    fun testGenesisEntry() {
        val logger = newLogger()

        logger.log(AuditEvent.ConsentGranted(timestamp = 1_000L))

        val firstEntry = logger.getEntries().first()
        assertEquals("GENESIS", firstEntry.previousHash)
        assertTrue(firstEntry.entryHash.isNotBlank())
    }

    //Checks that each audit entry points to the hash of the previous entry.
    @Test
    fun testChainLinks() {
        val logger = newLogger()

        logger.log(AuditEvent.ConsentGranted(timestamp = 1_000L))
        logger.log(AuditEvent.TestSessionStarted(timestamp = 2_000L))
        logger.log(AuditEvent.TestSessionEnded(timestamp = 3_000L))

        val entries = logger.getEntries()
        assertEquals(entries[0].entryHash, entries[1].previousHash)
        assertEquals(entries[1].entryHash, entries[2].previousHash)
    }

    //Verifies that an untampered audit chain passes integrity checking.
    @Test
    fun testVerifyValidChain() {
        val logger = newLogger()

        logger.log(AuditEvent.ConsentGranted(timestamp = 1_000L))
        logger.log(AuditEvent.EnrollmentStarted(timestamp = 2_000L, isAccessibilityMode = false))
        logger.log(
            AuditEvent.EnrollmentCompleted(
                timestamp = 3_000L,
                sessionCount = 3,
                isAccessibilityMode = false
            )
        )
        logger.log(AuditEvent.TestSessionStarted(timestamp = 4_000L))
        logger.log(
            AuditEvent.WindowScored(
                timestamp = 5_000L,
                trustScore = 92,
                confidence = 1.0,
                isSuspicious = false
            )
        )

        val result = logger.verifyChain()
        assertTrue(result.isValid)
        assertEquals(5, result.entriesChecked)
    }

    //Confirms that changing stored audit data breaks chain verification.
    @Test
    fun testDetectTampering() {
        val logger = newLogger()

        logger.log(AuditEvent.ConsentGranted(timestamp = 1_000L))
        logger.log(AuditEvent.TestSessionStarted(timestamp = 2_000L))
        logger.log(AuditEvent.TestSessionEnded(timestamp = 3_000L))

        val tamperedEntry = logger.getEntries()[1].copy(
            eventData = logger.getEntries()[1].eventData + ("timestamp" to "999999")
        )
        logger.replaceEntryForTesting(index = 1, entry = tamperedEntry)

        val result = logger.verifyChain()
        assertFalse(result.isValid)
        assertEquals(1, result.brokenAtSequence)
    }

    //Ensures the same audit event produces the same hash in different logs.
    @Test
    fun testEntryHashDeterministic() {
        val firstLogger = AuditLogger(temporaryFolder.newFolder("first"))
        val secondLogger = AuditLogger(temporaryFolder.newFolder("second"))
        val event = AuditEvent.ConsentGranted(timestamp = 1_234L)

        firstLogger.log(event)
        secondLogger.log(event)

        assertEquals(
            firstLogger.getEntries().single().entryHash,
            secondLogger.getEntries().single().entryHash
        )
    }

    //Checks that saved audit logs can be reloaded and still verified.
    @Test
    fun testExistingLogReloads() {
        val directory = temporaryFolder.newFolder("reload")
        val firstLogger = AuditLogger(directory)
        firstLogger.log(AuditEvent.ConsentGranted(timestamp = 1_000L))
        firstLogger.log(AuditEvent.TestSessionStarted(timestamp = 2_000L))

        val secondLogger = AuditLogger(directory)
        val result = secondLogger.verifyChain()

        assertEquals(2, secondLogger.entryCount())
        assertTrue(result.isValid)
        assertNotNull(secondLogger.getEntries().last().entryHash)
    }

    //Creates a new audit logger using a fresh temporary folder.
    private fun newLogger(): AuditLogger {
        return AuditLogger(temporaryFolder.newFolder())
    }
}