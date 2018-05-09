name := "musicbrainz_db_only"

version := "0.1"

scalaVersion := "2.12.6"

scalacOptions ++= Seq(
  "-Xlint",
  "-feature",
  "-deprecation"
)

libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-compress" % "1.16.1",
  "commons-codec" % "commons-codec" % "1.11",
  "commons-net" % "commons-net" % "3.6",

  "org.postgresql" % "postgresql" % "42.2.2",

  "org.jooq" % "jooq" % "3.10.6"
)