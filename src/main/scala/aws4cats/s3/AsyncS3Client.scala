package aws4cats.s3

import java.io.InputStream
import java.lang
import java.nio.ByteBuffer
import java.util.Optional
import java.util.concurrent.ExecutorService

import aws4cats.ExecutorServiceWrapper
import aws4cats.internal._
import cats.effect.{ContextShift, Effect, Resource}
import cats.effect.implicits._
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import org.http4s.headers.`Content-Type`
import org.http4s.{
  EntityDecoder,
  EntityEncoder,
  Headers,
  MediaType,
  Response => Http4sResp,
  Uri
}
import org.reactivestreams.Subscriber
import fs2.{Stream, io, text}
import software.amazon.awssdk.auth.credentials.{
  AwsCredentialsProvider,
  DefaultCredentialsProvider
}
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
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model._

import scala.concurrent.ExecutionContext

sealed abstract class AsyncS3Client[F[_]](client: S3AsyncClient)(
    implicit E: Effect[F],
    CS: ContextShift[F],
    L: Logger[F]
) extends S3Client[F] {

  def createBucket(
      bucketName: String
  ): F[String] =
    E.async { cb =>
      val r = CreateBucketRequest
        .builder()
        .bucket(bucketName)
        .build()
      client.createBucket(r).handleResult(cb, _.location())
    }

  def deleteBucket(
      bucketName: String
  ): F[Unit] =
    E.async { cb =>
      val r = DeleteBucketRequest.builder().bucket(bucketName).build()
      client.deleteBucket(r).handleVoidResult(cb)
    }

  def getObject[O](
      bucketName: String,
      ecR: Resource[F, ExecutionContext],
      chunkSize: Int
  )(implicit ED: EntityDecoder[F, O]) =
    ecR.use { ec =>
      E.async[InputStream] { cb =>
          val r = GetObjectRequest
            .builder()
            .bucket(bucketName)
            .build()
          client
            .getObject(r, AsyncResponseTransformer.toBytes[GetObjectResponse])
            .handleResult[InputStream](cb, _.asInputStream())
        }
        .flatMap(
          res =>
            ED.decode(Http4sResp[F](
                        headers =
                          Headers(`Content-Type`(MediaType.application.json)),
                        body = io.readInputStream(
                          res.pure[F],
                          chunkSize,
                          ec,
                          false
                        )
                      ),
                      false)
              .value)
    }

  def putObject[O](
      bucketName: String,
      obj: O
  )(implicit EE: EntityEncoder[F, O]): F[String] =
    EE.toEntity(obj)
      .body
      .compile
      // TODO:
      .toVector
      .map(_.toArray)
      .flatMap(body =>
        E.async { cb =>
          val r = PutObjectRequest.builder().bucket(bucketName).build()
          client
            .putObject(r, new ByteArrayAsyncRequestBody(body))
            .handleResult(cb, _.eTag())
      })

}

object AsyncS3Client {

  def apply[F[_]: Effect: Logger: ContextShift](
      ecR: Resource[F, ExecutionContext],
      credentialsProvider: AwsCredentialsProvider =
        DefaultCredentialsProvider.create()
  ): Resource[F, S3Client[F]] =
    ecR.map(
      ec =>
        new AsyncS3Client[F](
          S3AsyncClient
            .builder()
            .credentialsProvider(credentialsProvider)
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
        ) {})
}
