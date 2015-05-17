import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._
import sbt.Keys._

object Common {

  val settings =
    scalariformSettings ++List(
      // Core settings
      name := "rac",
      organization := "grandsys",
      version := "1.0.0",
      scalaVersion := Version.scala
    )
}
