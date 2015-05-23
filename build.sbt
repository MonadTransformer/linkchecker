organization := "info.rkuhn"

version := "0.1"

scalaVersion := "2.11.6"

val vAkka = "2.3.11"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.ning" % "async-http-client" % "1.9.24",
  "com.typesafe.akka" %% "akka-actor"   % vAkka,
  "com.typesafe.akka" %% "akka-cluster" % vAkka,
  "com.typesafe.akka" %% "akka-testkit" % vAkka,
  "org.jsoup" % "jsoup" % "1.8.2",
  "org.scalatest" %% "scalatest" % "2.2.5" % "test")
