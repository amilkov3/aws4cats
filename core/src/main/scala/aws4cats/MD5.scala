package aws4cats

import software.amazon.awssdk.utils.Md5Utils

case class MD5 private (
  override val toString: String
)

object MD5 {

  def apply(plainStr: String): MD5 =
    new MD5(Md5Utils.md5AsBase64(plainStr.getBytes))
}
