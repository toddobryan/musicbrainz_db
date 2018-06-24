## MusicBrainz DB Only

I've wanted to play with the MusicBrainz data for a long time, but

* I didn't want to set up the server
* I only really wanted the public data, not the extra stuff
* I didn't want to have to deal with Perl

As a result, I created this project, based very loosely on
https://github.com/elliotchance/mbzdb

Before you can run it, do the following.

1. Install PostgreSQL 10. (For Ubuntu 18.04,  
  `sudo apt-get install postgresql-server-dev-10`)
1. Install the following Postgres extensions (On Mac,  
  see:[installing extensions][1] or on Ubuntu  
  `sudo apt-get install postgresql-contrib`
    * cube
    * earthdistance
1. Download, build, and install [`musicbrainz_collate`][2].  
  You'll need `libicu-dev` and build tools.

1. Set everything up in Postgres:
    ```
    $ sudo -u postgres psql
    > CREATE DATABASE musicbrainz_db;
    > CREATE USER musicbrainz WITH PASSWORD 'musicbrainz';
    > GRANT ALL ON DATABASE musicbrainz_db TO musicbrainz;
    > \c musicbrainz_db;
    > CREATE EXTENSION cube;
    > CREATE EXTENSION earthdistance;
    > CREATE EXTENSION musicbrainz_collate;
    ```
    `\q` will exit `psql`.
1. Run the `InstallDb.scala` file. Assuming you've installed [SBT][3],  
  from the terminal, `sbt run musicbrainz.InstallDb`

[1]: http://www.reades.com/2015/12/11/installing-postgresql-extensions-on-mac-os-x/
[2]: https://github.com/metabrainz/postgresql-musicbrainz-collate/blob/master/README.musicbrainz_collate.md
[3]: https://www.scala-sbt.org/