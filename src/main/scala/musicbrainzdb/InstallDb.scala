package musicbrainzdb

/**
  * Main method that makes sure Postgres is installed and is accessible,
  * downloads the latest Musicbrainz db dumps for the main db and CD stubs,
  * and dumps everything into a PostgreSQL database.
  */
object InstallDb extends App {
  // back-up any existing musicbrainz_db database and then create new, empty musicbrainz_db
  // build-essential + libicu-dev
  // Add extensions: cube, earthdistance, musicbrainz_collate
  // http://www.reades.com/2015/12/11/installing-postgresql-extensions-on-mac-os-x/
  println("Downloading SQL files...")
  Downloader.downloadSqlFiles()
  println("Downloading database dumps...")
  Downloader.downloadDumps()
  println("Creating tables...")
  Postgres.createTables()
  println("Loading tables...")
  Postgres.loadTablesFromDump()
  println("Updating indexes...")
  Postgres.updateIndexes()
  println("Updating foreign keys...")
  Postgres.updateForeignKey()
  println("All done!")
}
