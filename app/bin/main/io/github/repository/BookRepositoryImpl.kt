package io.github.repository

import java.sql.*

import javax.sql.DataSource

import io.github.entity.*

class BookRepositoryImpl(val dataSource: DataSource) : BookRepository {
    
    override fun findRandomParagraph(wordCount: Int) =
        dataSource.connection.use { 
            val sql = """
                |SELECT b.title, s.title, p.content
                |FROM book b, section s, paragraph p
                |WHERE b.id = s.book_id AND s.id = p.section_id AND LENGTH(p.content) <= ?1
                |ORDER BY RAND()
                |LIMIT 1
                """.trimMargin()

            it.prepareStatement(sql).use { 
                it.setInt(1, wordCount)
                it.executeQuery().run { 
                    if (next()) Triple(getString(1), getString(2), getString(3)) else null
                }
            }
        }

    override fun findRandomParagraphByBookTitle(title: String, wordCount: Int) =
        dataSource.connection.use { 
            val sql = """
                |SELECT s.title, p.content
                |FROM book b, section s, paragraph p
                |WHERE b.id = s.book_id AND s.id = p.section_id AND b.title = ?2 AND LENGTH(p.content) <= ?1
                |ORDER BY RAND()
                |LIMIT 1
                """.trimMargin()
            it.prepareStatement(sql).use { 
                it.setInt(1, wordCount)
                it.setString(2, title)
                it.executeQuery().run { 
                    if (next()) Pair(getString(1), getString(2)) else null
                }
            }
        }

    override fun save(book: Book): Book {
        dataSource.connection.use { 
            it.autoCommit = false

            try {
                saveBook(it, book)
                saveSections(it, book.id!!, book.sections)
                saveParagraphs(it, book.sections)
                it.commit()
            } catch (e: Exception) {
                it.rollback()
                throw e
            }
        }

        return book
    }

    private fun saveBook(conn: Connection, book: Book) {
        conn.prepareStatement("INSERT INTO book(title) VALUES(?)", Statement.RETURN_GENERATED_KEYS).use {
            it.setString(1, book.title)
            it.executeUpdate()

            val rs = it.generatedKeys
            rs.next()
            book.id = rs.getLong(1)
        }
    }

    private fun saveSections(conn: Connection, bookId: Long, sections: List<Section>) {
        conn.prepareStatement("INSERT INTO section(urn, title, book_id) VALUES(?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS).use { stmt ->

            sections.forEach {
                stmt.setString(1, it.urn)
                stmt.setString(2, it.title)
                stmt.setLong(3, bookId)
                stmt.addBatch()
            }

            stmt.executeBatch()
            stmt.getGeneratedIds().zip(sections).forEach {
                (id, section) -> section.id = id
            }
        }
    }

    private fun saveParagraphs(conn: Connection, sections: List<Section>) {
        conn.prepareStatement("INSERT INTO paragraph(section_id, content) VALUES(?, ?)",
            Statement.RETURN_GENERATED_KEYS).use { stmt ->

            sections.forEach { section ->
                section.paragraphs.forEach {
                    stmt.setLong(1, section.id!!)
                    stmt.setString(2, it)
                    stmt.addBatch()
                }
            }

            stmt.executeBatch()
        }
    }

    private fun Statement.getGeneratedIds(): List<Long> {
        val ids = mutableListOf<Long>()
        val rs = generatedKeys
        while (rs.next())
            ids.add(rs.getLong(1))

        return ids
    }

    override fun deleteAll(): Unit =
        dataSource.connection.use { 
            it.createStatement().use { 
                it.executeUpdate("DELETE FROM book")
            }
        }
}