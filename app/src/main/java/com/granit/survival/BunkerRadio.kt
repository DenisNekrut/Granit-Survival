package com.granit.survival

data class RadioEvent(
    val id: String,
    val title: String,
    val description: String,
    val broadcastMinutes: Int = 10,
    val moraleGain: Float = 0f,
    val stressRelief: Float = 0f
)

object RadioEventCatalog {
    val events = listOf(
        RadioEvent("weather", "Сводка погоды", "В эфире передают: ночью ожидается похолодание. На открытых участках будет тихо, видимость останется ограниченной.", moraleGain = 0.5f),
        RadioEvent("maintenance", "Напоминание по обслуживанию", "Старая запись напоминает проверить запас фильтров и состояние вентиляции убежища.", stressRelief = 0.5f),
        RadioEvent("old_broadcast", "Фрагмент старой передачи", "Несколько секунд знакомой заставки, затем голос ведущего рассказывает о повседневной жизни до Катастрофы.", moraleGain = 1.5f),
        RadioEvent("weak_signal", "Слабый сигнал", "На частоте слышен короткий повторяющийся позывной. Ответа нет, сигнал быстро растворяется в помехах.", stressRelief = 0.5f),
        RadioEvent("quiet_frequency", "Тихая частота", "Только ровный шум эфира. Никаких новых сообщений сегодня не поступило.", moraleGain = 0.25f)
    )
}

data class BunkerRadioSnapshot(val lastListenedDay: Int = 0)

class BunkerRadioSystem {
    private var lastListenedDay: Int = 0

    fun canListen(day: Int): Boolean = day > lastListenedDay

    fun eventForDay(day: Int): RadioEvent? {
        if (!canListen(day) || RadioEventCatalog.events.isEmpty()) return null
        return RadioEventCatalog.events[Math.floorMod(day - 1, RadioEventCatalog.events.size)]
    }

    fun markListened(day: Int) {
        require(day >= 1)
        lastListenedDay = maxOf(lastListenedDay, day)
    }

    fun snapshot() = BunkerRadioSnapshot(lastListenedDay)

    fun restore(snapshot: BunkerRadioSnapshot) {
        lastListenedDay = snapshot.lastListenedDay.coerceAtLeast(0)
    }
}
