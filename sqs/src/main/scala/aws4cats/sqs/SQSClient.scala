package aws4cats.sqs

import aws4cats.AccountId
import aws4cats.sqs.builder._
import org.http4s.{EntityEncoder, Uri}

import scala.concurrent.duration.FiniteDuration

trait SQSClient[F[_]] {

  def addPermission(
    queueUri: Uri,
    actions: List[Action],
    accountIds: List[AccountId],
    label: Label
  ): F[Unit]

  def changeMessageVisibility(
    queueUri: Uri,
    receiptHandle: ReceiptHandle,
    visibilityTimeout: FiniteDuration
  ): F[Unit]

  //TODO: hmap?
  def createQueue(
    queueName: QueueName,
    attributes: Map[QueueAttributeName, String] = Map.empty
  ): F[Uri]

  def deleteMessage(
    queueUri: Uri,
    receiptHandle: ReceiptHandle
  ): F[Unit]

  def deleteQueue(queue: Uri): F[Unit]

  //TODO: hmap?
  def getQueueAttributes(
    queueUri: Uri,
    attributes: List[QueueAttributeName]): F[Map[QueueAttributeName, String]]

  def getQueueUrl(
    queueName: QueueName,
    ownerAccountId: Option[AccountId] = None): F[Uri]

  def listQueues(prefix: String): F[List[Uri]]

  def purgeQueue(queueUri: Uri): F[Unit]

  def receiveMessage(
    queueUri: Uri
  ): BaseReceiveMessageBuilder

  def sendMessage[M](
    queueUri: Uri,
    message: M
  )(implicit EE: EntityEncoder[F, M]): F[SendMessageResponse]

  def setQueueAttributes(
    queueUri: Uri,
    //TODO: hmap?
    attributes: Map[QueueAttributeName, String]
  ): F[Unit]

}
