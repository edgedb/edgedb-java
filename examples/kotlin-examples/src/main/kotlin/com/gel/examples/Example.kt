package com.gel.examples

import com.gel.driver.GelClientPool

interface Example {
    suspend fun runAsync(clientPool: GelClientPool)
}