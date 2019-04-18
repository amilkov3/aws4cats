package aws4cats.s3

package builder

import aws4cats.{BuilderStage, SendStage}
import cats.effect.Async
import aws4cats.internal._
import com.rits.cloning.Cloner
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.{
  CreateBucketConfiguration,
  CreateBucketRequest
}

abstract class BaseCreateBucketBuilder(
  builder: CreateBucketRequest.Builder,
  client: S3AsyncClient
) extends BuilderStage[
    CreateBucketRequest.Builder,
    SendStage[CreateBucketRequest, String],
    S3AsyncClient](builder, client) {

  def acl(bucketAcl: BucketCannedACL): BaseCreateBucketBuilder

  def grantRead(grantee: Grantee): BaseCreateBucketBuilder

  def grantWrite(grantee: Grantee): CreateBucketBuilder

  def grantReadACP(grantee: Grantee): CreateBucketBuilder

  def grantWriteACP(grantee: Grantee): CreateBucketBuilder

  def grantFullControl(grantee: Grantee): CreateBucketBuilder

  def locationConstraint(config: BucketRegion): CreateBucketBuilder

  def objectLockEnabledForBucket(bool: Boolean): CreateBucketBuilder
}

private[s3] class CreateBucketBuilder(
  builder: CreateBucketRequest.Builder,
  client: S3AsyncClient
) extends BaseCreateBucketBuilder(builder, client) {
  self =>

  private val cloner = new Cloner()

  protected def copy(
    modify: CreateBucketRequest.Builder => CreateBucketRequest.Builder)
    : CreateBucketBuilder =
    new CreateBucketBuilder(
      modify(cloner.deepClone(builder)),
      client
    )

  def acl(bucketAcl: BucketCannedACL): CreateBucketBuilder =
    copy(_.acl(bucketAcl.toString))

  def grantRead(grantee: Grantee): CreateBucketBuilder =
    copy(_.grantRead(grantee.toHeader))

  def grantWrite(grantee: Grantee): CreateBucketBuilder =
    copy(_.grantWrite(grantee.toHeader))

  def grantReadACP(grantee: Grantee): CreateBucketBuilder =
    copy(_.grantReadACP(grantee.toHeader))

  def grantWriteACP(grantee: Grantee): CreateBucketBuilder =
    copy(_.grantWriteACP(grantee.toHeader))

  def grantFullControl(grantee: Grantee): CreateBucketBuilder =
    copy(_.grantFullControl(grantee.toHeader))

  def locationConstraint(config: BucketRegion): CreateBucketBuilder =
    copy(
      _.createBucketConfiguration(
        CreateBucketConfiguration
          .builder()
          .locationConstraint(config.bucketLocationConstraint)
          .build
      )
    )

  def objectLockEnabledForBucket(bool: Boolean): CreateBucketBuilder =
    copy(_.objectLockEnabledForBucket(bool))

  override def build(): SendStage[CreateBucketRequest, String] =
    new SendStage[CreateBucketRequest, String](builder.build()) { self =>
      override def send[F[_]](implicit A: Async[F]): F[String] =
        A.async { cb =>
          client.createBucket(self.req).handleResult(cb, _.location())
        }
    }

}
