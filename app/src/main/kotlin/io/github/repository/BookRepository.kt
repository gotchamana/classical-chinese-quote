package io.github.repository

import io.github.entity.Book

interface BookRepository {
    fun findRandomParagraph(wordCount: Int): Triple<String, String, String>?
    fun findRandomParagraphByBookTitle(title: String, wordCount: Int): Pair<String, String>?
    fun save(book: Book): Book
    fun deleteAll()
}