name := """silawet-web"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava, PlayEbean)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  javaCore,
  javaJdbc,
  cache,
  javaWs,
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "rome" % "rome" % "1.0",
  "org.bouncycastle" % "bcprov-jdk16" % "1.45"
)
