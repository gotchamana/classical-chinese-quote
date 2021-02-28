package io.github.service

interface QuoteService {
    fun findRandomQuote(title: String? = null, wordCount: Int = Int.MAX_VALUE): Triple<String, String, String>?
}