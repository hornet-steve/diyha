name := "diyha_station"

organization := "hornetdevelopment"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.2"

crossScalaVersions := Seq("2.10.4", "2.11.2")

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.0" withSources(),
  "com.datastax.cassandra" % "cassandra-driver-core" % "3.0.1" withSources(),
  "org.scream3r" % "jssc" % "2.8.0" withSources(),
  "org.json4s" % "json4s-native_2.11" % "3.3.0" withSources(),
  "org.scalatest" %% "scalatest" % "2.2.1" % "test" withSources(),
  "org.scalacheck" %% "scalacheck" % "1.11.5" % "test" withSources()
)
