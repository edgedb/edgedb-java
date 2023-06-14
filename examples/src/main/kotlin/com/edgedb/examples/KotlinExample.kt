package com.edgedb.examples

import com.edgedb.driver.EdgeDBClient

interface KotlinExample {
    suspend fun runAsync(client: EdgeDBClient)
}