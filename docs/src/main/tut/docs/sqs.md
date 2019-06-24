---
layout: page
title:  "SQS"
section: "sqs"
position: 1
---

# SQS

Intended to mirror the [official API docs](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/APIReference/Welcome.html){:target="_blank"}
as closely as possible


### Instantiating a client

```scala
import java.util.concurrent.Executors

import aws4cats._
import aws4cats.sqs._
import cats.effect.{IO, Resource}
import org.http4s.Uri

import scala.concurrent.ExecutionContext

val ecR: Resource[IO, ExecutionContext] =
  Resource(
    IO {
      val executor = Executors.newFixedThreadPool(100)
      val ec = ExecutionContext.fromExecutor(executor)
      (ec: ExecutionContext, IO(executor.shutdown()))
    }
  )

val clientR: Resource[IO, SQSClient[IO]] = AsyncSQSClientBuilder[IO](
  ecR
).resource

val queueName = QueueName.unsafe("testqueue")

val queueUri = Uri.unsafeFromParts(
  `us-east-1`,
  AccountId.unsafe("150081971781"),
  queueName
)

```

### Create a queue

```scala

import eu.timepit.refined._

clientR.use(client =>
  client.createQueue(
    queueName, 
    // compile time macro that asserts that the literal `0` obeys
    // the conditions of the refinement `VisibilityTimeout.Refine`
    VisibilityTimeout ~> refineMV[VisibilityTimeout.Refine](0),
    // unsafe bind `VisibilityTimeout` to an int
    // this calls `refineV` under the hood which returns a
    // `Either[String, Refined[V, P]]` and then throws an exception
    // if its a `Left`
    DelaySeconds ~!> 0
  )
)
```
### Send a message

```scala

...
import cats.Applicative
import io.circe.Encoder
import io.circe.generic.semiauto._
import org.http4s.circe._
import org.http4s.{EntityEncoder, MediaType}

case class Foo(
  x: String,
  y: Int
)

object Foo {

  implicit val circeEncoder: Encoder[Foo] = deriveEncoder

  implicit def http4sEncoder[F[_]: Applicative]: EntityEncoder[F, Foo] =
      jsonEncoderOf[F, Foo]

}

client.sendMessage(queueUri, Foo("hello", 5)): IO[SendMessageResponse]
```

### Receiving a message

the SQS API only has one multistage method 
(need [http4s-circe](https://mvnrepository.com/artifact/org.http4s/http4s-circe){:target="_blank"}
for this example) :

```scala

...
import cats.effect.Sync
import io.circe.Decoder
import org.http4s.EntityDecoder

object Foo {

  implicit val circeDecoder: Decoder[Foo] = deriveDecoder
  
  implicit def http4sDecoder[F[_]: Sync]: EntityDecoder[F, Foo] =
      jsonOf[F, Foo]
  
}

clientR.use(client =>
  (client
    .receiveMessage(queueUri)
    .waitTime(5.seconds)
    .maxNumberOfMessages(2)
    .build
    .decode[IO, Foo](MediaType.application.json, true)
    .send) : IO[List[ReceiveMessageResponse[Foo]]]
)
```

comprised of a builder stage, where various request headers and params
can be set, a decode stage, where the content type and whether
the payload should be decoded strictly is specified, and the send
stage, where the request is finally constructed
