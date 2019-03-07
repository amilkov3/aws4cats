package aws4cats.sqs

import org.http4s.{EntityDecoder, EntityEncoder, Uri}

import scala.concurrent.duration.FiniteDuration

trait SQSClient[F[_]] {

  def changeMessageVisibility(
    queueUri: QueueUri,
    receiptHandle: ReceiptHandle,
    visibilityTimeout: FiniteDuration
  ): F[Unit]

  def createQueue(queueName: String): F[Uri]

  def deleteMessage[M](
      queue: QueueUri,
      receiptHandle: ReceiptHandle
  ): F[Unit]

  def deleteQueue(queue: QueueUri): F[Unit]

  //TODO: Consider making returned map type safe
  def getQueueAttributes(
      queue: QueueUri,
      attributes: List[ReadableQueueAttributes]): F[Map[String, String]]

  def getQueueUrl(queue: QueueUri): F[Uri]

  def listQueues(prefix: String): F[List[QueueUri]]

  def purgeQueue(queue: QueueUri): F[Unit]

  def receiveMessage[M](
    //TODO: Maybe just use `Uri` and have a rich `fromParts`
      queue: QueueUri,
      options: ReceiveMessageOptions
  )(implicit
    ED: EntityDecoder[F, M]): F[List[Either[SQSDecodeFailure, Message[M]]]]

  def sendMessage[M](
      queue: QueueUri,
      message: M
  )(implicit EE: EntityEncoder[F, M]): F[SendMessageResponse]


  def setQueueAttributes(
    queue: QueueUri,
    attributes: Map[String, String]
  ): F[Unit]

}
