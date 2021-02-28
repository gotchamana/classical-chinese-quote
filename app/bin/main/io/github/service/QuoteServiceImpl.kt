package io.github.service

import io.github.repository.BookRepository

class QuoteServiceImpl(val bookRepository: BookRepository) : QuoteService {
    
    override fun findRandomQuote(title: String?, wordCount: Int) =
        if (title == null)
            bookRepository.findRandomParagraph(wordCount)
        else
            bookRepository.findRandomParagraphByBookTitle(title, wordCount)?.let { Triple(title, it.first, it.second) }
}