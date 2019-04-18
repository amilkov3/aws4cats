package aws4cats

import cats.effect.Async
import software.amazon.awssdk.awscore.AwsRequest
import software.amazon.awssdk.core.SdkClient

trait Stage

/** @tparam B underlying AWS request builder type
  * @tparam S Next stage in constructing the request
  * @tparam C underlying AWS client that will issue the request
  * */
// TODO: simplify or simply get rid of this odd builder design pattern
abstract class BuilderStage[
  B <: AwsRequest.Builder,
  S <: Stage,
  C <: SdkClient
](builder: B, client: C)
  extends Stage {

  def build(): S
}

/** @tparam R underlying AWS request type
  * @tparam O decoded payload type
  * */
abstract class SendStage[R <: AwsRequest, O](val req: R) extends Stage {

  def send[F[_]](implicit A: Async[F]): F[O]
}
