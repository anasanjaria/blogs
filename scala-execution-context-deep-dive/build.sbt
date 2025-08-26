ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "execution-context-deep-dive"
  )

libraryDependencies ++= Seq(
  "com.google.guava" % "guava" % "32.1.3-jre",
  "org.scalatest" %% "scalatest" % "3.2.10" % Test
)
