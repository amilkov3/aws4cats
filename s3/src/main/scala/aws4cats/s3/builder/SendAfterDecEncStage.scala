package aws4cats.s3.builder

import aws4cats.Stage
import cats.effect.{Async, ContextShift, Resource}
import org.http4s.{EntityDecoder, EntityEncoder, MediaType}

import scala.concurrent.ExecutionContext

abstract case class DecodeStage[R, O[_]](req: R) extends Stage {

  def decode[F[_], A](
    mediaType: MediaType,
    strict: Boolean,
    blockingEcR: Resource[F, ExecutionContext],
    chunkSizeBytes: Int
  )(
    implicit ED: EntityDecoder[F, A],
    CS: ContextShift[F]): SendAfterDecEncStage[O[A], F]
}

abstract class SendAfterDecEncStage[O, F[_]] extends Stage {

  def send(implicit A: Async[F]): F[O]
}

abstract case class EncodeStage[R, O](req: R) extends Stage {

  def encode[F[_], A](a: A)(
    implicit EE: EntityEncoder[F, A]): SendAfterDecEncStage[O, F]

}
