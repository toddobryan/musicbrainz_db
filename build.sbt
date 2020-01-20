import java.nio.file.Files

val commonsCompressVersion = "1.19"
val commonsCodecVersion = "1.14"
val commonsNetVersion = "3.6"
val postgresqlVersion = "42.2.9"
val jooqVersion = "3.12.3"

val packageName = "musicbrainzdb"
val mainClassName = "InstallDb"
val jar = s"$mainClassName.jar"

val cronFolderName = "musicbrainz-update"

name := "musicbrainz_db_only"

version := "0.1"

scalaVersion := "2.13.1"

scalacOptions ++= Seq(
  "-Xlint",
  "-feature",
  "-deprecation",
  "-Ywarn-unused:implicits",
  "-Ywarn-unused:imports",
  "-Ywarn-unused:locals",
  "-Ywarn-unused:privates"
)

libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-compress" % commonsCompressVersion,
  "commons-codec" % "commons-codec" % commonsCodecVersion,
  "commons-net" % "commons-net" % commonsNetVersion,

  "org.postgresql" % "postgresql" % postgresqlVersion,

  "org.jooq" % "jooq" % jooqVersion
)

mainClass in (Compile, packageBin) := Some(s"$packageName.$mainClassName")
mainClass in assembly := (mainClass in (Compile, packageBin)).value
assemblyJarName in assembly := jar

lazy val writeToCronFolder = taskKey[Unit]("Writes InstallDb.jar to update-musicbrainz folder")
lazy val cronFolder = taskKey[File]("path to cronFolder")

cronFolder := {
  val folder = file(System.getProperty("user.home")) / cronFolderName
  println(folder.toString())
  if (!folder.exists()) {
    if (!folder.mkdir()) println("Folder couldn't be created")
  }
  folder
}

writeToCronFolder := {
  assembly.value
  val resources = cronFolder.value / "resources"
  if (!resources.exists()) {
    Files.createSymbolicLink(resources.asPath, (baseDirectory.value / "resources").asPath)
  }
  Files.copy((baseDirectory.value / "target" / "scala-2.13" / jar).asPath, ((cronFolder.value / jar)).asPath)
}