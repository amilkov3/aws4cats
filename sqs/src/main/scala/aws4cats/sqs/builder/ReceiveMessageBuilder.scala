package aws4cats.sqs.builder

import aws4cats.sqs.{
  ReceiptHandle,
  ReceiveMessageResponse,
  ReceiveMessageWaitTimeSeconds,
  VisibilityTimeout
}
import aws4cats.BuilderStage
import aws4cats.internal._
import cats.effect.Async
import fs2.{text, Stream}
import org.http4s.headers.`Content-Type`
import org.http4s.{EntityDecoder, Headers, MediaType, Response => Http4sResp}
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{Message => AwsMessage, _}

import scala.collection.JavaConverters._
import cats.implicits._
import com.rits.cloning.Cloner

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

  // TODO: Opted out of refinement type here to not sully the API
  def maxNumberOfMessages(n: Int): BaseReceiveMessageBuilder

  def messageAttributeNames(names: List[String]): BaseReceiveMessageBuilder

  def receiveRequestAttemptId(id: String): BaseReceiveMessageBuilder

  // TODO: Opted out of refinement type here to not sully the API
  def visibilityTimeOut(timeout: FiniteDuration): BaseReceiveMessageBuilder

  // TODO: Opted out of refinement type here to not sully the API
  def waitTime(time: FiniteDuration): BaseReceiveMessageBuilder

}

private[sqs] class ReceiveMessageBuilder(
  builder: ReceiveMessageRequest.Builder,
  client: SqsAsyncClient
) extends BaseReceiveMessageBuilder(builder, client) {
  self =>

  private val cloner = new Cloner()

  protected def copy(
    modify: ReceiveMessageRequest.Builder => ReceiveMessageRequest.Builder)
    : ReceiveMessageBuilder =
    new ReceiveMessageBuilder(
      modify(cloner.deepClone(builder)),
      client
    )

  def attributesNames(names: List[QueueAttributeName]): ReceiveMessageBuilder =
    copy(_.attributeNames(names.asJava))

  def maxNumberOfMessages(n: Int): ReceiveMessageBuilder =
    copy(_.maxNumberOfMessages(n))

  def messageAttributeNames(names: List[String]): ReceiveMessageBuilder =
    copy(_.messageAttributeNames(names.asJava))

  def receiveRequestAttemptId(id: String): ReceiveMessageBuilder =
    copy(_.receiveRequestAttemptId(id))

  def visibilityTimeOut(timeout: FiniteDuration): ReceiveMessageBuilder =
    copy(_.visibilityTimeout(timeout.toSeconds.toInt))

  def waitTime(time: FiniteDuration): ReceiveMessageBuilder =
    copy(_.waitTimeSeconds(time.toSeconds.toInt))

  override def build(): DecodeStage[
    ReceiveMessageRequest,
    λ[A => List[ReceiveMessageResponse[A]]]] =
    new DecodeStage[
      ReceiveMessageRequest,
      λ[A => List[ReceiveMessageResponse[A]]]](builder.build()) {
      self =>

      override def decode[F[_], A](mediaType: MediaType, strict: Boolean)(
        implicit ED: EntityDecoder[F, A])
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
                        headers = Headers.of(`Content-Type`(mediaType)),
                        body = Stream(resp.body).through(text.utf8Encode)
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
