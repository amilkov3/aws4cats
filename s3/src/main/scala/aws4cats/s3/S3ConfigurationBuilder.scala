package aws4cats.s3

import com.rits.cloning.Cloner
import software.amazon.awssdk.services.s3.{
  S3Configuration => AwsS3Configuration
}

sealed abstract class S3ConfigurationBuilder(
  builder: AwsS3Configuration.Builder
) {

  private val cloner = new Cloner()

  private def copy(
    modify: AwsS3Configuration.Builder => AwsS3Configuration.Builder
  ): S3ConfigurationBuilder =
    new S3ConfigurationBuilder(
      builder = modify(cloner.deepClone(builder))
    ) {}

  def checksumValidationEnabled(enable: Boolean): S3ConfigurationBuilder =
    copy(_.checksumValidationEnabled(enable))

  def chunkedEncodingEnabled(enable: Boolean): S3ConfigurationBuilder =
    copy(_.chunkedEncodingEnabled(enable))

  def dualstackEnabled(enable: Boolean): S3ConfigurationBuilder =
    copy(_.dualstackEnabled(enable))

  def pathStyleAccessEnabled(enable: Boolean): S3ConfigurationBuilder =
    copy(_.pathStyleAccessEnabled(enable))

  def accelerateModeEnabled(enable: Boolean): S3ConfigurationBuilder =
    copy(_.accelerateModeEnabled(enable))

  def build(): S3Configuration =
    new S3Configuration(builder.build()) {}

}

object S3ConfigurationBuilder {

  def apply: S3ConfigurationBuilder =
    new S3ConfigurationBuilder(AwsS3Configuration.builder()) {}
}

sealed abstract class S3Configuration(val repr: AwsS3Configuration)
