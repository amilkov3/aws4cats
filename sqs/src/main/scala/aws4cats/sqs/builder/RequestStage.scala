package aws4cats.sqs.builder

import cats.effect._
import aws4cats._
import org.http4s.{EntityDecoder, MediaType}

abstract case class DecodeStage[R, O[_]](req: R) extends Stage {

  def decode[F[_], A](mediaType: MediaType, strict: Boolean)(
    implicit ED: EntityDecoder[F, A]): SendWithDecodeStage[O[A], F]
}

abstract class SendWithDecodeStage[O, F[_]] extends Stage {

  def send(implicit A: Async[F]): F[O]
}
