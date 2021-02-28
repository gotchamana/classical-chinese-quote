package io.github

import java.net.URL
import java.net.URLEncoder.encode
import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.util.concurrent.CompletableFuture

import javax.sql.DataSource

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

import org.apache.commons.lang3.SystemUtils.*
import org.h2.jdbcx.JdbcDataSource

import io.github.config.Configuration
import io.github.entity.*
import io.github.repository.BookRepositoryImpl
import io.github.service.*

const val APP_NAME = "ccq"
const val CONFIG_FILE_NAME = "config.json"

fun main(args: Array<String>) {
    val usage = """
        |Usage: ${APP_NAME} [-t BOOK_TITLE] [-w MAX_WORD_COUNT]
        |       ${APP_NAME} -u
        |       ${APP_NAME} -h
    """.trimMargin()

    if (!validateArguments(args)) {
        println(usage)
        return
    }

    val options = parseArguments(args)

    if (options.help) {
        println(usage)
        return
    }

    val config = Configuration.readFromJson(readConfigFile())

    if (options.update) {
        CompletableFuture.runAsync { updateDatabase(config) }
            .exceptionally { it.printStackTrace().run { null } }
            .also(::showProgressAnimation)
        return
    }

    getQuote(config, options.title, options.wordCount).let { 
        if (it == null) "查無句子"
        else formatQuote(it)
    }.also(::println)
}

private fun validateArguments(args: Array<String>): Boolean {
    fun isValidTitle(): Boolean {
        val index = args.indexOf("-t")
        return index >= 0 && args.size > index + 1 && !(args[index + 1] in listOf("-h", "-u", "-w"))
    }

    fun isValidWordCount(): Boolean {
        val index = args.indexOf("-w")
        return index >= 0 && args.size > index + 1 && !(args[index + 1] in listOf("-h", "-u", "-t")) &&
            args[index + 1].toIntOrNull() != null
    }

    if (args.size == 0) return true
    if (("-h" in args || "-u" in args) && args.size == 1) return true
    if (isValidTitle() && args.size == 2) return true
    if (isValidWordCount() && args.size == 2) return true
    if (isValidTitle() && isValidWordCount() && args.size == 4) return true

    return false
}

private fun parseArguments(args: Array<String>) =
    object {
        val help = "-h" in args
        val update = "-u" in args
        val title: String?
            get() {
                val index = args.indexOf("-t")
                return if (index < 0) null else args[index + 1]
            }
        val wordCount: Int?
            get() {
                val index = args.indexOf("-w")
                return if (index < 0) null else args[index + 1].toInt()
            }
    }

private fun readConfigFile(): String {
    if (IS_OS_UNIX)
        Paths.get(USER_HOME, ".config", APP_NAME, CONFIG_FILE_NAME).let { 
            if (Files.exists(it))
                return Files.readString(it)
        }
    else if (IS_OS_WINDOWS)
        Paths.get(System.getenv("AppData"), APP_NAME, CONFIG_FILE_NAME).let { 
            if (Files.exists(it))
                return Files.readString(it)
        }

    return getClasspathResource(CONFIG_FILE_NAME).readText()
}

private fun updateDatabase(config: Configuration) {
    val bookRepository = BookRepositoryImpl(getDataSource(config))
    bookRepository.deleteAll()
    downloadBooks(config).forEach(bookRepository::save)
}

private fun downloadBooks(config: Configuration) = 
    config.books.map {
        val sections = it.urns.map {
            val queryUrl = URL("${config.apiUrl}?urn=${encode(it, StandardCharsets.UTF_8)}")
            val section = jacksonObjectMapper().readValue(queryUrl, Map::class.java)

            @Suppress("UNCHECKED_CAST")
            Section(null, it, section["title"] as String, section["fulltext"] as List<String>)
        }

        Book(null, it.title, sections)
    }

private fun showProgressAnimation(job: CompletableFuture<Void>) {
    val anim = "|/-\\"
    var counter = 0

    while (!job.isDone) {
        print("\r[${anim[counter++ % anim.length]}] Update database...")
        Thread.sleep(100)
    }
    println("\rDone                  ")
}

private fun getClasspathResource(file: String) = object {}.javaClass.classLoader.getResource(file)

private fun getDataSource(config: Configuration) =
    JdbcDataSource().apply {
        setURL("${config.database.url};INIT=RUNSCRIPT FROM 'classpath:schema.sql'")
        user = config.database.user
        password = config.database.password
    }

private fun getQuote(config: Configuration, title: String?, wordCount: Int?): Triple<String, String, String>? {
    val bookRepository = BookRepositoryImpl(getDataSource(config))
    val quoteService = QuoteServiceImpl(bookRepository)
    return if (wordCount == null) quoteService.findRandomQuote(title)
        else quoteService.findRandomQuote(title, wordCount)
}

private fun formatQuote(quote: Triple<String, String, String>): String {
    val (bookTitle, sectionTitle, text) = quote
    return if (bookTitle == sectionTitle) "${text} ── 《${bookTitle}》"
        else "${text} ── 《${bookTitle}‧${sectionTitle}》"
}