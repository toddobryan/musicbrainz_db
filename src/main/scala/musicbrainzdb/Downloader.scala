package musicbrainzdb

import java.io.{BufferedInputStream, ByteArrayOutputStream, File, FileOutputStream}
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths, StandardOpenOption}

import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.net.ftp.{FTP, FTPClient, FTPReply}

import scala.io.Source

object Downloader {
  val Md5Sums = "MD5SUMS"
  val Sha256Sums = "SHA256SUMS"
  val RemoteDumpDir = "mbdump"

  lazy val latest: String = {
    val ftp: FTPClient = newFtpClient()
    ftp.enterLocalPassiveMode()
    ftp.changeWorkingDirectory("/pub/musicbrainz/data/fullexport/")
    val baos = new ByteArrayOutputStream()
    ftp.retrieveFile("LATEST", baos)
    baos.close()
    ftp.disconnect()
    new String(baos.toByteArray, StandardCharsets.UTF_8).trim()
  }

  def downloadSqlFiles(): Unit = {
    MusicbrainzSettings.remoteSqlFiles.foreach(downloadSqlFile(MusicbrainzSettings, _))
    CoverArtArchiveSettings.remoteSqlFiles.foreach(downloadSqlFile(CoverArtArchiveSettings, _))
  }

  def downloadSqlFile(settings: Settings, file: RemoteFile): Unit = {
    val localPath: Path = Paths.get(settings.sqlLocation, file.name)
    Files.deleteIfExists(localPath)
    val dataFromRemote: String = file.get(settings.remoteBaseUrl)
    Files.write(localPath, dataFromRemote.getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)
  }

  def getHash(name: String): String = {
    Base64.encodeBase64String(
      DigestUtils.md5(
        Source.fromURL(new URL(s"${MusicbrainzSettings.remoteBaseUrl}/$name")).mkString))
  }

  def newFtpClient(): FTPClient = {
    val ftp = new FTPClient()
    ftp.connect("ftp.musicbrainz.org")
    val reply = ftp.getReplyCode
    if (!FTPReply.isPositiveCompletion(reply)) {
      ftp.disconnect()
      throw new RuntimeException("Error connecting to Musicbrainz FTP server")
    }
    ftp.login("anonymous", "anonymous")
    ftp
  }

  def downloadDumps(settings: Settings): Unit = {
    println(s"The latest mbdump is $latest")
    val dumpPath = Paths.get(MusicbrainzSettings.dbDumpDir, latest)
    if (!Files.exists(dumpPath)) {
      Files.createDirectory(dumpPath)
    }
    val ftp: FTPClient = newFtpClient()
    ftp.enterLocalPassiveMode()
    ftp.changeWorkingDirectory(s"/pub/musicbrainz/data/fullexport/$latest/")
    retrieveIntoDumps(ftp, Md5Sums)
    retrieveIntoDumps(ftp, Sha256Sums)
    settings.dumpNames.foreach { dump =>
      if (isUpToDate(dump)) {
        println(s"${dump} file is already up to date.")
      } else {
        retrieveIntoDumps(ftp, dump)
        println("Checking download for accuracy...")
        if (!isUpToDate(dump)) {
          throw new RuntimeException("Download didn't check out.")
        }
      }
    }
  }

  def retrieveIntoDumps(ftp: FTPClient, filename: String): Unit = {
    val file = Paths.get(MusicbrainzSettings.dbDumpDir, latest, filename)
    ftp.enterLocalPassiveMode()
    ftp.setFileType(FTP.BINARY_FILE_TYPE)
    val in = new BufferedInputStream(ftp.retrieveFileStream(filename))
    val out = new FileOutputStream(file.toFile)
    print(s"Reading $filename")
    var byte = in.read()
    var bytesRead = 1L
    while(byte != -1) {
      bytesRead += 1
      if (bytesRead % (1024 * 1024) == 0) {
        print(".")
      }
      if (bytesRead % (100 * 1024 * 1024) == 0) {
        print(s"\n${bytesRead / (1024 * 1024)}MB")
      }
      out.write(byte)
      byte = in.read()
    }
    println()
    val successful = ftp.completePendingCommand()
    out.close()
    in.close()
    if (!successful) {
      throw new RuntimeException("Download failed!")
    }
  }

  def dumpDir(): String = {
    s"${MusicbrainzSettings.dbDumpDir}/$latest"
  }

  def dumpFile(filename: String): String = {
    s"${dumpDir()}/$filename"
  }

  /**
    * Given a .tar.bz2 filename, grab the MD5 and SHA256 hashes from their files
    */
  def expectedHashes(filename: String): Option[(String, String)] = {
    val md5s: List[String] = Source.fromFile(new File(dumpFile(Md5Sums))).getLines().toList
    val sha256s: List[String] = Source.fromFile(new File(dumpFile(Sha256Sums))).getLines().toList
    val md5: Option[String] = md5s.find(_.trim().endsWith(filename))
    val sha256: Option[String] = sha256s.find(_.trim().endsWith(filename))
    if (md5.isDefined && sha256.isDefined) {
      Some((toFirstSpace(md5.get), toFirstSpace(sha256.get)))
    } else {
      None
    }
  }

  def toFirstSpace(str: String): String = {
    val space = str.indexOf(' ')
    if (space != -1) {
      str.substring(0, space)
    } else {
      ""
    }
  }

  def isUpToDate(filename: String): Boolean = {
    val path = Paths.get(dumpFile(filename))
    Files.exists(path) && {
      println(s"$filename already exists. Checking if it's correct...")
      val (expectedMd5, expectedSha256) = expectedHashes(filename).get
      println(s"md5 hash should be $expectedMd5")
      println(s"sha256 hash should be $expectedSha256")
      val md5 = DigestUtils.md5Hex(Files.newInputStream(path))
      println(s"md5 hash is $md5")
      if (md5 != expectedMd5) {
        false
      } else {
        val sha256 = DigestUtils.sha256Hex(Files.newInputStream(path))
        println(s"sha256 hash is $sha256")
        expectedSha256 == sha256
      }
    }
  }
}
