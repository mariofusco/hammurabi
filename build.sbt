/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
lazy val commonSettings = Seq(
  name := "hammurabi",
  organization := "hammurabi",
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

scalacOptions += "-feature"

scalacOptions += "-deprecation"
