package aws4cats.sqs

import aws4cats.AccountId
import org.http4s.{EntityDecoder, EntityEncoder, MediaType, Uri}
import software.amazon.awssdk.core.SdkClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{
  QueueAttributeName,
  ReceiveMessageRequest
}

import scala.concurrent.duration.FiniteDuration

trait SQSClient[F[_]] {

  def addPermission(
      queueUri: Uri,
      actions: List[Action],
      accountIds: List[AccountId],
      label: String
  ): F[Unit]

  def changeMessageVisibility(
      queueUri: Uri,
      receiptHandle: ReceiptHandle,
      visibilityTimeout: FiniteDuration
  ): F[Unit]

  //TODO: hmap?
  def createQueue(
      queueName: QueueName,
      attributes: Map[QueueAttributeName, String]
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

  def getQueueUrl(queueName: QueueName,
                  ownerAccountId: Option[AccountId] = None): F[Uri]

  def listQueues(prefix: String): F[List[Uri]]

  def purgeQueue(queueUri: Uri): F[Unit]

  def receiveMessage[M](
      queueUri: Uri
  ): BuilderStage[
    ReceiveMessageRequest.Builder,
    DecodeStage[ReceiveMessageRequest, λ[A => List[ReceiveMessageResponse[A]]]],
    //TODO: dont want an async constriant in this trait
    SqsAsyncClient]

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
