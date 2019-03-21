package aws4cats.sqs

import cats.effect.IO
import cats.effect.implicits._
import test._

class AsyncSQSClientIT extends BaseTest {

  val clientR = AsyncSQSClientBuilder[IO](
    ecR
  ).resource

  "API" should "work" in {

    clientR.use { client =>
      client.createQueue(
        QueueName.unsafe("testqueue"),
        Map()
      )

    }

  }
}
