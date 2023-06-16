package com.edgedb.examples

import com.edgedb.driver.EdgeDBClient

interface Example {
    suspend fun runAsync(client: EdgeDBClient)
}