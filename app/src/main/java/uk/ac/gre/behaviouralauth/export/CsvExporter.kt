package uk.ac.gre.behaviouralauth.export

import android.content.Context
import java.io.BufferedWriter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvExporter(private val context: Context) {

    //Exports collected feature records into a timestamped CSV file.
    fun export(records: List<FeatureRecord>): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(
            context.getExternalFilesDir(null),
            "behavioural_features_$timestamp.csv"
        )

        file.bufferedWriter(Charsets.UTF_8).use { writer ->
            writeHeader(writer)
            records.forEach { record ->
                writeRow(writer, record)
            }
        }

        return file
    }

    private fun writeHeader(writer: BufferedWriter) {
        //Writes the column names first so the export is easy to understand.
        writer.appendLine(CSV_HEADER)
    }

    private fun writeRow(writer: BufferedWriter, record: FeatureRecord) {
        //Converts one feature record into a single CSV row.
        writer.appendLine(
            listOf(
                csvString(record.userId),
                csvString(record.sessionId),
                csvString(record.sessionType),
                record.windowStartMs.toString(),
                record.windowEndMs.toString(),
                record.keystrokeCount.toString(),
                record.gestureCount.toString(),
                csvDouble(record.confidence),
                csvDouble(record.meanInterKeyInterval),
                csvDouble(record.stdInterKeyInterval),
                csvDouble(record.medianInterKeyInterval),
                csvDouble(record.deleteRatio),
                csvDouble(record.typingSpeed),
                csvDouble(record.meanSwipeVelocity),
                csvDouble(record.stdSwipeVelocity),
                csvDouble(record.meanSwipeDuration),
                csvDouble(record.meanSwipeDistance),
                csvDouble(record.stdSwipeDuration),
                csvDouble(record.stdSwipeDistance)
            ).joinToString(",")
        )
    }

    private fun csvDouble(value: Double): String {
        //Keeps decimal values consistent across devices and locales.
        return String.format(Locale.US, "%.6f", value)
    }

    private fun csvString(value: String): String {
        //Escapes text safely in case it contains commas, quotes, or line breaks.
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    private companion object {
        const val CSV_HEADER =
            "user_id,session_id,session_type,window_start_ms,window_end_ms,keystroke_count,gesture_count,confidence,mean_inter_key_interval,std_inter_key_interval,median_inter_key_interval,delete_ratio,typing_speed,mean_swipe_velocity,std_swipe_velocity,mean_swipe_duration,mean_swipe_distance,std_swipe_duration,std_swipe_distance"
    }
}