name := "HikariCPTimeoutDemo"

version := "1.0"

scalaVersion := "2.13.8"

libraryDependencies ++= Seq(
  "com.zaxxer" % "HikariCP" % "4.0.3",
  "org.postgresql" % "postgresql" % "42.7.6",
  "com.google.guava" % "guava" % "33.4.8-jre",
  "org.scalatest" %% "scalatest" % "3.2.19" % Test
)