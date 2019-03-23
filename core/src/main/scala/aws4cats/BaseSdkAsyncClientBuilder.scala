package aws4cats

import java.net.URI

import cats.effect.Resource
import com.rits.cloning.Cloner
import org.http4s.Uri
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder
import software.amazon.awssdk.core.client.builder.{
  SdkAsyncClientBuilder,
  SdkClientBuilder
}
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient

abstract class BaseSdkAsyncClientBuilder[
  B <: BaseSdkAsyncClientBuilder.Base[B, C], C, F[_], C1[_[_]],
  X <: BaseSdkAsyncClientBuilder[B, C, F, C1, X]](
  builder: B
) { self =>

  protected val cloner = new Cloner()

  protected def copy(modify: B => B): X

  def httpClient(client: SdkAsyncHttpClient): X =
    copy(_.httpClient(client.client))

  def endpoint(uri: Uri): X =
    copy(_.endpointOverride(URI.create(uri.renderString)))

  def region(region: Region): X =
    copy(_.region(region.region))

  def credentialsProvider(provider: AwsCredentialsProvider): X =
    copy(_.credentialsProvider(provider))

  def resource: Resource[F, C1[F]]

}

object BaseSdkAsyncClientBuilder {

  type Base[B <: Base[B, C], C] = SdkAsyncClientBuilder[B, C]
    with AwsClientBuilder[B, C]
}
