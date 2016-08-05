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
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.8",
  "org.apache.httpcomponents" % "httpclient" % "4.5.2",
  "org.apache.httpcomponents" % "httpcore" % "4.4.5",
  "commons-validator" % "commons-validator" % "1.4.0",
  "com.kohlschutter.boilerpipe" % "boilerpipe-common" % "2.0-SNAPSHOT",
  "org.jsoup" % "jsoup" % "1.9.2",
  "org.scalactic" %% "scalactic" % "3.0.0",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test"
)

resolvers ++= Seq(
  "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/",
  "Artima Maven Repository" at "http://repo.artima.com/releases",
  Resolver.sonatypeRepo("releases"),
  Resolver.mavenLocal,
  Resolver.defaultLocal
)

logBuffered in Test := false

mainClass in assembly := Some("Main")

//retrieveManaged := true

test in assembly := {}

assemblyMergeStrategy in assembly := {
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
