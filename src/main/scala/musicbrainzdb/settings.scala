package musicbrainzdb

trait Settings {
  val remoteBaseUrl: String
  val sqlLocation: String
  val dbSchema: String
  val dumpNames: List[String]
  val dbDumpDir: String = "resources/db_dumps"

  val searchConfigUrl: Option[RemoteFile] = None
  val extensionsUrl: Option[RemoteFile] = None
  val collationsUrl: Option[RemoteFile] = None
  val typesUrl: Option[RemoteFile]
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
  val dbName = "musicbrainz"
  val dbUser = "musicbrainz"
  val dbPassword = "musicbrainz"
  val dbSchema = "musicbrainz"

  val remoteBaseUrl: String =
    "https://raw.githubusercontent.com/metabrainz/musicbrainz-server/master/admin/sql"

  override val searchConfigUrl: Option[RemoteFile] =
    Some(RemoteFile("CreateSearchConfiguration.sql", "ZGF/owrW3iQxsIpkZkfdsw==", dropFirstLine))
  override val extensionsUrl: Option[RemoteFile] =
    Some(RemoteFile("Extensions.sql", "rOawIv9JDFZ4LkpPPuU1aA==", dropFirstLine))
  override val collationsUrl: Option[RemoteFile] =
    Some(RemoteFile("CreateCollations.sql", "RTo18SRSLu6VcRZYILxEOw==", dropFirstLine))
  val typesUrl: Option[RemoteFile] =
    Some(RemoteFile("CreateTypes.sql", "c3SVOoohuG+N9/1iUT2bnQ==", dropFirstLine))
  val tableUrl: RemoteFile =
    RemoteFile("CreateTables.sql", "l3EbpPB81TQZ9GnQbcwmHg==", dropFirstLine)
  val fkUrl: RemoteFile = RemoteFile("CreateFKConstraints.sql", "P6pSpGXPV+jK7nFe1s8iiA==",
      dropFirstTwoLines, ignoreForeignKey("editor"), ignoreForeignKey("owner"))
  val indexUrl: RemoteFile = RemoteFile("CreateIndexes.sql", "bVOUamO0+4UP37nOoxfgVg==",
      dropFirstLine)
  val pkUrl: RemoteFile = RemoteFile("CreatePrimaryKeys.sql", "8RG5YwM4mzufBMgG9Kdezg==",
      dropFirstTwoLines)
  val funcUrl: RemoteFile = RemoteFile("CreateFunctions.sql", "QP03jOaf5NcU+LWHLH5SQw==",
      dropFirstLine)
  val remoteSqlFiles: Seq[RemoteFile] = 
    List(extensionsUrl.get, searchConfigUrl.get, collationsUrl.get, typesUrl.get,
      tableUrl, fkUrl, indexUrl, pkUrl, funcUrl)

  val sqlLocation = "resources/sql"
  val dumpNames = List("mbdump.tar.bz2", "mbdump-derived.tar.bz2")
}

object MusicbrainzSettings extends MusicbrainzSettings

object CoverArtArchiveSettings extends Settings {
  val dbSchema = "cover_art_archive"

  val remoteBaseUrl: String = MusicbrainzSettings.remoteBaseUrl + "/caa"

  val typesUrl: Option[RemoteFile] = None

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

