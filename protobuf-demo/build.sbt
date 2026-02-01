import xerial.sbt.pack.PackPlugin._

name := "ProtobufDemo"

version := "1.0"

scalaVersion := "2.13.16"

val commonSettings = Seq(
  scalaVersion := "2.13.16",
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.2.19" % Test,
    "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
  ),
  Compile / PB.targets := Seq(
    scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
  )
)

lazy val root = (project in file("."))
  .aggregate(producer, consumer)
  .settings(
    name := "ProtobufDemo"
  )

lazy val producer = (project in file("producer"))
  .settings(
    commonSettings,
    name := "producer",
    packSettings,
    packMain := Map("producer" -> "ProducerApp")
  )

lazy val consumer = (project in file("consumer"))
  .settings(
    commonSettings,
    name := "consumer",
    packSettings,
    packMain := Map("consumer" -> "ConsumerApp")
  )
