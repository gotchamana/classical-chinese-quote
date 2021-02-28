package io.github.entity

data class Book(var id: Long? = null, val title: String, val sections: List<Section>)