name := "PersonalCalendar"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.19",
  "com.typesafe.akka" %% "akka-stream" % "2.5.19",
  "com.typesafe.akka" %% "akka-http" % "10.1.3",
  "org.postgresql" % "postgresql" % "42.1.4",
  "com.typesafe.slick" %% "slick" % "3.2.3",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.0-RC1",
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.9",
  "ch.qos.logback" % "logback-classic" % "1.0.9",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.2.3",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.7",
  "org.scalatest" % "scalatest_2.12" % "3.0.5" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.19"
)



