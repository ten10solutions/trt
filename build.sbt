import com.typesafe.sbt.packager.Keys._

import PlayKeys._

lazy val root = (project in file(".")).enablePlugins(PlayScala)

name := "trt"

organization := "com.thetestpeople"

version := "0.1.0-SNAPSHOT"

scalacOptions ++= List("-deprecation", "-feature")

scalaVersion := "2.11.1"

libraryDependencies ++= List(
  ws,
  jdbc,
  "com.google.guava" % "guava" % "16.0.1",
  "com.google.code.findbugs" % "jsr305" % "2.0.3", // Workaround for scalac/Guava clash, SI-7751 
  "com.typesafe.slick" %% "slick" % "2.1.0",
  "com.h2database" % "h2" % "1.3.166",
  "postgresql" % "postgresql" % "9.1-901.jdbc4",
  "com.ocpsoft" % "ocpsoft-pretty-time" % "1.0.7",
  "joda-time" % "joda-time" % "2.3",
  "org.pegdown" % "pegdown" % "1.0.2", // Needed for error when generating report: https://groups.google.com/forum/#!topic/scalatest-users/6TGms7jYn6E
  "org.joda" % "joda-convert" % "1.5",
  "com.github.tototoshi" %% "slick-joda-mapper" % "1.2.0",
  "com.github.nscala-time" %% "nscala-time" % "1.4.0",
  "commons-validator" % "commons-validator" % "1.4.0",
  "commons-io" % "commons-io" % "2.2",
  "org.apache.lucene" % "lucene-core" % "4.9.0",
  "org.apache.lucene" % "lucene-analyzers-common" % "4.9.0",
  "org.apache.lucene" % "lucene-highlighter" % "4.9.0",
  "org.apache.lucene" % "lucene-queryparser" % "4.9.0",
  "com.google.guava" % "guava" % "16.0.1",
  "org.liquibase" % "liquibase-core" % "3.1.1",
  "oro" % "oro" % "2.0.8")

libraryDependencies ++= List(
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.seleniumhq.selenium" % "selenium-java" % "2.43.1" % "test",
  "com.github.detro.ghostdriver" % "phantomjsdriver" % "1.1.0" % "test")

routesImport ++= List(
  "extensions.Binders._",
  "extensions.Aliases._",
  "com.thetestpeople.trt.model._",
  "com.thetestpeople.trt.model.jenkins._",
  "java.net.URI")

TwirlKeys.templateImports ++= Seq(
  "viewModel._", 
  "com.thetestpeople.trt.model._",
  "com.thetestpeople.trt.model.jenkins._",
  "java.net.URI",
  "com.thetestpeople.trt.analysis._")

// Eclipse integration:

EclipseKeys.withSource := true

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

EclipseKeys.eclipseOutput := Some("bin")

// Change resources dir from default for easy integration with Eclipse
resourceDirectory in Test <<= baseDirectory(_ / "testResources")

// Override specs2 options
// & Stop problem with tests executing twice because of "JUnitRunner" annotation:

(testOptions in Test) := Seq(Tests.Argument(TestFrameworks.JUnit, "--ignore-runners=org.scalatest.junit.JUnitRunner")) 

(testOptions in Test) += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/report")

testFrameworks := Seq(TestFrameworks.ScalaTest)

fork in Test := false

// Deb packaging:

packageDescription := "Reporting and analysis of automated test results"

packageSummary := "Test Reporty Thing (trt)"

maintainer := "Matt Russell <MattRussellUK@gmail.com>"

debianPackageDependencies in Debian ++= Seq("default-jre-headless (>= 1.7)")

linuxPackageMappings in Debian <+= (baseDirectory) map { bd =>
  packageMapping(
   (bd / "emptyDir") -> "/var/lib/trt"
  ) withUser "trt" withGroup "trt" withPerms "0755"
}
