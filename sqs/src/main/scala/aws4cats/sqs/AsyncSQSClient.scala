package aws4cats.sqs

import aws4cats.{AccountId, BaseSdkAsyncClientBuilder, ExecutorServiceWrapper}
import aws4cats.internal._
import aws4cats.sqs.builder._
import cats.effect._
import cats.implicits._
import software.amazon.awssdk.core.client.config.{
  ClientAsyncConfiguration,
  SdkAdvancedAsyncClientOption
}
import software.amazon.awssdk.services.sqs.model._
import software.amazon.awssdk.services.sqs._
import fs2.text
import io.chrisdavenport.log4cats.Logger
import org.http4s.{EntityEncoder, Uri}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

sealed abstract class AsyncSQSClient[F[_]](client: SqsAsyncClient)(
  implicit A: Async[F],
  L: Logger[F]
) extends SQSClient[F] {

  override def addPermission(
    queueUri: Uri,
    actions: List[Action],
    accountIds: List[AccountId],
    label: Label
  ): F[Unit] =
    A.async { cb =>
      val req = AddPermissionRequest
        .builder()
        .actions(actions.map(_.toString).asJavaCollection)
        .awsAccountIds(accountIds.map(_.value): _*)
        .label(label.value)
        .build()
      client.addPermission(req).handleVoidResult(cb)
    }

  override def changeMessageVisibility(
    queueUri: Uri,
    receiptHandle: ReceiptHandle,
    visibilityTimeout: FiniteDuration
  ): F[Unit] =
    A.async { cb =>
      val req = ChangeMessageVisibilityRequest
        .builder()
        .queueUrl(queueUri.renderString)
        .receiptHandle(receiptHandle.handle)
        .visibilityTimeout(visibilityTimeout.toSeconds.toInt)
        .build()
      client.changeMessageVisibility(req).handleVoidResult(cb)
    }

  override def createQueue(
    queueName: QueueName,
    attributes: Pair*
  ): F[Uri] =
    A.async { cb =>
      val req = CreateQueueRequest
        .builder()
        .queueName(queueName.value)
        .attributes(
          attributes
            .map { pair =>
              val (k, v) = pair.repr
              k.queueAttributeName -> v
            }
            .toMap
            .asJava)
        .build()
      client
        .createQueue(req)
        .handleResultE(
          cb,
          resp =>
            Uri
              .fromString(resp.queueUrl())
              .leftMap(err => new Exception(err.message))
        )
    }

  override def deleteMessage(
    queue: Uri,
    receiptHandle: ReceiptHandle
  ): F[Unit] =
    A.async { cb =>
      val req =
        DeleteMessageRequest
          .builder()
          .queueUrl(queue.renderString)
          .receiptHandle(receiptHandle.handle)
          .build()
      client.deleteMessage(req).handleVoidResult(cb)
    }

  override def deleteQueue(queue: Uri): F[Unit] =
    A.async { cb =>
      val req =
        DeleteQueueRequest.builder().queueUrl(queue.renderString).build()
      client.deleteQueue(req).handleVoidResult(cb)
    }

  override def getQueueAttributes(
    queue: Uri,
    attributes: List[QueueAttributeName]): F[Map[QueueAttributeName, String]] =
    A.async { cb =>
      val req = GetQueueAttributesRequest
        .builder()
        .queueUrl(queue.renderString)
        .attributeNames(attributes.map(_.queueAttributeName).asJavaCollection)
        .build()
      client
        .getQueueAttributes(req)
        .handleResult(
          cb,
          _.attributes().asScala.toMap.map {
            case (k, v) => QueueAttributeName.fromEnum(k) -> v
          }
        )
    }

  override def getQueueUrl(
    queueName: QueueName,
    ownerAccountId: Option[AccountId] = None): F[Uri] =
    A.async[Uri] { cb =>
      val rb = GetQueueUrlRequest.builder().queueName(queueName.value)
      val req = ownerAccountId.fold(rb.build())(id =>
        rb.queueOwnerAWSAccountId(id.value).build)
      client
        .getQueueUrl(req)
        .handleResultE(
          cb,
          res =>
            Uri
              .fromString(res.queueUrl())
              .leftMap(err => new Exception(err.message)))

    }

  override def listQueues(prefix: String): F[List[Uri]] =
    A.async { cb =>
      val req = ListQueuesRequest.builder().queueNamePrefix(prefix).build()
      client
        .listQueues(req)
        .handleResultE(
          cb,
          _.queueUrls().asScala.toList
            .traverse(Uri.fromString)
            .leftMap(err => new Exception(err.message))
        )
    }

  override def purgeQueue(queue: Uri): F[Unit] =
    A.async { cb =>
      val req =
        PurgeQueueRequest.builder().queueUrl(queue.renderString).build()
      client.purgeQueue(req).handleVoidResult(cb)
    }

  override def receiveMessage(
    queueUri: Uri
  ): BaseReceiveMessageBuilder =
    new ReceiveMessageBuilder(
      ReceiveMessageRequest
        .builder()
        .queueUrl(queueUri.renderString)
        .attributeNames(),
      client
    )

  override def sendMessage[M](
    queue: Uri,
    message: M
  )(implicit EE: EntityEncoder[F, M]): F[SendMessageResponse] =
    EE.toEntity(message)
      .body
      .through(text.utf8Decode)
      .compile
      .lastOrError
      .flatMap(
        body =>
          A.async { cb =>
            val req = SendMessageRequest
              .builder()
              .queueUrl(queue.renderString)
              .messageBody(body)
              .build()
            client
              .sendMessage(req)
              .handleResult(
                cb,
                res =>
                  SendMessageResponse(
                    res.md5OfMessageAttributes(),
                    res.md5OfMessageBody(),
                    res.messageId(),
                    Option(res.sequenceNumber())
                )
              )
        }
      )

  override def setQueueAttributes(
    queue: Uri,
    attributes: Pair*
  ): F[Unit] =
    A.async { cb =>
      val req =
        SetQueueAttributesRequest
          .builder()
          .queueUrl(queue.renderString)
          .attributes(
            attributes
              .map { pair =>
                val (k, v) = pair.repr
                k.queueAttributeName -> v
              }
              .toMap
              .asJava)
          .build()
      client.setQueueAttributes(req).handleVoidResult(cb)
    }

}

