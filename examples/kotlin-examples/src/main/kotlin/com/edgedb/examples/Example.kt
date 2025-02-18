package com.edgedb.examples

import com.edgedb.driver.GelClientPool

interface Example {
    suspend fun runAsync(clientPool: GelClientPool)
}