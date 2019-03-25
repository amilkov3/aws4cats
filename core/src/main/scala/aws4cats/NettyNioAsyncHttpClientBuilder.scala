package aws4cats

import java.time.Duration
import com.rits.cloning.Cloner

import scala.concurrent.duration._
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.http.async.{
  SdkAsyncHttpClient => AwsSdkAsyncHttpClient
}

sealed abstract class NettyNioAsyncHttpClientBuilder(
  builder: NettyNioAsyncHttpClient.Builder
) {

  private val cloner = new Cloner()

  private def copy(
    set: NettyNioAsyncHttpClient.Builder => NettyNioAsyncHttpClient.Builder
  ): NettyNioAsyncHttpClientBuilder =
    new NettyNioAsyncHttpClientBuilder(
      builder = set(cloner.deepClone(builder))
    ) {}

  def connectionMaxIdleTime(
    time: FiniteDuration): NettyNioAsyncHttpClientBuilder =
    copy(_.connectionMaxIdleTime(Duration.ofNanos(time.toNanos)))

  def connectionTimeout(
    timeout: FiniteDuration): NettyNioAsyncHttpClientBuilder =
    copy(_.connectionTimeout(Duration.ofNanos(timeout.toNanos)))

  def connectionTimeToLive(
    ttl: FiniteDuration): NettyNioAsyncHttpClientBuilder =
    copy(_.connectionTimeToLive(Duration.ofNanos(ttl.toNanos)))

  def maxConnections(num: Int): NettyNioAsyncHttpClientBuilder =
    copy(_.maxConcurrency(num))

  def maxPendingConnectionAcquires(num: Int): NettyNioAsyncHttpClientBuilder =
    copy(_.maxPendingConnectionAcquires(num))

  def readTimeout(timeout: FiniteDuration): NettyNioAsyncHttpClientBuilder =
    copy(_.readTimeout(Duration.ofNanos(timeout.toNanos)))

  def useIdleConnectionReaper(use: Boolean): NettyNioAsyncHttpClientBuilder =
    copy(_.useIdleConnectionReaper(use))

  def writeTimeout(timeout: FiniteDuration): NettyNioAsyncHttpClientBuilder =
    copy(_.writeTimeout(Duration.ofNanos(timeout.toNanos)))

  def build(): SdkAsyncHttpClient =
    SdkAsyncHttpClient(builder.build())

}

object NettyNioAsyncHttpClientBuilder {

  def apply(): NettyNioAsyncHttpClientBuilder =
    new NettyNioAsyncHttpClientBuilder(
      NettyNioAsyncHttpClient.builder()
    ) {}
}

// TODO: wrapper trait so users can implement their own clients
case class SdkAsyncHttpClient(client: AwsSdkAsyncHttpClient)
