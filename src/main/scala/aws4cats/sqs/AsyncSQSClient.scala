package aws4cats.sqs

import java.util.concurrent.ExecutorService

import aws4cats.{ExecutorServiceWrapper, Region}
import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import com.amazonaws.auth.{AWSCredentialsProvider, DefaultAWSCredentialsProviderChain}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.client.builder.ExecutorFactory
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sqs.model.{Message => AwsMessage, _}
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClient}
import fs2.{Stream, text}
import io.chrisdavenport.log4cats.Logger
import org.http4s.headers.`Content-Type`
import org.http4s.{EntityDecoder, EntityEncoder, Headers, MediaType, Response, Uri}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

sealed abstract class AsyncSQSClient[F[_]](client: AmazonSQSAsync)(
    implicit E: Effect[F],
    L: Logger[F]
) extends SQSClient[F] {

  override def changeMessageVisibility(
    queueUri: QueueUri,
    receiptHandle:  ReceiptHandle,
    visibilityTimeout:  FiniteDuration
  ): F[Unit] =
    E.async{ cb =>
      val r = new ChangeMessageVisibilityRequest(
        queueUri.uri.renderString,
        receiptHandle.repr,
        visibilityTimeout.toSeconds.toInt
      val h = new AsyncHandler[ChangeMessageVisibilityRequest, ChangeMessageVisibilityResult] {
        override def onError(exception: Exception): Unit = cb(Left(exception))
        override def onSuccess(request: ChangeMessageVisibilityRequest, result: ChangeMessageVisibilityResult): Unit =
          cb(Right(()))
      }
      client.changeMessageVisibilityAsync(r, h)
    }

  override def createQueue(queueName: String): F[Uri] =
    E.async { cb =>
      val r = new CreateQueueRequest(queueName)
      val h = new AsyncHandler[CreateQueueRequest, CreateQueueResult] {
        override def onError(exception: Exception): Unit = cb(Left(exception))
        override def onSuccess(request: CreateQueueRequest,
                               result: CreateQueueResult): Unit =
          Uri.fromString(result.getQueueUrl)
            .fold(err => cb(Left(new Exception(err.message))),
              uri => cb(Right(uri))
            )
      }
      client.createQueueAsync(r, h)
    }

  override def deleteMessage[M](
      queue: QueueUri,
      receiptHandle: ReceiptHandle
  ): F[Unit] = {
    E.async[Unit] { cb =>
      val r =
        new DeleteMessageRequest(queue.uri.renderString, receiptHandle.repr)
      val h = new AsyncHandler[DeleteMessageRequest, DeleteMessageResult] {
        override def onError(exception: Exception): Unit = cb(Left(exception))
        override def onSuccess(request: DeleteMessageRequest,
                               result: DeleteMessageResult): Unit =
          cb(Right(()))
      }
      client.deleteMessageAsync(r, h)
    }
  }

  override def deleteQueue(queue: QueueUri): F[Unit] =
    E.async{ cb =>
      val h = new AsyncHandler[DeleteQueueRequest, DeleteQueueResult]{
        override def onError(exception: Exception): Unit =
          cb(Left(exception))
        override def onSuccess(request: DeleteQueueRequest, result: DeleteQueueResult): Unit =
          cb(Right(()))
      }
      client.deleteQueueAsync(queue.uri.renderString, h)
    }


  override def getQueueAttributes(
      queue: QueueUri,
      attributes: List[ReadableQueueAttributes]): F[Map[String, String]] =
    E.async { cb =>
      val r = new GetQueueAttributesRequest(
        queue.uri.renderString,
        ReadableQueueAttributes.asJavaList(attributes)
      )
      val h =
        new AsyncHandler[GetQueueAttributesRequest, GetQueueAttributesResult] {
          override def onError(exception: Exception): Unit = cb(Left(exception))
          override def onSuccess(request: GetQueueAttributesRequest,
                                 result: GetQueueAttributesResult): Unit =
            cb(Right(result.getAttributes.asScala.toMap))
        }
      client.getQueueAttributesAsync(r, h)
    }

  override def getQueueUrl(queue: QueueUri): F[Uri] = {
    E.fromEither(
        Either.catchNonFatal(
          queue.uri.renderString.split('\\').last
        )
      )
      .flatMap(queueName =>
        E.async[Uri] { cb =>
          val h = new AsyncHandler[GetQueueUrlRequest, GetQueueUrlResult] {
            override def onError(exception: Exception): Unit =
              cb(Left(exception))
            override def onSuccess(
                request: _root_.com.amazonaws.services.sqs.model.GetQueueUrlRequest,
                result: _root_.com.amazonaws.services.sqs.model.GetQueueUrlResult)
              : Unit =
              Uri
                .fromString(result.getQueueUrl)
                .fold(err => cb(Left(new Exception(err.message))),
                      uri => cb(Right(uri)))
          }
          client.getQueueUrlAsync(queueName, h)
      })
  }

  override def listQueues(prefix: String): F[List[QueueUri]] =
    E.async{ cb =>
      val h = new AsyncHandler[ListQueuesRequest, ListQueuesResult] {
        override def onError(exception: Exception): Unit = cb(Left(exception))
        override def onSuccess(request: ListQueuesRequest, result: ListQueuesResult): Unit =
          result.getQueueUrls.asScala.toList.traverse(Uri.fromString)
            .fold(err => cb(Left(new Exception(err))), uris => cb(Right(uris)))
      }
      client.listQueuesAsync(prefix, h)
    }

  override def purgeQueue(queue: QueueUri): F[Unit] = {
    E.async { cb =>
      val r = new PurgeQueueRequest(queue.uri.renderString)
      val h = new AsyncHandler[PurgeQueueRequest, PurgeQueueResult] {
        override def onError(exception: Exception): Unit = cb(Left(exception))
        override def onSuccess(request: PurgeQueueRequest,
                               result: PurgeQueueResult): Unit =
          cb(Right(()))
      }
      client.purgeQueueAsync(r, h)
    }
  }

  override def receiveMessage[M](
      queue: QueueUri,
      options: ReceiveMessageOptions
  )(implicit
    ED: EntityDecoder[F, M]): F[List[Either[SQSDecodeFailure, Message[M]]]] =
    E.async[List[AwsMessage]] { cb =>
        val r = new ReceiveMessageRequest(queue.uri.renderString)
        r.setMaxNumberOfMessages(options.numMessage)
        val h =
          new AsyncHandler[ReceiveMessageRequest, ReceiveMessageResult] {
            override def onError(exception: Exception): Unit =
              cb(Left(exception))
            override def onSuccess(request: ReceiveMessageRequest,
                                   result: ReceiveMessageResult): Unit =
              cb(Right(result.getMessages.asScala.toList))
          }
        client.receiveMessageAsync(r, h)
      }
      .flatMap(_.traverse { message =>
        ED.decode(
            Response[F](
              headers = Headers(`Content-Type`(MediaType.application.json)),
              body = Stream(message.getBody).through(text.utf8Encode)
            ),
            true
          )
          .bimap(
            SQSDecodeFailure(_, message.getReceiptHandle),
            Message(message.getReceiptHandle, _)
          )
          .value
      })

  override def sendMessage[M](
      queue: QueueUri,
      message: M
  )(implicit EE: EntityEncoder[F, M]): F[SendMessageResponse] =
    E.async { cb =>
      val r = new SendMessageRequest(
        queue.uri.renderString,
        EE.toEntity(message)
          .body
          .through(text.utf8Decode)
          .compile
          .lastOrError
          .toIO
          .unsafeRunSync()
      )
      val h = new AsyncHandler[SendMessageRequest, SendMessageResult] {
        override def onError(exception: Exception): Unit = cb(Left(exception))

        override def onSuccess(request: SendMessageRequest,
                               result: SendMessageResult): Unit =
          cb(
            Right(
              SendMessageResponse(
                result.getMD5OfMessageAttributes,
                result.getMD5OfMessageBody,
                result.getMessageId
              )
            ))

      }
      client.sendMessageAsync(r, h)
    }


  override def setQueueAttributes(
    queue: QueueUri,
    attributes: Map[String, String]
  ): F[Unit] =
    E.async{ cb =>
      val h = new AsyncHandler[SetQueueAttributesRequest, SetQueueAttributesResult] {
        override def onError(exception: Exception): Unit = cb(Left(exception))
        override def onSuccess(request: SetQueueAttributesRequest, result: SetQueueAttributesResult): Unit =
          cb(Right(()))

      }
      client.setQueueAttributesAsync(queue.uri.renderString, attributes.asJava, h)
    }

}

object AsyncSQSClient {

  def apply[F[_]: Effect: Logger](
      ecR: Resource[F, ExecutionContext],
      endpointConfiguration: Option[(Uri, Region)] = None,
      credentialsProvider: AWSCredentialsProvider =
        new DefaultAWSCredentialsProviderChain
  ): Resource[F, AsyncSQSClient[F]] = {
    ecR.map { ec =>
      val cl = AmazonSQSAsyncClient
        .asyncBuilder()
        .withCredentials(credentialsProvider)
        .withExecutorFactory(new ExecutorFactory {
          override def newExecutor(): ExecutorService =
            new ExecutorServiceWrapper(ec)
        })
      new AsyncSQSClient[F](endpointConfiguration.fold(cl.build()){case (uri, region) =>
        cl.withEndpointConfiguration(new EndpointConfiguration(uri.renderString, region.toString)).build()}) {}
    }
  }
}
