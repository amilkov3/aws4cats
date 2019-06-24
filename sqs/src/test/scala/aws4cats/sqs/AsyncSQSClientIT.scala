package aws4cats.sqs

import cats.effect.{IO, Sync}
import cats.implicits._
import org.http4s.{EntityDecoder, EntityEncoder, MediaType, Uri}
import org.http4s.circe._
import aws4cats._
import eu.timepit.refined._
import eu.timepit.refined.numeric._
import eu.timepit.refined.auto._
import cats.Applicative
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import org.specs2.Specification
import org.specs2.specification.BeforeAfterAll
import test._

import concurrent.duration._

class AsyncSQSClientIT extends Specification with BeforeAfterAll {

  def is =
    s2"""
         This is a specification to test the `AsyncSQSClient`

         The API should:
         work                                       $testAPI
    """

  val clientR = AsyncSQSClientBuilder[IO](
    ecR
  ).resource

  val queueName = QueueName.unsafe("testqueue")

  val queueUri = Uri.unsafeFromParts(
    `us-east-1`,
    AccountId.unsafe("150081971781"),
    queueName
  )
  case class Foo(
    x: String,
    y: Int
  )

  object Foo {

    implicit val enc: Encoder[Foo] = deriveEncoder

    implicit val dec: Decoder[Foo] = deriveDecoder

    implicit def encoder[F[_]: Applicative]: EntityEncoder[F, Foo] =
      jsonEncoderOf[F, Foo]

    implicit def decoder[F[_]: Sync]: EntityDecoder[F, Foo] =
      jsonOf[F, Foo]

  }

  val foo = Foo("hello", 5)

  override def beforeAll(): Unit =
    clientR
      .use(
        _.createQueue(
          queueName,
          VisibilityTimeout ~> refineMV[VisibilityTimeout.Refine](0),
        )
      )
      .unsafeRunSync()

  def numMessages[F[_]](client: SQSClient[F])(implicit S: Sync[F]): F[Int] =
    client
      .getQueueAttributes(queueUri, List(ApproximateNumberOfMessages))
      .flatMap { attribs =>
        S.fromEither(
          attribs
            .collectFirst {
              case (k, v) if (k == ApproximateNumberOfMessages) => v.toInt
            }
            .toRight(new Exception("No attribute ApproximateNumberOfMessages"))
        )
      }

  def testAPI =
    clientR
      .use { client =>
        client.setQueueAttributes(queueUri, DelaySeconds ~!> 0) *>
          client
            .getQueueAttributes(queueUri, List(DelaySeconds, VisibilityTimeout))
            .map(
              _.collectFirst { case (k, v) if (k == DelaySeconds) => v } mustEqual Some(
                "0")
            ) *>
          client
            .sendMessage(queueUri, foo) *>
          client
            .receiveMessage(queueUri)
            .waitTime(5.seconds)
            .maxNumberOfMessages(2)
            .build
            .decode[IO, Foo](MediaType.application.json, true)
            .send
            .map(res => {
              val message = res.head
              message.body mustEqual Right(foo)
              message.receiptHandle
            })
            .flatMap(receiptHandle =>
              client.deleteMessage(queueUri, receiptHandle)) *>
          numMessages(client).map(_ mustEqual 0) *>
          1.to(5).toList.traverse(_ => client.sendMessage(queueUri, foo)) *>
          client.purgeQueue(queueUri) *>
          numMessages(client).map(_ mustEqual 0)
      }
      .unsafeRunSync()

  override def afterAll(): Unit =
    clientR.use(client => client.deleteQueue(queueUri)).unsafeRunSync()

}
