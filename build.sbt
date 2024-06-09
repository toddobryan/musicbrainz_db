import java.io.PrintWriter
import java.nio.file.Files

val commonsCompressVersion = "1.26.1"
val commonsCodecVersion = "1.17.0"
val commonsNetVersion = "3.11.0"
val postgresqlVersion = "42.7.3"
val jooqVersion = "3.19.8"

val packageName = "musicbrainzdb"
val mainClassName = "InstallDb"
val jar = s"$mainClassName.jar"

val cronFolderName = "musicbrainz-update"

lazy val writeToCronFolder = taskKey[Unit]("Writes InstallDb.jar to update-musicbrainz folder")
lazy val cronFolder = taskKey[File]("path to cronFolder")

val bashFileContents =
  """#!/bin/bash
    |cd /home/toddobryan/musicbrainz-update
    |java -jar InstallDb.jar
    |""".stripMargin


lazy val root = (project in file(".")).settings(
  name := "musicbrainz_db_only",
  version := "0.1-SNAPSHOT",
  scalaVersion := "3.4.2",
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
  ),

  libraryDependencies ++= Seq(
    "org.apache.commons" % "commons-compress" % commonsCompressVersion,
    "commons-codec" % "commons-codec" % commonsCodecVersion,
    "commons-net" % "commons-net" % commonsNetVersion,

    "org.postgresql" % "postgresql" % postgresqlVersion,

    "org.jooq" % "jooq" % jooqVersion
  ),

  Compile / packageBin / mainClass := Some(s"$packageName.$mainClassName"),
  assembly / mainClass := (Compile / packageBin / mainClass).value,
  assembly / assemblyJarName := jar,

  cronFolder := {
    val folder = file(System.getProperty("user.home")) / cronFolderName
    println(folder.toString())
    if (!folder.exists()) {
      if (!folder.mkdir()) println("Folder couldn't be created")
    }
    folder
  },


  writeToCronFolder := {
    assembly.value
    val folder = cronFolder.value
    val resources = folder / "resources"
    if (!resources.exists()) {
      Files.createSymbolicLink(resources.asPath, (baseDirectory.value / "resources").asPath)
    }
    Files.copy((baseDirectory.value / "target" / "scala-2.13" / jar).asPath, (folder / jar).asPath)
    val bashFile = folder / "musicbrainz_update.sh"
    new PrintWriter(bashFile.getAbsolutePath) {
      write(bashFileContents)
      close()
    }
    bashFile.setExecutable(true)
  }
)

