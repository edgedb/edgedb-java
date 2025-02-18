package com.edgedb.examples

import com.edgedb.driver.GelClientPool
import com.edgedb.driver.annotations.EdgeDBLinkType
import com.edgedb.driver.annotations.EdgeDBType
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory

class LinkProperties : Example {
    companion object {
        private val logger = LoggerFactory.getLogger(LinkProperties::class.java)
        private const val INSERT_QUERY = """
            with 
                a := (insert Person { name := 'Person A', age := 20 } unless conflict on .name),
                b := (insert Person { name := 'Person B', age := 21 } unless conflict on .name),
                c := (insert Person { name := 'Person C', age := 22, friends := b } unless conflict on .name)
            insert Person { 
                name := 'Person D', 
                age := 23,
                friends := { 
                    a, 
                    b, 
                    c 
                },
                best_friend := c 
            } unless conflict on .name
        """
    }

    @EdgeDBType
    class Person {
        var name: String? = null
        var age: Long? = null
        var bestFriend: Person? = null

        @EdgeDBLinkType(Person::class)
        var friends: Collection<Person>? = null
    }

    override suspend fun runAsync(clientPool: GelClientPool) {
        clientPool.execute(INSERT_QUERY).await()

        val result = clientPool.queryRequiredSingle(
                Person::class.java,
                """
                    select Person { 
                        name, 
                        age, 
                        friends: { 
                            name, 
                            age, 
                            friends 
                        }, 
                        best_friend: { 
                            name, 
                            age, 
                            friends 
                        } 
                    } filter .name = 'Person D'
                """.trimIndent()
        ).await()

        logger.info("Person with links: {}", result)
    }
}