package com.granit.survival.save

/** Current persisted JSON schema. Increment only when a migration is added. */
internal object SaveVersion {
    const val CURRENT = 10
    fun isSupported(version: Int): Boolean = version in 1..CURRENT
}
