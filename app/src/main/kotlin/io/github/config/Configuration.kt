package io.github.config

import java.io.IOException

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

import org.apache.commons.validator.routines.UrlValidator

import io.github.exception.ConfigurationException

data class Configuration(val apiUrl: String, val database: Database, val books: List<Book>) {

    data class Database(val url: String, val user: String, val password: String)

    data class Book(val title: String, val urns: List<String>)

    companion object {
        fun readFromJson(json: String) =
            try {
                jacksonObjectMapper().readValue<Configuration>(json).also(::validateConfiguration) 
            } catch(e: IOException) {
                throw ConfigurationException("Reading configuration failed", e)
            }

        private fun validateConfiguration(config: Configuration) {
            if (!isValidApiUrl(config.apiUrl) ||
                !isValidDatabaseUrl(config.database.url) ||
                !isValidBooks(config.books))
                throw ConfigurationException("Invalid configuration")
        }

        private fun isValidApiUrl(url: String): Boolean {
            val validator = UrlValidator(arrayOf("http", "https"))
            return validator.isValid(url)
        }

        private fun isValidDatabaseUrl(url: String) = url.startsWith("jdbc:h2:")

        private fun isValidBooks(books: List<Book>): Boolean {
            return !books.any { 
                it.title.isBlank() || !isValidUrns(it.urns)
            }
        }

        private fun isValidUrns(urns: List<String>): Boolean {
            return urns.isNotEmpty() && urns.all { it.startsWith("ctp:") }
        }
    }
}