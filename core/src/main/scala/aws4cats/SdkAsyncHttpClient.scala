package aws4cats

import software.amazon.awssdk.http.async.{
  SdkAsyncHttpClient => AwsSdkAsyncHttpClient
}

sealed trait SdkAsyncHttpClient extends Product with Serializable {
  val client: AwsSdkAsyncHttpClient
}

case class Wrapped(client: AwsSdkAsyncHttpClient) extends SdkAsyncHttpClient
