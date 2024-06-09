package musicbrainzdb

import java.io.{BufferedInputStream, InputStream}
import java.nio.file.{Files, Paths}
import java.sql.{Driver, DriverManager}
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.jooq.impl.DSL
import org.jooq.{DSLContext, SQLDialect}
import org.postgresql.copy.CopyManager
import org.postgresql.core.BaseConnection

import java.util.ServiceLoader
import scala.io.Source

object Postgres {

  lazy val conn: BaseConnection = DriverManager.getConnection(
    "jdbc:postgresql://localhost/musicbrainz",
    "musicbrainz",
    "musicbrainz",
  ).asInstanceOf[BaseConnection]

  implicit lazy val ctx: DSLContext = DSL.using(conn, SQLDialect.POSTGRES)

  def dropSchema(settings: Settings): Unit = {
    ctx.execute(s"DROP SCHEMA IF EXISTS ${settings.dbSchema} CASCADE")
    ctx.execute(s"CREATE SCHEMA ${settings.dbSchema}")
    ctx.execute(s"SET search_path TO ${settings.dbSchema}, public")
  }
  
  def createSearchConfig(settings: Settings): Unit = settings.searchConfigUrl.foreach { sc =>
    runSqlFromFile(settings, sc.name)
  }

  def createExtensions(settings: Settings): Unit = settings.extensionsUrl.foreach { eu =>
    runSqlFromFile(settings, eu.name)
  }

  def createCollations(settings: Settings): Unit = settings.collationsUrl.foreach { cu =>
    runSqlFromFile(settings, cu.name)
  }

  def createTypes(settings: Settings): Unit =
    List(
      "cover_art_presence", "edit_note_status", "event_art_presence",
      "fluency", "oauth_code_challenge_method",
      "ratable_entity_type", "taggable_entity_type"
    ).foreach { tpe =>
      ctx.execute(s"DROP TYPE IF EXISTS $tpe CASCADE")
    }
    settings.typesUrl.foreach { tu =>
      runSqlFromFile(settings, tu.name)
    }

  def createTables(settings: Settings): Unit = {
    runSqlFromFile(settings, settings.tableUrl.name)
  }

  def loadTablesFromDump(settings: Settings): Unit = {
    settings.dumpNames.foreach { dumpName =>
      val in = new BZip2CompressorInputStream(
        new BufferedInputStream(
          Files.newInputStream(
            Paths.get(settings.dbDumpDir, Downloader.latest, dumpName))))
      val archive = new TarArchiveInputStream(in)
      var currentEntry = archive.getNextEntry
      while (currentEntry != null) {
        if (currentEntry.getName.startsWith(s"${Downloader.RemoteDumpDir}/")) {
          val name = currentEntry.getName.substring(s"${Downloader.RemoteDumpDir}/".length)
          println(s"Inserting into $name...")
          Postgres.copyDataIn(name, archive)
        } else {
          println(s"Skipping ${currentEntry.getName}.")
        }
        currentEntry = archive.getNextEntry
      }
      println("Done.")
      archive.close()
    }
  }

  def updateIndexes(settings: Settings): Unit = {
    println("  Updating functions...")
    runSqlFromFile(settings, settings.funcUrl.name)
    println("  Updating indexes...")
    runSqlFromFile(settings, settings.indexUrl.name)
    println("  Updating primary keys...")
    runSqlFromFile(settings, settings.pkUrl.name)
  }

  def updateForeignKey(settings: Settings): Unit = {
    runSqlFromFile(settings, settings.fkUrl.name)
  }

  def runSqlFromFile(settings: Settings, filename: String): Unit = {
    val sql = Source.fromFile(s"${settings.sqlLocation}/$filename").mkString
    ctx.execute(sql)
  }


  def copyDataIn(tableName: String, inputStream: InputStream): Unit = {
    val copyManager = new CopyManager(conn)
    val numRows = copyManager.copyIn(s"COPY $tableName FROM STDIN", inputStream)
    println(s"Updated $numRows rows.")
  }
}
/*
@main
def main(): Unit =
  val loadedDrivers: ServiceLoader[Driver] = ServiceLoader.load(classOf[Driver])
  loadedDrivers.forEach(println(_))
  val conn: BaseConnection = DriverManager.getConnection(
    "jdbc:postgresql://localhost/musicbrainz",
    "musicbrainz",
    "musicbrainz",
  ).asInstanceOf[BaseConnection]
  println(conn)
*/