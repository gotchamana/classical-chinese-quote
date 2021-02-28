package io.github.entity

data class Section(var id: Long? = null, val urn: String, val title: String, val paragraphs: List<String>)