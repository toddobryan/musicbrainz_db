package musicbrainzdb

import java.net.URL

import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils

import scala.io.Source

class RemoteFile(
    val name: String,
    val hash: String,
    val transformers: List[String => String]) {

  def get(urlBase: String): String = {
    val src = Source.fromURL(new URL(s"$urlBase/$name")).mkString
    val newHash = Base64.encodeBase64String(DigestUtils.md5(src))
    if (newHash != hash) {
      throw new RuntimeException(
        s"File $name has changed (new hash = $newHash). " +
            "Check differences to make sure process still works.")
    }
    transformers.toList.foldLeft(src)((newSrc, transformer) => transformer(newSrc))
  }
}

object RemoteFile {
  def apply(name: String, hash: String, transformers: (String => String)*) =
    new RemoteFile(name, hash, transformers.toList)
}
