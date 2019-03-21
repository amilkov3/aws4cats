package aws4cats.s3

import java.io.InputStream
import java.lang
import java.nio.ByteBuffer
import java.time.Duration
import java.util.Optional
import java.util.concurrent.{CompletableFuture, Executor, ExecutorService}
import java.time.temporal.ChronoUnit

import scala.concurrent.duration
import aws4cats.{ExecutorServiceWrapper, Region}
import aws4cats.BaseSdkAsyncClientBuilder
import software.amazon.awssdk.services.s3.{
  S3AsyncClient,
  S3AsyncClientBuilder,
  S3ClientBuilder
}
import software.amazon.awssdk.services.s3
import aws4cats.internal._
import builder._
import cats.effect.{ContextShift, Effect, Resource}
import cats.effect.implicits._
import cats.implicits._
import com.rits.cloning.Cloner
import io.chrisdavenport.log4cats.Logger
import org.http4s.headers.`Content-Type`
import org.http4s.{
  EntityDecoder,
  EntityEncoder,
  Headers,
  MediaType,
  Response => Http4sResp
}
import org.reactivestreams.Subscriber
import fs2.{io, text, Stream}
import software.amazon.awssdk.auth.credentials.{
  AwsCredentialsProvider,
  DefaultCredentialsProvider
}
import software.amazon.awssdk.awscore.AwsRequest
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.async.{
  AsyncRequestBody,
  AsyncResponseTransformer
}
import software.amazon.awssdk.core.client.config.{
  ClientAsyncConfiguration,
  SdkAdvancedAsyncClientOption
}
import software.amazon.awssdk.core.internal.async.{
  ByteArrayAsyncRequestBody,
  ByteArrayAsyncResponseTransformer
}
import software.amazon.awssdk.core.sync.ResponseTransformer
import software.amazon.awssdk.http.async.{
  AsyncExecuteRequest,
  SdkAsyncHttpClient
}
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.services.s3.model.{
  GetObjectResponse => AwsGetObjectResponse,
  _
}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

sealed abstract class AsyncS3Client[F[_]](client: S3AsyncClient)(
  implicit A: Effect[F],
  CS: ContextShift[F],
  L: Logger[F]
) extends S3Client[F] {

  def createBucket(
    bucketName: String
  ): CreateBucketBuilder =
    new CreateBucketBuilder(
      CreateBucketRequest
        .builder()
        .bucket(bucketName),
      client)

  def deleteBucket(
    bucketName: String
  ): F[Unit] =
    A.async { cb =>
      val r = DeleteBucketRequest.builder().bucket(bucketName).build()
      client.deleteBucket(r).handleVoidResult(cb)
    }

  def getObject[O](
    bucketName: String,
    blockingEcR: Resource[F, ExecutionContext],
    chunkSizeBytes: Int
  )(implicit ED: EntityDecoder[F, O]): GetObjectBuilder =
    new GetObjectBuilder(
      GetObjectRequest
        .builder()
        .bucket(bucketName),
      client
    )

  def putObject[O](
    bucketName: String,
    key: String,
    obj: O
  )(implicit EE: EntityEncoder[F, O]): PutObjectBuilder =
    new PutObjectBuilder(
      PutObjectRequest.builder().bucket(bucketName).key(key),
      client
    )

  def deleteObject(
    bucketName: String,
    versionId: Option[String] = None,
    mfa: Option[String] = None
  ): F[(Boolean, String)] =
    A.async { cb =>
      val r = DeleteObjectRequest
        .builder()
        .versionId(versionId.getOrElse(null))
        .mfa(mfa.getOrElse(null))
        .build
      client
        .deleteObject(r)
        .handleResult(
          cb,
          res => (res.deleteMarker(): Boolean) -> res.versionId())
    }

}

sealed abstract class AsyncS3ClientBuilder[F[_]: Effect: ContextShift: Logger](
  builder: S3AsyncClientBuilder,
  ecR: Resource[F, ExecutionContext]
) extends BaseSdkAsyncClientBuilder[S3AsyncClientBuilder, S3AsyncClient](
    builder) {

  override protected def copy(
    modify: S3AsyncClientBuilder => S3AsyncClientBuilder) =
    new AsyncS3ClientBuilder[F](
      builder = modify(cloner.deepClone(builder)),
      ecR = ecR
    ) {}

  def s3Configuration(conf: S3Configuration): AsyncS3ClientBuilder[F] =
    copy(_.serviceConfiguration(conf.repr))

  def resource: Resource[F, S3Client[F]] =
    ecR.map(
      ec =>
        new AsyncS3Client[F](
          builder
            .asyncConfiguration(
              ClientAsyncConfiguration
                .builder()
                .advancedOption(
                  SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR,
                  new ExecutorServiceWrapper(ec)
                )
                .build()
            )
            .build()
        ) {}
    )

}

object AsyncS3ClientBuilder {

  def apply[F[_]: Effect: Logger: ContextShift](
    ecR: Resource[F, ExecutionContext]
  ): AsyncS3ClientBuilder[F] =
    new AsyncS3ClientBuilder[F](
      S3AsyncClient.builder(),
      ecR
    ) {}

}
