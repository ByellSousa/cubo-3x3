package com.gabs.cubo3x3.domain.reminder

import java.time.DayOfWeek

enum class ReminderDay(
    val dayOfWeek: DayOfWeek,
    val shortLabel: String,
) {
    MONDAY(DayOfWeek.MONDAY, "Seg"),
    TUESDAY(DayOfWeek.TUESDAY, "Ter"),
    WEDNESDAY(DayOfWeek.WEDNESDAY, "Qua"),
    THURSDAY(DayOfWeek.THURSDAY, "Qui"),
    FRIDAY(DayOfWeek.FRIDAY, "Sex"),
    SATURDAY(DayOfWeek.SATURDAY, "Sáb"),
    SUNDAY(DayOfWeek.SUNDAY, "Dom"),
}

data class ReminderSettings(
    val enabled: Boolean = false,
    val hour: Int = 19,
    val minute: Int = 0,
    val days: Set<ReminderDay> = ReminderDay.entries.toSet(),
) {
    init {
        require(hour in 0..23) { "Hora inválida" }
        require(minute in 0..59) { "Minuto inválido" }
        require(!enabled || days.isNotEmpty()) { "Selecione ao menos um dia" }
    }
}
