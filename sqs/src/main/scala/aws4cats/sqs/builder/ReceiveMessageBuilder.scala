package aws4cats.sqs.builder

import aws4cats.sqs.{ReceiptHandle, ReceiveMessageResponse}
import aws4cats.BuilderStage
import aws4cats.internal._
import cats.effect.Async
import fs2.{text, Stream}
import org.http4s.headers.`Content-Type`
import org.http4s.{EntityDecoder, Headers, Response => Http4sResp}
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{Message => AwsMessage, _}

import scala.collection.JavaConverters._
import cats.implicits._

import scala.concurrent.duration._

abstract class BaseReceiveMessageBuilder(
  builder: ReceiveMessageRequest.Builder,
  client: SqsAsyncClient)
  extends BuilderStage[
    ReceiveMessageRequest.Builder,
    DecodeStage[ReceiveMessageRequest, λ[A => List[ReceiveMessageResponse[A]]]],
    SqsAsyncClient
  ](builder, client) {

  def attributesNames(
    names: List[QueueAttributeName]): BaseReceiveMessageBuilder

  def maxNumberOfMessages(n: Int): BaseReceiveMessageBuilder

  def messageAttributeNames(names: List[String]): BaseReceiveMessageBuilder

  def receiveRequestAttemptId(id: String): BaseReceiveMessageBuilder

  def visibilityTimeOut(timeout: FiniteDuration): BaseReceiveMessageBuilder

  def waitTimeSeconds(time: FiniteDuration): BaseReceiveMessageBuilder

}

private[sqs] class ReceiveMessageBuilder(
  builder: ReceiveMessageRequest.Builder,
  client: SqsAsyncClient
) extends BaseReceiveMessageBuilder(builder, client) {
  self =>

  def attributesNames(
    names: List[QueueAttributeName]): ReceiveMessageBuilder = {
    builder.attributeNames(names.asJava)
    self
  }

  def maxNumberOfMessages(n: Int): ReceiveMessageBuilder = {
    builder.maxNumberOfMessages(n)
    self
  }

  def messageAttributeNames(names: List[String]): ReceiveMessageBuilder = {
    builder.messageAttributeNames(names.asJava)
    self
  }

  def receiveRequestAttemptId(id: String): ReceiveMessageBuilder = {
    builder.receiveRequestAttemptId(id)
    self
  }

  def visibilityTimeOut(timeout: FiniteDuration): ReceiveMessageBuilder = {
    builder.visibilityTimeout(timeout.toSeconds.toInt)
    self
  }

  def waitTimeSeconds(time: FiniteDuration): ReceiveMessageBuilder = {
    builder.waitTimeSeconds(time.toSeconds.toInt)
    self
  }

  override def build(): DecodeStage[
    ReceiveMessageRequest,
    λ[A => List[ReceiveMessageResponse[A]]]] =
    new DecodeStage[
      ReceiveMessageRequest,
      λ[A => List[ReceiveMessageResponse[A]]]](builder.build()) {
      self =>

      override def decode[F[_], A](
        mediaType: _root_.org.http4s.MediaType,
        strict: Boolean)(implicit ED: EntityDecoder[F, A])
        : SendWithDecodeStage[List[ReceiveMessageResponse[A]], F] =
        new SendWithDecodeStage[List[ReceiveMessageResponse[A]], F] {
          override def send(
            implicit A: Async[F]): F[List[ReceiveMessageResponse[A]]] =
            A.async[List[AwsMessage]] { cb =>
                client
                  .receiveMessage(self.req)
                  .handleResult(cb, _.messages().asScala.toList)
              }
              .flatMap(
                _.traverse { resp =>
                  ED.decode(
                      Http4sResp[F](
                        headers = Headers(`Content-Type`(mediaType)),
                        body = Stream(resp.body()).through(text.utf8Encode)
                      ),
                      strict
                    )
                    .value
                    .map(
                      message =>
                        ReceiveMessageResponse(
                          ReceiptHandle(resp.receiptHandle()),
                          resp.attributes().asScala.toMap,
                          message
                      )
                    )
                }
              )

        }
    }

}
