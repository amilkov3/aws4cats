package aws4cats.s3

package builder

import aws4cats._
import aws4cats.internal._
import cats.effect.{Async, ContextShift, Resource}
import cats.implicits._
import fs2.io
import org.http4s.{EntityDecoder, Headers, MediaType, Response => Http4sResp}
import org.http4s.headers._
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.{
  GetObjectRequest,
  GetObjectResponse => AwsGetObjectResponse
}

import scala.concurrent.ExecutionContext

private[s3] class GetObjectBuilder(
  builder: GetObjectRequest.Builder,
  client: S3AsyncClient
) extends BuilderStage[
    GetObjectRequest.Builder,
    DecodeStage[GetObjectRequest, GetObjectResponse],
    S3AsyncClient
  ](builder, client) { self =>

  override def build(): DecodeStage[GetObjectRequest, GetObjectResponse] =
    new DecodeStage[GetObjectRequest, GetObjectResponse](builder.build()) {
      self =>
      override def decode[F[_], A](
        mediaType: MediaType,
        strict: Boolean,
        blockingEcR: Resource[F, ExecutionContext],
        chunkSizeBytes: Int
      )(
        implicit ED: EntityDecoder[F, A],
        CS: ContextShift[F]): SendAfterDecEncStage[GetObjectResponse[A], F] =
        new SendAfterDecEncStage[GetObjectResponse[A], F] {
          override def send(implicit A: Async[F]): F[GetObjectResponse[A]] =
            blockingEcR.use { blockingEc =>
              for {
                res <- A.async[ResponseBytes[AwsGetObjectResponse]] { cb =>
                  client
                    .getObject(
                      self.req,
                      AsyncResponseTransformer.toBytes[AwsGetObjectResponse])
                    .handleResult[ResponseBytes[AwsGetObjectResponse]](
                      cb,
                      identity)
                }
                mediaType <- A.fromEither(
                  MediaType.parse(res.response().contentEncoding()))
                o <- ED
                  .decode(
                    Http4sResp[F](
                      headers = Headers(`Content-Type`(mediaType)),
                      body = io.readInputStream(
                        res.asInputStream().pure[F],
                        chunkSizeBytes,
                        blockingEc,
                        false
                      )
                    ),
                    false
                  )
                  .value
                resp = res.response()
                headers <- A.fromEither(
                  (
                    `Cache-Control`
                      .parse(resp.cacheControl())
                      .leftMap(_.message)
                      .toValidatedNel,
                    `Content-Disposition`
                      .parse(resp.contentDisposition())
                      .leftMap(_.message)
                      .toValidatedNel,
                    `Content-Encoding`
                      .parse(resp.contentEncoding())
                      .leftMap(_.message)
                      .toValidatedNel,
                    `Content-Length`
                      .parse(resp.contentLength().toString)
                      .leftMap(_.message)
                      .toValidatedNel,
                    `Content-Range`
                      .parse(resp.contentRange())
                      .leftMap(_.message)
                      .toValidatedNel,
                    `Content-Type`
                      .parse(resp.contentType())
                      .leftMap(_.message)
                      .toValidatedNel)
                    .mapN {
                      case (cc, cd, ce, cl, cr, ct) => (cc, cd, ce, cl, cr, ct)
                    }
                    .leftMap(nel => new Exception(nel.mkString_(", ")))
                    .toEither
                )
                (cc, cd, ce, cl, cr, ct) = headers
              } yield
                GetObjectResponse(
                  resp.acceptRanges(),
                  o,
                  cc,
                  cd,
                  ce,
                  resp.contentLanguage(),
                  cl,
                  cr,
                  ct,
                  resp.deleteMarker(),
                  resp.eTag(),
                  resp.expiration(),
                  resp.expires(),
                  resp.lastModified(),
                  resp.missingMeta(),
                  ObjectLockLegalHoldStatus.fromEnum(
                    resp.objectLockLegalHoldStatus()),
                  ObjectLockMode.fromEnum(resp.objectLockMode()),
                  resp.objectLockRetainUntilDate(),
                  resp.partsCount(),
                  ReplicationStatus.fromEnum(resp.replicationStatus()),
                  RequestCharged.fromEnum(resp.requestCharged()),
                  resp.restore(),
                  ServerSideEncryption.fromEnum(resp.serverSideEncryption()),
                  resp.sseCustomerAlgorithm(),
                  resp.sseCustomerKeyMD5(),
                  resp.ssekmsKeyId(),
                  StorageClass.fromEnum(resp.storageClass()),
                  resp.tagCount(),
                  resp.websiteRedirectLocation()
                )
            }
        }
    }
}
