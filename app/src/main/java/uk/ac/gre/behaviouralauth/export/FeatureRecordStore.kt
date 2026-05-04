package uk.ac.gre.behaviouralauth.export

class FeatureRecordStore {
    //Keeps feature records in memory until they are exported or cleared.
    private val records = mutableListOf<FeatureRecord>()

    fun add(record: FeatureRecord) {
        records.add(record)
    }

    fun count(): Int = records.size

    //Returns a copy so callers cannot directly change the stored list.
    fun snapshot(): List<FeatureRecord> = records.toList()

    fun clear() {
        records.clear()
    }
}