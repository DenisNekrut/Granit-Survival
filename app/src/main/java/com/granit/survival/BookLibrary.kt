package com.granit.survival

enum class BookCategory(val displayName: String) {
    FICTION("Художественная литература"),
    SCIENCE("Научная книга"),
    TECHNICAL("Техническое руководство"),
    DIARY("Дневник"),
    MAGAZINE("Журнал")
}

data class BookDefinition(
    val id: String,
    val title: String,
    val category: BookCategory,
    val description: String,
    val readMinutes: Int,
    val moraleGain: Float = 0f,
    val stressRelief: Float = 0f
)

object BookCatalog {
    val all = listOf(
        BookDefinition("old_novel", "Старый роман", BookCategory.FICTION,
            "История из мира до Катастрофы", 25, moraleGain = 2f),
        BookDefinition("field_atlas", "Атлас природы", BookCategory.SCIENCE,
            "Наблюдения о растениях, погоде и местности", 30, moraleGain = 1f, stressRelief = 1f),
        BookDefinition("field_first_aid", "Первая помощь в полевых условиях", BookCategory.TECHNICAL,
            "Практическое руководство по уходу за собой", 45),
        BookDefinition("bunker_diary", "Дневник дежурного", BookCategory.DIARY,
            "Записи человека, который жил в этом убежище", 20, stressRelief = 3f),
        BookDefinition("old_magazine", "Старый журнал", BookCategory.MAGAZINE,
            "Короткие заметки и статьи прежних лет", 15, moraleGain = 1f)
    )
    fun byId(id: String): BookDefinition? = all.firstOrNull { it.id == id }
}

data class BookLibrarySnapshot(val readBookIds: Set<String> = emptySet())

class BookLibrary {
    private val readIds = linkedSetOf<String>()

    fun hasRead(bookId: String): Boolean = bookId in readIds
    fun markRead(bookId: String): Boolean {
        if (BookCatalog.byId(bookId) == null) return false
        return readIds.add(bookId)
    }
    fun snapshot() = BookLibrarySnapshot(readIds.toSet())
    fun restore(snapshot: BookLibrarySnapshot) {
        readIds.clear()
        snapshot.readBookIds.filter { BookCatalog.byId(it) != null }.forEach(readIds::add)
    }
}
