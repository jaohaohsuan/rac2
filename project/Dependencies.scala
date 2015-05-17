import sbt.Keys._
import sbt._

object Version {
  val akka         = "2.3.11"
  val logback      = "1.1.2"
  val scala        = "2.11.6"
  val scalaTest    = "2.2.1"
  val spray        = "1.3.2"
}

object Library {
  val akkaActor                = "com.typesafe.akka"      %% "akka-actor"                       % Version.akka
  val akkaSlf4j                = "com.typesafe.akka"      %% "akka-slf4j"                       % Version.akka
  val akkaPersistence          = "com.typesafe.akka"      %% "akka-persistence-experimental"    % Version.akka
  val akkaCluster              = "com.typesafe.akka"      %% "akka-cluster"                     % Version.akka
  val akkaContrib              = "com.typesafe.akka"      %% "akka-contrib"                     % Version.akka
  val akkaTestkit              = "com.typesafe.akka"      %% "akka-testkit"                     % Version.akka
  val akkaMultiNodeTestkit     = "com.typesafe.akka"      %% "akka-multi-node-testkit"          % Version.akka
  val spray                    = "io.spray"               %% "spray-can"                        % Version.spray
  val sprayRouting             = "io.spray"               %% "spray-routing"                    % Version.spray
  val sprayJson                = "io.spray"               %% "spray-json"                       % "1.2.6"
  val sprayTestkit             = "io.spray"               %% "spray-testkit"                    % Version.spray
  val logbackClassic           = "ch.qos.logback"         %  "logback-classic"                  % Version.logback
  val scalaTest                = "org.scalatest"          %% "scalatest"                        % Version.scalaTest
  val commonsIo                = "commons-io"             %  "commons-io"                       % "2.4"
  val elastic4s                = "com.sksamuel.elastic4s" %% "elastic4s"                        % "1.5.6"
  val nscalaTime               = "com.github.nscala-time" %% "nscala-time"                      % "2.0.0"
  val hashids                  = "com.timesprint"         %% "hashids-scala"                    % "1.0.0"
  val persistenceShardInmemory = "com.github.jdgoldie"    %% "akka-persistence-shared-inmemory" % "1.0.16"
  val json4sNative             = "org.json4s"             %% "json4s-native"                    % "3.2.10"


}

object Dependencies {

  import Library._

  val resolvers = Seq(
      "Spray Repository"    at "http://repo.spray.io/",
      "jdgoldie at bintray" at "http://dl.bintray.com/jdgoldie/maven"
  )

  val cqrsTemplate = List(
    akkaActor,
    akkaPersistence,
    akkaCluster,
    akkaContrib,
    akkaSlf4j,
    spray,
    sprayRouting,
    sprayJson,
    logbackClassic,
    elastic4s,
    nscalaTime,
    hashids,
    persistenceShardInmemory,
    json4sNative,
    akkaTestkit % "test",
    akkaMultiNodeTestkit % "test",
    sprayTestkit % "test",
    scalaTest   % "test",
    commonsIo   % "test"
  )
}
