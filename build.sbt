organization := "com.freeheap"

name := "akkawl"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.8",
  "com.freeheap" %% "drawler" % "1.0-SNAPSHOT",
  "edu.uci.ics" % "crawler4j" % "4.2",
  "com.datastax.cassandra" % "cassandra-driver-core" % "3.0.2",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.8"
)

resolvers ++= Seq(
  "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/",
  Resolver.sonatypeRepo("releases"),
  Resolver.mavenLocal,
  Resolver.defaultLocal
)

mainClass in assembly := Some("Main")

//retrieveManaged := true

test in assembly := {}

assemblyMergeStrategy in assembly := {
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
