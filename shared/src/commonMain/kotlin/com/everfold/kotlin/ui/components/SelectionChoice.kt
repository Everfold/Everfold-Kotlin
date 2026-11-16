package com.everfold.kotlin.ui.components

internal data class SelectionChoice(
    val id: String,
    val label: String,
    val detail: String = "",
    val selected: Boolean = false,
)
