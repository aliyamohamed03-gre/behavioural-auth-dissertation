package uk.ac.gre.behaviouralauth.export

class FeatureRecordStore {
    private val records = mutableListOf<FeatureRecord>()

    fun add(record: FeatureRecord) {
        records.add(record)
    }

    fun count(): Int = records.size

    fun snapshot(): List<FeatureRecord> = records.toList()

    fun clear() {
        records.clear()
    }
}
