name := "ReactiveScalaGitlab"

version := "0.1"

scalaVersion := "2.12.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.18",
  "com.typesafe.akka" % "akka-testkit_2.12" % "2.5.18",
  "org.scalatest" % "scalatest_2.12" % "3.0.4" % "test",
  "com.typesafe.akka" %% "akka-persistence" % "2.5.18",
  "org.iq80.leveldb" % "leveldb" % "0.9",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "com.typesafe.akka" %% "akka-stream" % "2.5.18",
  "com.typesafe.akka" %% "akka-remote" % "2.5.18",
  "com.typesafe.akka" %% "akka-http" % "10.0.10",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.10")
