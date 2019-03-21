package aws4cats.sqs

import java.net.URI

import aws4cats.{
  sqs,
  AccountId,
  BaseSdkAsyncClientBuilder,
  ExecutorServiceWrapper
}
import aws4cats.internal._
import aws4cats.sqs.builder._
import cats.effect._
import cats.implicits._
import software.amazon.awssdk.auth.credentials.{
  AwsCredentialsProvider,
  AwsCredentialsProviderChain,
  DefaultCredentialsProvider
}
import software.amazon.awssdk.core.client.config.{
  ClientAsyncConfiguration,
  SdkAdvancedAsyncClientOption
}
import software.amazon.awssdk.services.sqs.model.{
  Message => AwsMessage,
  ReceiveMessageResponse => AwsReceiveMessageResponse,
  QueueAttributeName => AwsQueueAttributeName,
  _
}
import software.amazon.awssdk.services.sqs._
import fs2.{text, Stream}
import io.chrisdavenport.log4cats.Logger
import org.http4s.headers.`Content-Type`
import org.http4s.{
  DecodeFailure,
  EntityDecoder,
  EntityEncoder,
  Headers,
  MediaType,
  Uri,
  Response => Http4sResp
}

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
    label: String
  ): F[Unit] =
    A.async { cb =>
      val r = AddPermissionRequest
        .builder()
        .actions(actions.map(_.toString).asJavaCollection)
        .awsAccountIds(accountIds.map(_.id.toString): _*)
        .label(label)
        .build()
      client.addPermission(r).handleVoidResult(cb)
    }

  override def changeMessageVisibility(
    queueUri: Uri,
    receiptHandle: ReceiptHandle,
    visibilityTimeout: FiniteDuration
  ): F[Unit] =
    A.async { cb =>
      val r = ChangeMessageVisibilityRequest
        .builder()
        .queueUrl(queueUri.renderString)
        .receiptHandle(receiptHandle.repr)
        .visibilityTimeout(visibilityTimeout.toSeconds.toInt)
        .build()
      client.changeMessageVisibility(r).handleVoidResult(cb)
    }

  override def createQueue(
    queueName: QueueName,
    attributes: Map[QueueAttributeName, String] = Map.empty
  ): F[Uri] =
    A.async { cb =>
      val r = CreateQueueRequest
        .builder()
        .queueName(queueName.name)
        .attributes(attributes.asJava)
        .build()
      client
        .createQueue(r)
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
      val r =
        DeleteMessageRequest
          .builder()
          .queueUrl(queue.renderString)
          .receiptHandle(receiptHandle.repr)
          .build()
      client.deleteMessage(r).handleVoidResult(cb)
    }

  override def deleteQueue(queue: Uri): F[Unit] =
    A.async { cb =>
      val r =
        DeleteQueueRequest.builder().queueUrl(queue.renderString).build()
      client.deleteQueue(r).handleVoidResult(cb)
    }

  override def getQueueAttributes(
    queue: Uri,
    attributes: List[QueueAttributeName]): F[Map[QueueAttributeName, String]] =
    A.async { cb =>
      val r = GetQueueAttributesRequest
        .builder()
        .queueUrl(queue.renderString)
        .attributeNames(attributes.map(_.queueAttributeName).asJavaCollection)
        .build()
      client
        .getQueueAttributes(r)
        .handleResult(cb, _.attributes().asScala.toMap)
    }

  override def getQueueUrl(
    queueName: QueueName,
    ownerAccountId: Option[AccountId] = None): F[Uri] =
    A.async[Uri] { cb =>
      val rb = GetQueueUrlRequest.builder().queueName(queueName.name)
      val r = ownerAccountId.fold(rb.build())(id =>
        rb.queueOwnerAWSAccountId(id.id.toString).build)
      client
        .getQueueUrl(r)
        .handleResultE(
          cb,
          res =>
            Uri
              .fromString(res.queueUrl())
              .leftMap(err => new Exception(err.message)))

    }

  override def listQueues(prefix: String): F[List[Uri]] =
    A.async { cb =>
      val r = ListQueuesRequest.builder().queueNamePrefix(prefix).build()
      client
        .listQueues(r)
        .handleResultE(
          cb,
          _.queueUrls().asScala.toList
            .traverse(Uri.fromString)
            .leftMap(err => new Exception(err.message))
        )
    }

  override def purgeQueue(queue: Uri): F[Unit] =
    A.async { cb =>
      val r =
        PurgeQueueRequest.builder().queueUrl(queue.renderString).build()
      client.purgeQueue(r).handleVoidResult(cb)
    }

  override def receiveMessage[M](
    queueUri: Uri
  ): ReceiveMessageBuilder =
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
            val r = SendMessageRequest
              .builder()
              .queueUrl(queue.renderString)
              .messageBody(body)
              .build()
            client
              .sendMessage(r)
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
    attributes: Map[QueueAttributeName, String]
  ): F[Unit] =
    A.async { cb =>
      val r =
        SetQueueAttributesRequest
          .builder()
          .queueUrl(queue.renderString)
          .attributes(attributes.map {
            case (k, v) => k.queueAttributeName -> v
          }.asJava)
          .build()
      client.setQueueAttributes(r).handleVoidResult(cb)
    }

}

sealed abstract class AsyncSQSClientBuilder[F[_]: Effect: ContextShift: Logger](
  builder: SqsAsyncClientBuilder,
  ecR: Resource[F, ExecutionContext]
) extends BaseSdkAsyncClientBuilder[SqsAsyncClientBuilder, SqsAsyncClient](
    builder) {

  def resource: Resource[F, SQSClient[F]] =
    ecR.map(
      ec =>
        new AsyncSQSClient[F](
          builder
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
        ) {}
    )

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