sealed abstract class AsyncSQSClientBuilder[F[_]: Effect: ContextShift: Logger](
  builder: SqsAsyncClientBuilder,
  ecR: Resource[F, ExecutionContext]
) extends BaseSdkAsyncClientBuilder[
    SqsAsyncClientBuilder,
    SqsAsyncClient,
    F,
    SQSClient,
    AsyncSQSClientBuilder[F]](builder) {

  override protected def copy(
    modify: SqsAsyncClientBuilder => SqsAsyncClientBuilder)
    : AsyncSQSClientBuilder[F] =
    new AsyncSQSClientBuilder[F](
      builder = modify(cloner.deepClone(builder)),
      ecR = ecR
    ) {}

  override def resource: Resource[F, SQSClient[F]] =
    ecR.flatMap { ec =>
      Resource[F, SQSClient[F]] {
        val b = builder
          .asyncConfiguration(
            ClientAsyncConfiguration
              .builder()
              .advancedOption(
                SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR,
                new ExecutorServiceWrapper(ec)
              )
              .build()
          )
          .build()
        (((new AsyncSQSClient[F](b) {}): SQSClient[F]) -> Sync[F].delay(
          b.close())).pure[F]
      }
    }

}

object AsyncSQSClientBuilder {

  def apply[F[_]: Effect: Logger: ContextShift](
    ecR: Resource[F, ExecutionContext]
  ): AsyncSQSClientBuilder[F] =
    new AsyncSQSClientBuilder[F](
      SqsAsyncClient.builder(),
      ecR
    ) {}

}
