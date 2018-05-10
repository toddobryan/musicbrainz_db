package musicbrainzdb

import java.io.{BufferedInputStream, InputStream}
import java.nio.file.{Files, Paths}
import java.sql.DriverManager

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.jooq.impl.DSL._
import org.jooq.impl.{DSL, SQLDataType}
import org.jooq.{DSLContext, SQLDialect}
import org.postgresql.copy.CopyManager
import org.postgresql.core.BaseConnection

import scala.io.Source

object Postgres {

  lazy val conn: BaseConnection = DriverManager.getConnection(
    "jdbc:postgresql://localhost/musicbrainz_db", "musicbrainz", "musicbrainz").
      asInstanceOf[BaseConnection]

  implicit lazy val ctx: DSLContext = DSL.using(conn, SQLDialect.POSTGRES_9_5)

  def tableExists(tableName: String): Boolean = {
    ctx.select(count())
        .from("information_schema.tables")
        .where(s"table_name = ${inline(tableName)}")
        .fetchOne(0, classOf[Int]) == 1
  }

  def createExtraTables(): Unit = {
    val tableName = "key_value_table"
    if (!tableExists(tableName)) {
      ctx.createTable(tableName)
          .column("name", SQLDataType.VARCHAR.length(255))
          .column("value", SQLDataType.CLOB)
          .constraints(constraint("PK_NAME").primaryKey("name"))
          .execute();
    }
  }

  def createTables(): Unit = {
    ctx.execute("CREATE SCHEMA musicbrainz")
    ctx.execute("SET search_path TO musicbrainz, metadata, public")
    runSqlFromFile(Settings.tableUrl.name)
  }

  def loadTablesFromDump(): Unit = {
    val in = new BZip2CompressorInputStream(
      new BufferedInputStream(
        Files.newInputStream(
          Paths.get(Settings.dbDumpLocal, Downloader.latest, Downloader.MainDump))))
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

  def updateIndexes(): Unit = {
    println("  Updating functions...")
    runSqlFromFile(Settings.funcUrl.name)
    println("  Updating indexes...")
    runSqlFromFile(Settings.indexUrl.name)
    println("  Updating primary keys...")
    runSqlFromFile(Settings.pkUrl.name)
  }

  def updateForeignKey(): Unit = {
    runSqlFromFile(Settings.fkUrl.name)
  }

  def runSqlFromFile(filename: String): Unit = {
    val sql = Source.fromFile(s"${Settings.sqlLocation}/$filename").mkString
    ctx.execute(sql)
  }


  def copyDataIn(tableName: String, inputStream: InputStream): Unit = {
    val copyManager = new CopyManager(conn)
    val numRows = copyManager.copyIn(s"COPY $tableName FROM STDIN", inputStream)
    println(s"Updated $numRows rows.")
  }
}
