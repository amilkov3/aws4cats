package aws4cats

import com.rits.cloning.Cloner
import org.http4s.Header
import software.amazon.awssdk.core.client.config.{
  ClientOverrideConfiguration => AwsClientOverrideConfiguration
}
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient

import scala.collection.JavaConverters._

sealed abstract class ClientOverrideConfigurationBuilder(
  builder: AwsClientOverrideConfiguration.Builder
) {

  private val cloner = new Cloner()

  private def copy(
    set: AwsClientOverrideConfiguration.Builder => AwsClientOverrideConfiguration.Builder
  ): ClientOverrideConfigurationBuilder =
    new ClientOverrideConfigurationBuilder(
      builder = set(cloner.deepClone(builder))
    ) {}

  def headers(headers: List[Header]): ClientOverrideConfigurationBuilder =
    copy(
      _.headers(
        headers
          .foldLeft(Map.empty[String, List[String]])(
            (m, h) =>
              m + (h.name.value -> (h.value :: m
                .getOrElse(h.name.value, List.empty[String])))
          )
          .mapValues(_.asJava)
          .asJava
      )
    )

  def build(): ClientOverrideConfiguration =
    new ClientOverrideConfiguration(builder.build()) {}
}

sealed abstract class ClientOverrideConfiguration(
  conf: AwsClientOverrideConfiguration)
