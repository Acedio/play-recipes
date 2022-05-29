name := """givery-recipes"""
organization := "com.simentco"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.8"

libraryDependencies ++= Seq(
  "org.playframework.anorm" %% "anorm" % "2.6.10",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test,
  caffeine,
  guice,
  jdbc
)

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.simentco.binders._"
