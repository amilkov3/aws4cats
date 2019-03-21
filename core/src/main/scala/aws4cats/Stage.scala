package aws4cats

import cats.effect.Async
import software.amazon.awssdk.awscore.AwsRequest
import software.amazon.awssdk.core.SdkClient

trait Stage

abstract class BuilderStage[B <: AwsRequest.Builder, S <: Stage, C <: SdkClient](
    builder: B,
    client: C)
    extends Stage {

  def build(): S
}

abstract class SendStage[R, O](val req: R) extends Stage {

  def send[F[_]](implicit A: Async[F]): F[O]
}
