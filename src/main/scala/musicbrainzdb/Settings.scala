package musicbrainzdb

object Settings {
  val dbName = "musicbrainz_db"
  val dbUser = "musicbrainz"
  val dbPassword = "musicbrainz"
  val dbSchema = "musicbrainz"

  val remoteBaseUrl: String =
    "https://raw.githubusercontent.com/metabrainz/musicbrainz-server/master/admin/sql"

  val tableUrl: RemoteFile = RemoteFile("CreateTables.sql", "Ui1+U4Rq6mkQ9mnWtU2c0w==",
      dropFirstLine)
  val fkUrl: RemoteFile = RemoteFile("CreateFKConstraints.sql", "H1C3otYNqGGZ7fqOfMgTOw==",
      dropFirstTwoLines)
  val indexUrl: RemoteFile = RemoteFile("CreateIndexes.sql", "YgUNIMlvbBL5UraaUrmrMA==",
      dropFirstLine)
  val pkUrl: RemoteFile = RemoteFile("CreatePrimaryKeys.sql", "plaC6NmgfmUoxJsSR/IeTg==",
      dropFirstTwoLines)
  val funcUrl: RemoteFile = RemoteFile("CreateFunctions.sql", "qVGF2jz+8bwtb/beSo4svw==",
      dropFirstLine)
  val remoteSqlFiles: Seq[RemoteFile] = List(tableUrl, fkUrl, indexUrl, pkUrl, funcUrl)

  val sqlLocation = "resources/sql"
  val dbDumpLocal = "resources/db_dumps"

  def dropFirstLine(str: String) = raw"[^\n]*\n".r.replaceFirstIn(str, "")

  def dropFirstTwoLines(str: String) = dropFirstLine(dropFirstLine(str))
}
