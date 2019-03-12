package aws4cats.sqs

import aws4cats.Region
import cats.effect._
import aws4cats.internal._
import cats.implicits._
import org.http4s.{DecodeFailure, EntityDecoder, MediaType, Uri}
import software.amazon.awssdk.awscore.AwsRequest
import software.amazon.awssdk.core.SdkClient
import software.amazon.awssdk.services.sqs.model.AddPermissionRequest
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{
  MessageSystemAttributeName,
  SqsRequest
}
import software.amazon.awssdk.utils.builder.ToCopyableBuilder

case class ReceiptHandle(
    repr: String
)

case class ReceiveMessageResponse[M](
    receiptHandle: ReceiptHandle,
    attributes: Map[MessageSystemAttributeName, String],
    body: Either[DecodeFailure, M]
)

case class QueueName(
    name: String
)

object QueueName {

  def unsafe(name: String): QueueName = apply(name).rethrow

  def apply(name: String): Either[String, QueueName] =
    if (name.matches("[a-zA-Z0-9-_]{1,80}"))
      (new QueueName(name)).asRight[String]
    else
      s"Queue name: $name must be alphanumeric (- and _ are allowed as well) and no more than 80 chars"
        .asLeft[QueueName]
}

sealed trait Stage

abstract case class DecodeStage[R, O[_]](req: R) extends Stage {

  def decode[F[_], A](mediaType: MediaType, strict: Boolean)(
      implicit ED: EntityDecoder[F, A]): SendWithDecodeStage[O[A], F]
}

abstract class SendStage[R, O](val req: R) extends Stage {

  def send[F[_]](implicit A: Async[F]): F[O]
}

abstract class SendWithDecodeStage[O, F[_]] extends Stage {

  def send(implicit A: Async[F]): F[O]
}

abstract class BuilderStage[B <: AwsRequest.Builder, S <: Stage, C <: SdkClient](
    builder: B,
    client: C)
    extends Stage {

  def build(): S
}

case class SendMessageResponse(
    md5OfMessageAttributes: String,
    md5OfMessageBody: String,
    messageId: String,
    // only for FIFO queues
    sequenceNumber: Option[String]
)

sealed trait Action
case object SendMessage extends Action
case object DeleteMessage extends Action
case object ChangeMessageVisibility extends Action
