package aws4cats.s3

import java.util.concurrent.ExecutorService

import aws4cats.ExecutorServiceWrapper
import cats.effect.{Effect, Resource}
import io.chrisdavenport.log4cats.Logger
import software.amazon.awssdk.auth.credentials.{AwsCredentialsProvider, DefaultCredentialsProvider}
import software.amazon.awssdk.core.client.config.{ClientAsyncConfiguration, SdkAdvancedAsyncClientOption}
import software.amazon.awssdk.services.s3.S3AsyncClient

import scala.concurrent.ExecutionContext

sealed abstract class AsyncS3Client[F[_]](client: S3AsyncClient)(
    implicit E: Effect[F],
    L: Logger[F]
) extends S3Client[F] {

}


object AsyncS3Client {

  def apply[F[_]: Effect: Logger](
    ecR: Resource[F, ExecutionContext],
    credentialsProvider: AwsCredentialsProvider =
        DefaultCredentialsProvider.create()
  ): Resource[F, S3Client[F]] =
    ecR.map(ec =>
      new AsyncS3Client[F](
        S3AsyncClient
        .builder()
        .credentialsProvider(credentialsProvider)
        .asyncConfiguration(
          ClientAsyncConfiguration.builder()
          .advancedOption(
            SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR,
            new ExecutorServiceWrapper(ec)
          ).build()
        ).build()
      ) {}
    )
}
