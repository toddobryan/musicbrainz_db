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
  for (settings <- List(MusicbrainzSettings, CoverArtArchiveSettings)) {
    println(s"Downloading database dumps for ${settings.dbSchema}")
    Downloader.downloadDumps(settings)
    println(s"Dropping old schema ${settings.dbSchema}")
    Postgres.dropSchema(settings)
    println(s"Creating extensions for ${settings.dbSchema}")
    Postgres.createExtensions(settings)
    println(s"Creating collations for ${settings.dbSchema}")
    Postgres.createCollations(settings)
    println(s"Creating search configurations for ${settings.dbSchema}")
    Postgres.createSearchConfig(settings)
    println(s"Creating types for ${settings.dbSchema}")
    Postgres.createTypes(settings)
    println(s"Creating tables for ${settings.dbSchema}")
    Postgres.createTables(settings)
    println(s"Loading tables for ${settings.dbSchema}")
    Postgres.loadTablesFromDump(settings)
    println(s"Updating indexes for ${settings.dbSchema}")
    Postgres.updateIndexes(settings)
    println(s"Updating foreign keys for ${settings.dbSchema}")
    Postgres.updateForeignKey(settings)
    println(s"All done with ${settings.dbSchema}!")
  }
}
