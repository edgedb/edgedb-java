ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.3"

libraryDependencies ++= Seq(
  "com.edgedb" % "driver" % "0.2.3" from "file:///" + System.getProperty("user.dir") + "/lib/com.edgedb.driver-0.2.3.jar",
  "ch.qos.logback" % "logback-classic" % "1.4.7",
  "ch.qos.logback" % "logback-core" % "1.4.7",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.15.1",
  "io.netty" % "netty-all" % "4.1.89.Final",
  "org.jooq" % "joou" % "0.9.4",
  "org.reflections" % "reflections" % "0.10.2"
)

lazy val root = (project in file("."))
  .settings(
    name := "scala-examples",
    idePackagePrefix := Some("com.edgedb.examples")
  )
