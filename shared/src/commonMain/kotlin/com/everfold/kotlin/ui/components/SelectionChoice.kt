package com.everfold.kotlin.ui.components

internal data class SelectionChoice(
    val id: String,
    val label: String,
    val detail: String = "",
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val readOnly: Boolean = false,
    val copyValue: String? = null,
    val searchable: Boolean = true,
)
