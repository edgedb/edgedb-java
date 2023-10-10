![EdgeDB Java](./branding/banner.png)

<div align="center">
  <h1>☕ The official Java/JVM client library for EdgeDB ☕</h1>

  <a href="https://github.com/edgedb/edgedb-java/actions" rel="nofollow">
    <img src="https://github.com/edgedb/edgedb-java/actions/workflows/gradle.yml/badge.svg?event=push&branch=master" alt="Build status">
  </a>
  <a href="https://github.com/edgedb/edgedb/blob/master/LICENSE">
    <img src="https://img.shields.io/badge/license-Apache%202.0-blue" />
  </a>
  <a href="https://discord.gg/edgedb">
    <img src="https://discord.com/api/guilds/841451783728529451/widget.png" alt="Discord">
  </a>
</div>

## Installation

The Java binding is distrubuted via maven central:

#### Gradle
```groovy
implementation 'com.edgedb:driver:0.2.2'
```

#### Maven
```xml
<dependency>
  <groupId>com.edgedb</groupId>
  <artifactId>driver</artifactId>
  <version>0.2.2</version>
</dependency>
```

#### SBT

```scala
libraryDependencies ++= Seq(
  "com.edgedb" % "driver" % "0.2.2"
)
```

## Usage

The `EdgeDBClient` class contains all the methods necessary to interact with the EdgeDB database.

```java
import com.edgedb.driver.EdgeDBClient;

void main() {
    var client = new EdgeDBClient();

    client.query(String.class, "SELECT 'Hello, Java!'")
        .thenAccept(System.out::println);
}
```

The `EdgeDBClient` uses `CompletionState` for asynchronous operations, allowing you
to integrate it with your favorite asynchronous frameworks

```java
import com.edgedb.driver.EdgeDBClient;
import reactor.core.publisher.Mono;

void main() {
    var client = new EdgeDBClient();

    Mono.fromFuture(client.querySingle(String.class, "SELECT 'Hello, Java!'"))
        .doOnNext(System.out::println)
        .block();
}
```

This also means it plays nicely with other JVM language that support asynchronous programming via `CompletionStage`

```kotlin

import com.edgedb.driver.EdgeDBClient
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking

fun main() {
    val client = EdgeDBClient()

    runBlocking {
        client.querySingle(String::class.java, "SELECT 'Hello, Kotlin!'")
            .thenAccept { println(it) }
            .await()
    }
}
```

```scala

import com.edgedb.driver.EdgeDBClient
import scala.jdk.FutureConverters.*

object Main extends App {
  val client = new EdgeDBClient()

  client.querySingle(classOf[String], "SELECT 'Hello, Scala!'")
    .asScala
    .map(println)
}
```

## Examples
Some examples of using the Java client api can be found in the [examples](./examples) directory.

## Compiling
This project uses gradle. To build the project run the following command:

```bash
./gradlew build
```
