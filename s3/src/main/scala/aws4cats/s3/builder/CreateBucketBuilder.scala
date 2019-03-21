package aws4cats.s3

package builder

import aws4cats.{BuilderStage, SendStage}
import cats.effect.Async
import aws4cats.internal._
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.{
  CreateBucketConfiguration,
  CreateBucketRequest
}

private[s3] class CreateBucketBuilder(
  builder: CreateBucketRequest.Builder,
  client: S3AsyncClient
) extends BuilderStage[
    CreateBucketRequest.Builder,
    SendStage[CreateBucketRequest, String],
    S3AsyncClient
  ](builder, client) {
  self =>

  def acl(bucketAcl: BucketCannedACL): CreateBucketBuilder = {
    builder.acl(bucketAcl.toString)
    self
  }

  def grantRead(grantee: Grantee): CreateBucketBuilder = {
    builder.grantRead(grantee.toHeader)
    self
  }

  def grantWrite(grantee: Grantee): CreateBucketBuilder = {
    builder.grantWrite(grantee.toHeader)
    self
  }

  def grantReadACP(grantee: Grantee): CreateBucketBuilder = {
    builder.grantReadACP(grantee.toHeader)
    self
  }

  def grantWriteACP(grantee: Grantee): CreateBucketBuilder = {
    builder.grantWriteACP(grantee.toHeader)
    self
  }

  def grantFullControl(grantee: Grantee): CreateBucketBuilder = {
    builder.grantFullControl(grantee.toHeader)
    self
  }

  def locationConstraint(config: BucketRegion): CreateBucketBuilder = {
    builder.createBucketConfiguration(
      CreateBucketConfiguration
        .builder()
        .locationConstraint(config.bucketLocationConstraint)
        .build
    )
    self
  }

  def objectLockEnabledForBucket(bool: Boolean): CreateBucketBuilder = {
    builder.objectLockEnabledForBucket(bool)
    self
  }

  override def build(): SendStage[CreateBucketRequest, String] =
    new SendStage[CreateBucketRequest, String](builder.build()) { self =>
      override def send[F[_]](implicit A: Async[F]): F[String] =
        A.async { cb =>
          client.createBucket(self.req).handleResult(cb, _.location())
        }
    }

}
