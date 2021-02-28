package io.github.repository

import java.sql.Connection

fun initData(connection: Connection) {
    if (isInMemoryDatabase(connection))
        connection.createStatement().use { 
            it.executeUpdate("RUNSCRIPT FROM 'classpath:data.sql'")
        }
}

private fun isInMemoryDatabase(connection: Connection) =
    connection.createStatement().use { 
         it.executeQuery("CALL DATABASE_PATH()").run { 
            next()
            getString(1) == null
         }
    }