package musicbrainzdb

import java.io.{BufferedInputStream, InputStream}
import java.nio.file.{Files, Paths}
import java.sql.DriverManager

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.jooq.impl.DSL
import org.jooq.{DSLContext, SQLDialect}
import org.postgresql.copy.CopyManager
import org.postgresql.core.BaseConnection

import scala.io.Source

object Postgres {

  lazy val conn: BaseConnection = DriverManager.getConnection(
    "jdbc:postgresql://localhost/musicbrainz_db", "musicbrainz", "musicbrainz").
      asInstanceOf[BaseConnection]

  implicit lazy val ctx: DSLContext = DSL.using(conn, SQLDialect.POSTGRES_9_5)

  def dropSchema(settings: Settings): Unit = {
    ctx.execute(s"DROP SCHEMA IF EXISTS ${settings.dbSchema} CASCADE")
  }

  def createTables(settings: Settings): Unit = {
    ctx.execute(s"CREATE SCHEMA ${settings.dbSchema}")
    ctx.execute(s"SET search_path TO ${settings.dbSchema}, public")
    runSqlFromFile(settings, settings.tableUrl.name)
  }

  def loadTablesFromDump(settings: Settings): Unit = {
    settings.dumpNames.foreach { dumpName =>
      val in = new BZip2CompressorInputStream(
        new BufferedInputStream(
          Files.newInputStream(
            Paths.get(settings.dbDumpDir, Downloader.latest, dumpName))))
      val archive = new TarArchiveInputStream(in)
      var currentEntry = archive.getNextTarEntry
      while (currentEntry != null) {
        if (currentEntry.getName.startsWith(s"${Downloader.RemoteDumpDir}/")) {
          val name = currentEntry.getName.substring(s"${Downloader.RemoteDumpDir}/".length)
          println(s"Inserting into $name...")
          Postgres.copyDataIn(name, archive)
        } else {
          println(s"Skipping ${currentEntry.getName}.")
        }
        currentEntry = archive.getNextTarEntry
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
