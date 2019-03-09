package aws4cats.sqs

import java.net.URI

import aws4cats.ExecutorServiceWrapper
import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import software.amazon.awssdk.auth.credentials.{AwsCredentialsProvider, AwsCredentialsProviderChain, DefaultCredentialsProvider}
import software.amazon.awssdk.core.client.config.{ClientAsyncConfiguration, SdkAdvancedAsyncClientOption}
import software.amazon.awssdk.services.sqs.model.{Message => AwsMessage, _}
import software.amazon.awssdk.services.sqs._
import fs2.{Stream, text}
import io.chrisdavenport.log4cats.Logger
import org.http4s.headers.`Content-Type`
import org.http4s.{EntityDecoder, EntityEncoder, Headers, MediaType, Response => Http4sResp, Uri}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

sealed abstract class AsyncSQSClient[F[_]](client: SqsAsyncClient)(
    implicit E: Effect[F],
    L: Logger[F]
) extends SQSClient[F] {

  override def changeMessageVisibility(
    queueUri: QueueUri,
    receiptHandle:  ReceiptHandle,
    visibilityTimeout:  FiniteDuration
  ): F[Unit] =
    E.async{ cb =>
      val r = ChangeMessageVisibilityRequest.builder()
        .queueUrl(queueUri.uri.renderString)
        .receiptHandle(receiptHandle.repr)
        .visibilityTimeout(visibilityTimeout.toSeconds.toInt)
        .build()
      client.changeMessageVisibility(r).handle[Unit]((_, err) =>
        Option(err).fold(cb(Right(())))(err => cb(Left(err)))
      )
    }

  override def createQueue(queueName: String): F[Uri] =
    E.async { cb =>
      val r = CreateQueueRequest.builder().queueName(queueName).build()
      client.createQueue(r).handle[Unit]((res, err) => Option(res).fold(cb(Left(err)))(resp =>
        Uri.fromString(resp.queueUrl())
            .fold(err => cb(Left(new Exception(err.message))),
              uri => cb(Right(uri))
            )
      ))
    }

  override def deleteMessage[M](
      queue: QueueUri,
      receiptHandle: ReceiptHandle
  ): F[Unit] = {
    E.async { cb =>
      val r =
        DeleteMessageRequest.builder()
          .queueUrl(queue.uri.renderString)
          .receiptHandle(receiptHandle.repr)
        .build()
      client.deleteMessage(r).handle[Unit]((_, err) =>
        Option(err).fold(cb(Right(())))(err => cb(Left(err)))
      )
    }
  }

  override def deleteQueue(queue: QueueUri): F[Unit] =
    E.async{ cb =>
      val r = DeleteQueueRequest.builder().queueUrl(queue.uri.renderString).build()
      client.deleteQueue(r).handle[Unit]((_, err) => Option(err).fold(cb(Right(())))(err =>
      cb(Left(err))))
    }


  override def getQueueAttributes(
      queue: QueueUri,
      attributes: List[QueueAttributeName]): F[Map[QueueAttributeName, String]] =
    E.async { cb =>
      val r = GetQueueAttributesRequest.builder()
        .queueUrl(queue.uri.renderString)
        .attributeNames(attributes.asJavaCollection)
        .build()
      client.getQueueAttributes(r).handle[Unit]((res, err) => Option(res).fold(cb(Left(err)))(res =>
        cb(Right(res.attributes().asScala.toMap))
      ))
    }

  override def getQueueUrl(queue: QueueUri): F[Uri] = {
    E.fromEither(
        Either.catchNonFatal(
          queue.uri.renderString.split('\\').last
        )
      )
      .flatMap(queueName =>
        E.async[Uri] { cb =>
          val r = GetQueueUrlRequest.builder().queueName(queueName).build()
          client.getQueueUrl(r).handle[Unit]((res, err) => Option(res).fold(cb(Left(err)))(res =>
            Uri
                .fromString(res.queueUrl())
                .fold(err => cb(Left(new Exception(err.message))),
                      uri => cb(Right(uri)))
          ))
      })
  }

  override def listQueues(prefix: String): F[List[QueueUri]] =
    E.async{ cb =>
      val r = ListQueuesRequest.builder().queueNamePrefix(prefix).build()
      client.listQueues(r).handle[Unit]((res, err) => Option(res).fold(cb(Left(err)))(_.queueUrls().asScala.toList.traverse(Uri.fromString)
            .fold(err => cb(Left(new Exception(err))), uris => cb(Right(uris.map(QueueUri(_)))))))
    }

  override def purgeQueue(queue: QueueUri): F[Unit] = {
    E.async { cb =>
      val r = PurgeQueueRequest.builder().queueUrl(queue.uri.renderString).build()
      client.purgeQueue(r).handle[Unit]((_, err) =>
        Option(err).fold(cb(Right(())))(err => cb(Left(err)))
      )
    }
  }

  override def receiveMessage[M](
      queue: QueueUri,
      options: ReceiveMessageOptions
  )(implicit
    ED: EntityDecoder[F, M]): F[List[Either[SQSDecodeFailure, Message[M]]]] =
    E.async[List[AwsMessage]] { cb =>
        val r = ReceiveMessageRequest.builder()
          .maxNumberOfMessages(options.numMessage)
          .build()
        client.receiveMessage(r).handle[Unit]((res, err) => Option(res).fold(cb(Left(err)))(res =>
          cb(Right(res.messages().asScala.toList))
        ))
      }
      .flatMap(_.traverse { message =>
        ED.decode(
            Http4sResp[F](
              headers = Headers(`Content-Type`(MediaType.application.json)),
              body = Stream(message.body()).through(text.utf8Encode)
            ),
            true
          )
          .bimap(
            SQSDecodeFailure(_, message.receiptHandle()),
            Message(message.receiptHandle(), _)
          )
          .value
      })

  override def sendMessage[M](
      queue: QueueUri,
      message: M
  )(implicit EE: EntityEncoder[F, M]): F[SendMessageResponse] =
    E.async { cb =>
      val r = SendMessageRequest
        .builder()
        .queueUrl(queue.uri.renderString)
        .messageBody(
          EE.toEntity(message)
            .body
            .through(text.utf8Decode)
            .compile
            .lastOrError
            .toIO
            .unsafeRunSync()
        )
        .build()
      client.sendMessage(r).handle[Unit]((res, err) => Option(res).fold(cb(Left(err)))(res =>
      cb(Right(
        SendMessageResponse(
          res.md5OfMessageAttributes(),
          res.md5OfMessageBody(),
          res.messageId()
        )
      ))))
    }


  override def setQueueAttributes(
    queue: QueueUri,
    attributes: Map[QueueAttributeName, String]
  ): F[Unit] =
    E.async{ cb =>
      val r = SetQueueAttributesRequest
        .builder()
        .queueUrl(queue.uri.renderString)
        .attributes(attributes.asJava)
        .build()
      client.setQueueAttributes(r).handle[Unit]((_, err) =>
        Option(err).fold(cb(Right(())))(err => cb(Left(err)))
      )
    }

}

object AsyncSQSClient {

  def apply[F[_]: Effect: Logger](
      ecR: Resource[F, ExecutionContext],
      endpointConfiguration: Option[Uri] = None,
      credentialsProvider: AwsCredentialsProvider =
        DefaultCredentialsProvider.create()
  ): Resource[F, AsyncSQSClient[F]] = {
    ecR.map { ec =>
      val builder = SqsAsyncClient.builder()
        .credentialsProvider(credentialsProvider)
        .asyncConfiguration(
          ClientAsyncConfiguration.builder()
          .advancedOption(
            SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR,
            new ExecutorServiceWrapper(ec)
          ).build()
        )
      new AsyncSQSClient[F](endpointConfiguration.fold(builder.build())(uri =>
        builder.endpointOverride(URI.create(uri.renderString)).build())
      ) {}
    }
  }
}
