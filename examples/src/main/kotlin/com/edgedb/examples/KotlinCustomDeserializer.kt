package com.edgedb.examples

import com.edgedb.driver.EdgeDBClient
import com.edgedb.driver.annotations.EdgeDBDeserializer
import com.edgedb.driver.annotations.EdgeDBName
import com.edgedb.driver.annotations.EdgeDBType
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory

class KotlinCustomDeserializer : KotlinExample {
    companion object {
        private val logger = LoggerFactory.getLogger(KotlinCustomDeserializer::class.java)!!
    }

    @EdgeDBType
    data class Person
        @EdgeDBDeserializer
        constructor (
                @EdgeDBName("name")
                val name: String,
                @EdgeDBName("age")
                val age: Long
        ) {
            init {
                logger.info("Custom deserializer called with: name: {}, age: {}", name, age)
            }
    }

    override suspend fun runAsync(client: EdgeDBClient) {
        val person = client.queryRequiredSingle(
                Person::class.java,
                """
                    insert Person { name := 'Example', age := 123 } unless conflict on .name;
                    select Person { name, age } filter .name = 'Example'
                """.trimIndent()
        ).await()

        logger.info("Got person: {}", person)
    }
}