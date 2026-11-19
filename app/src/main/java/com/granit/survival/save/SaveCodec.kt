package com.granit.survival.save

import org.json.JSONObject

/** Single entry point for parsing and writing the on-disk save envelope. */
internal object SaveCodec {
    fun decode(serialized: String): JSONObject {
        val parsed = JSONObject(serialized)
        return SaveValidator.validate(SaveMigration.migrate(parsed))
    }

    fun encode(state: JSONObject, savedAtMillis: Long): String {
        val root = JSONObject()
            .put("saveVersion", SaveVersion.CURRENT)
            .put("savedAtMillis", savedAtMillis.coerceAtLeast(0L))
            .put("state", JSONObject(state.toString()))
        return SaveValidator.validate(SaveMigration.migrate(root)).toString()
    }
}
