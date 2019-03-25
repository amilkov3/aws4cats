---
layout: page
title:  "SQS"
section: "sqs"
position: 1
---

# SQS

Intended to mirror the [official API docs](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/APIReference/Welcome.html)
as closely as possible


### Installing

```
"ml.milkov" %% "aws4" % "0.2.0"
```

### External setup

#### Authenticating against AWS

You should use the AWS CLI to obtain temporary
credentials (will be written to `~/.aws/credentials`) for
a profile that has permissions to talk to the SQS queues
in the given region you'd like to interact with. Then you
can simply set:

```
export AWS_PROFILE=<said-profile>
export AWS_REGION=<queue-location-region>
```

The following client uses the `software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider`
which read the `AWS_PROFILE` variable to determine for which profile
to read in credentials

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
  AccountId.unsafe(150081971781L),
  queueName
)

```

### API Methods

Most methods are single stage. i.e.


```scala

import scala.concurrent.duration._

clientR.use(client =>
  client.createQueue(queueName, VisibilityTimeout ~> 0.seconds)
)
```

the SQS API only has one multistage method 
(need [http4s-circe](https://mvnrepository.com/artifact/org.http4s/http4s-circe) 
for this) :

```scala

import cats.Applicative
import io.circe.generic.semiauto._
import org.http4s.{EntityEncoder, MediaType}
import org.http4s.circe._

case class Foo(
  x: String,
  y: Int
)

object Foo {

  implicit val circeEncoder: Encoder[Foo] = deriveEncoder

  implicit def http4sEncoder[F[_]: Applicative]: EntityEncoder[F, Foo] =
      jsonEncoderOf[F, Foo]
}

clientR.use(client =>
  client
    .receiveMessage(queueUri)
    .waitTime(5.seconds)
    .maxNumberOfMessages(2)
    .build
    .decode[IO, Foo](MediaType.application.json, true)
    .send
)
```

comprised of a builder stage, where various request headers and params
can be set, a decode stage, where the content type and whether
the payload should be decoded strictly is specified, and the send
stage, where the request is constructed
