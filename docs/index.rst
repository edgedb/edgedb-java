.. _gel-java-intro:

===========================
Java client library for Gel
===========================

.. toctree:: 
    :maxdepth: 3
    :hidden:

    configuration
    connecting
    datamodeling
    datatypes
    transactions


This is the official Gel Java client, available to other JVM languages as well.

Quickstart
----------

To get started, you will need to setup an Gel project and have an instance
created. For more information regarding how to do this, we recommend going
through the `Quickstart guide <https://www.edgedb.com/docs/intro/quickstart>`_.

Once you have an instance running, you can add the driver dependency to your
project.

.. tabs::

    .. code-tab:: xml-doc
        :caption: Maven

        <dependency>
            <groupId>com.gel</groupId>
            <artifactId>driver</artifactId>
            <version>0.2.1</version>
        </dependency>

    .. code-tab:: groovy
        :caption: Gradle

        implementation 'com.gel:driver'

Once you have the dependency added, you can start using the client. The
following is a simple example of how to connect to an Gel instance and
execute a query:

.. tabs::

    .. code-tab:: java
        :caption: Futures

        import com.gel.driver.GelClientPool;
        import java.util.concurrent.CompletableFuture;

        public class Main {
            public static void main(String[] args) {
                var clientPool = new GelClientPool();

                clientPool.querySingle(String.class, "SELECT 'Hello, Java!'")
                    .thenAccept(System.out::println)
                    .toCompletableFuture().get();
            }
        }

    .. code-tab:: java
        :caption: Reactor

        import com.gel.driver.GelClientPool;
        import reactor.core.publisher.Mono;

        public class Main {
            public static void main(String[] args) {
                var clientPool = new GelClientPool();

                Mono.fromFuture(clientPool.querySingle(String.class, "SELECT 'Hello, Java!'"))
                    .doOnNext(System.out::println)
                    .block();
            }
        }

You can represent schema types with classes, reflecting the properties/links in the 
schema type:

.. tabs:: 

    .. code-tab:: java
        :caption: Code

        @GelType
        public class Person {
            public String name;
            public int age;
        }

        ..

        clientPool.query(Person.class, "SELECT Person { name, age }")
            .thenAccept(result -> {
                for(var person : result) {
                    System.out.println("Person { " + person.name + ", " + person.age + "}");
                }
            });

    .. code-tab:: sdl
        :caption: Schema 2.x

        module default {
            type Person {
                property name -> str;
                property age -> int32;
            }
        }

    .. code-tab:: sdl
        :caption: Schema 3+

        module default {
            type Person {
                name: str;
                age: int32;
            }
        }
        
Learn more about :ref:`data modeling <gel_java_datamodeling>`.

Logging
-------

The java binding uses the `SLF4J <https://www.slf4j.org/>`_ logging facade.
To enable logging, you will need to add a SLF4J implementation to your project,
you can read more about that 
`here <https://www.slf4j.org/manual.html#swapping>`_.