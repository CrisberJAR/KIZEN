package com.kizen.tasks.domain.model

enum class Priority(val label: String, val colorHex: String) {
    LOW("Baja", "#B5EAD7"),
    MEDIUM("Media", "#A8D8EA"),
    HIGH("Alta", "#F8C8DC");

    companion object {
        fun from(raw: String): Priority =
            entries.find { it.name == raw } ?: MEDIUM
    }
}
