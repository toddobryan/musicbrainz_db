package musicbrainzdb

trait Settings {
  val remoteBaseUrl: String
  val sqlLocation: String
  val dbSchema: String
  val dumpNames: List[String]
  val dbDumpDir: String = "resources/db_dumps"

  val tableUrl: RemoteFile
  val fkUrl: RemoteFile
  val indexUrl: RemoteFile
  val pkUrl: RemoteFile
  val funcUrl: RemoteFile

  def dropFirstLine(str: String): String = raw"[^\n]*\n".r.replaceFirstIn(str, "")

  def dropFirstTwoLines(str: String): String = dropFirstLine(dropFirstLine(str))

  def ignoreForeignKey(tableName: String): (String => String) = {
    def dropStatements(str: String): String =
      raw"\n\n((?!\n\n)[\s\S])*FOREIGN KEY \(editor\)((?!\n\n)[\s\S])*".r.replaceAllIn(str, "")
    dropStatements
  }
}

class MusicbrainzSettings extends Settings {
  val dbName = "musicbrainz_db"
  val dbUser = "musicbrainz"
  val dbPassword = "musicbrainz"
  val dbSchema = "musicbrainz"

  val remoteBaseUrl: String =
    "https://raw.githubusercontent.com/metabrainz/musicbrainz-server/master/admin/sql"

  val tableUrl: RemoteFile = RemoteFile("CreateTables.sql", "jRn5/BywQaV3cTYIYT9HuQ==",
      dropFirstLine)
  val fkUrl: RemoteFile = RemoteFile("CreateFKConstraints.sql", "8jzVysqOMt2beKYIpLSsYQ==",
      dropFirstTwoLines, ignoreForeignKey("editor"), ignoreForeignKey("owner"))
  val indexUrl: RemoteFile = RemoteFile("CreateIndexes.sql", "vSHASWxaBqaQBSOB9vyyGA==",
      dropFirstLine)
  val pkUrl: RemoteFile = RemoteFile("CreatePrimaryKeys.sql", "pkG2t4TJYbKJPY95UDlcjg==",
      dropFirstTwoLines)
  val funcUrl: RemoteFile = RemoteFile("CreateFunctions.sql", "7bwhQ4nkPaTE1Asgs1G92g==",
      dropFirstLine)
  val remoteSqlFiles: Seq[RemoteFile] = List(tableUrl, fkUrl, indexUrl, pkUrl, funcUrl)

  val sqlLocation = "resources/sql"
  val dumpNames = List("mbdump.tar.bz2", "mbdump-derived.tar.bz2")
}

object MusicbrainzSettings extends MusicbrainzSettings

object CoverArtArchiveSettings extends Settings {
  val dbSchema = "cover_art_archive"

  val remoteBaseUrl: String = MusicbrainzSettings.remoteBaseUrl + "/caa"

  val tableUrl: RemoteFile = RemoteFile("CreateTables.sql", "7XJKwltxczlvnuvTLlB1Cg==",
    dropFirstLine)
  val fkUrl: RemoteFile = RemoteFile("CreateFKConstraints.sql", "Z5L8SZvbX+EQ3got2qurQw==",
    dropFirstTwoLines)
  val indexUrl: RemoteFile = RemoteFile("CreateIndexes.sql", "jfK30ApBo4qps2pPOj4jJw==",
    dropFirstLine)
  val pkUrl: RemoteFile = RemoteFile("CreatePrimaryKeys.sql", "UtLFdiwEOeoxhGTDsLhmfQ==",
    dropFirstTwoLines)
  val funcUrl: RemoteFile = RemoteFile("CreateFunctions.sql", "E2r62HDqTLykhG19mYxwcw==",
    dropFirstLine)
  val remoteSqlFiles: Seq[RemoteFile] = List(tableUrl, fkUrl, indexUrl, pkUrl, funcUrl)

  val sqlLocation: String = MusicbrainzSettings.sqlLocation + "/caa"

  val dumpNames = List("mbdump-cover-art-archive.tar.bz2")
}

