package aws4cats.sqs.builder

import cats.effect._
import aws4cats._
import org.http4s.{EntityDecoder, MediaType}
import software.amazon.awssdk.services.sqs.model.SqsRequest

/** @tparam R underlying AWS request type
  * @tparam O higher order response type that holds
  *           the type to decode payload to (A below)
  *           This is done because we're deferring the
  *           declaration of A to `decode` and then reifying
  *           our type constructor O to kind *
  * */
// TODO: Can simplify this to just `send` without decode
// since this is only builder pattern method on SQS
abstract case class DecodeStage[R <: SqsRequest, O[_]](req: R) extends Stage {

  /**
    * @tparam F effect context
    * @tparam A type to decode payload to
    * */
  def decode[F[_], A](mediaType: MediaType, strict: Boolean)(
    implicit ED: EntityDecoder[F, A]): SendWithDecodeStage[O[A], F]
}

/** @tparam O decoded payload type
  * @tparam F effect context
  */
abstract class SendWithDecodeStage[O, F[_]] extends Stage {

  def send(implicit A: Async[F]): F[O]
}
