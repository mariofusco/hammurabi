lazy val commonSettings = Seq(
  name := "hammurabi",
  organization := "io.highlandcows",
  version := "1.0",
  scalaVersion := "2.11.5"
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*)

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-core" % "0.9.22",
  "ch.qos.logback" % "logback-classic" % "0.9.22",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "org.scala-lang" % "scala-library" % "2.11.5"
)