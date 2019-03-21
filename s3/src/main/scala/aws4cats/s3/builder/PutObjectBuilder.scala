package aws4cats.s3

package builder

import java.time.Instant
import java.util.Base64

import aws4cats._
import aws4cats.internal._
import cats.effect.Async
import cats.implicits._
import org.apache.commons.codec.digest.DigestUtils
import org.http4s.EntityEncoder
import org.http4s.headers._
import software.amazon.awssdk.core.internal.async.ByteArrayAsyncRequestBody
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.{PutObjectRequest, Tagging}

import scala.collection.JavaConverters._

private[s3] class PutObjectBuilder(
  builder: PutObjectRequest.Builder,
  client: S3AsyncClient
) extends BuilderStage[
    PutObjectRequest.Builder,
    EncodeStage[PutObjectRequest, PutObjectResponse],
    S3AsyncClient
  ](builder, client) {
  self =>

  def acl(bucketAcl: ObjectCannedACL): PutObjectBuilder = {
    builder.acl(bucketAcl.toString)
    self
  }

  def cacheControl(cacheControl: `Cache-Control`): PutObjectBuilder = {
    builder.cacheControl(cacheControl.value)
    self
  }

  def contentDisposition(
    contentDisposition: `Content-Disposition`): PutObjectBuilder = {
    builder.contentDisposition(contentDisposition.value)
    self
  }

  def contentEncoding(contentEncoding: `Content-Encoding`): PutObjectBuilder = {
    builder.contentEncoding(contentEncoding.contentCoding.coding)
    self
  }

  def contentLanguage(contentLanguage: String): PutObjectBuilder = {
    builder.contentLanguage(contentLanguage)
    self
  }

  def contentLength(contentLength: `Content-Length`): PutObjectBuilder = {
    builder.contentLength(contentLength.length)
    self
  }

  def contentMD5(contentMD5: MD5): PutObjectBuilder = {
    builder.contentMD5(contentMD5.toString)
    self
  }

  def contentType(contentType: `Content-Type`): PutObjectBuilder = {
    builder.contentType(contentType.value)
    self
  }

  def contentType(expires: Expires): PutObjectBuilder = {
    builder.expires(expires.expirationDate.toInstant)
    self
  }

  def grantFullControl(grantee: Grantee): PutObjectBuilder = {
    builder.grantFullControl(grantee.toHeader)
    self
  }

  def grantRead(grantee: Grantee): PutObjectBuilder = {
    builder.grantRead(grantee.toHeader)
    self
  }

  def grantReadACP(grantee: Grantee): PutObjectBuilder = {
    builder.grantReadACP(grantee.toHeader)
    self
  }

  def grantWriteACP(grantee: Grantee): PutObjectBuilder = {
    builder.grantWriteACP(grantee.toHeader)
    self
  }

  def objectLockLegalHoldStatus(
    status: ObjectLockLegalHoldStatus): PutObjectBuilder = {
    builder.objectLockLegalHoldStatus(status.objectLockLegalHoldStatus)
    self
  }

  def objectLockMode(mode: ObjectLockMode): PutObjectBuilder = {
    builder.objectLockMode(mode.objectLockMode)
    self
  }

  def objectLockRetainUntilDate(date: Instant): PutObjectBuilder = {
    builder.objectLockRetainUntilDate(date)
    self
  }

  def requestPayer(requestPayer: RequestPayer): PutObjectBuilder = {
    builder.requestPayer(requestPayer.requestPayer)
    self
  }

  def serverSideEncryption(sse: ServerSideEncryption): PutObjectBuilder = {
    builder.serverSideEncryption(sse.serverSideEncryption.toString)
    self
  }

  def sseCustomer(customerKey: String): PutObjectBuilder = {
    builder.sseCustomerAlgorithm(AES256.toString)
    builder.sseCustomerKey(
      Base64.getEncoder.encodeToString(customerKey.getBytes))
    builder.sseCustomerKeyMD5(
      Base64.getEncoder.encodeToString(DigestUtils.md5(customerKey)))
    self
  }

  def ssekmsKeyId(keyId: String): PutObjectBuilder = {
    builder.ssekmsKeyId(keyId)
    self
  }

  def storageClass(storageClass: StorageClass): PutObjectBuilder = {
    builder.storageClass(storageClass.storageClass)
    self
  }

  def tagging(tagging: Set[Tag]): PutObjectBuilder = {
    builder.tagging(
      Tagging.builder().tagSet(tagging.map(_.toTag).asJavaCollection).build)
    self
  }

  override def build(): EncodeStage[PutObjectRequest, PutObjectResponse] =
    new EncodeStage[PutObjectRequest, PutObjectResponse](builder.build()) {
      self =>
      override def encode[F[_], A](a: A)(implicit EE: EntityEncoder[F, A])
        : SendAfterDecEncStage[PutObjectResponse, F] =
        new SendAfterDecEncStage[PutObjectResponse, F] {
          override def send(implicit A: Async[F]): F[PutObjectResponse] =
            EE.toEntity(a)
              .body
              .compile
              // TODO:
              .toVector
              .map(_.toArray)
              .flatMap(
                body =>
                  A.async { cb =>
                    client
                      .putObject(self.req, new ByteArrayAsyncRequestBody(body))
                      .handleResult(
                        cb,
                        res =>
                          PutObjectResponse(
                            res.expiration(),
                            ServerSideEncryption.fromEnum(
                              res.serverSideEncryption()),
                            res.sseCustomerKeyMD5(),
                            res.ssekmsKeyId(),
                            res.eTag(),
                            RequestCharged.fromEnum(res.requestCharged())
                        )
                      )
                }
              )
        }

    }

}
