name := "moviereservation"

version := "0.1"

scalaVersion := "2.12.2"

mainClass in (Compile, run) := Some("com.taintech.moviereservation.Main")

libraryDependencies ++= {
  val akkaVersion = "2.5.2"
  val akkaHttpVersion = "10.0.7"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "org.iq80.leveldb" % "leveldb" % "0.7",
    "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
    "org.jsoup" % "jsoup" % "1.10.2",
    "org.scalatest" %% "scalatest" % "3.0.1" % Test,
    "org.mockito" % "mockito-core" % "2.8.9" % Test,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
    "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.1.1" % Test
  )
}
