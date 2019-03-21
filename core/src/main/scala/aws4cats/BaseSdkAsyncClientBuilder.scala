package aws4cats

import java.net.URI

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
  B <: BaseSdkAsyncClientBuilder.Base[B, C], C](
  builder: B
) { self =>

  protected val cloner = new Cloner()

  protected def copy(modify: B => B) =
    new BaseSdkAsyncClientBuilder[B, C](
      builder = modify(cloner.deepClone(builder))
    ) {}

  def httpClient(client: SdkAsyncHttpClient): BaseSdkAsyncClientBuilder[B, C] =
    copy(_.httpClient(client.client))

  def endpoint(uri: Uri): BaseSdkAsyncClientBuilder[B, C] =
    copy(_.endpointOverride(URI.create(uri.renderString)))

  def region(region: Region): BaseSdkAsyncClientBuilder[B, C] =
    copy(_.region(region.region))

  def credentialsProvider(
    provider: AwsCredentialsProvider): BaseSdkAsyncClientBuilder[B, C] =
    copy(_.credentialsProvider(provider))

}

object BaseSdkAsyncClientBuilder {

  type Base[B <: Base[B, C], C] = SdkAsyncClientBuilder[B, C]
    with AwsClientBuilder[B, C]
}
