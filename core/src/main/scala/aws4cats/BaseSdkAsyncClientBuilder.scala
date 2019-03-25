package aws4cats

import java.net.URI

import cats.effect.Resource
import com.rits.cloning.Cloner
import org.http4s.Uri
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder
import software.amazon.awssdk.core.client.builder.SdkAsyncClientBuilder

/** @tparam UB underlying AWS builder type
  * @tparam B your wrapper builder type
  * @tparam UC underlying AWS client interface
  * @tparam C your wrapper client trait
  * @tparam F effect context
  * */
// TODO: simplify
abstract class BaseSdkAsyncClientBuilder[
  UB <: BaseSdkAsyncClientBuilder.Repr[UB, UC], UC, F[_], C[_[_]],
  B <: BaseSdkAsyncClientBuilder[UB, UC, F, C, B]](
  builder: UB
) { self =>

  protected val cloner = new Cloner()

  protected def copy(modify: UB => UB): B

  def httpClient(client: SdkAsyncHttpClient): B =
    copy(_.httpClient(client.client))

  def endpoint(uri: Uri): B =
    copy(_.endpointOverride(URI.create(uri.renderString)))

  def region(region: Region): B =
    copy(_.region(region.region))

  def credentialsProvider(provider: AwsCredentialsProvider): B =
    copy(_.credentialsProvider(provider))

  def resource: Resource[F, C[F]]

}

object BaseSdkAsyncClientBuilder {

  type Repr[B <: Repr[B, C], C] = SdkAsyncClientBuilder[B, C]
    with AwsClientBuilder[B, C]
}
