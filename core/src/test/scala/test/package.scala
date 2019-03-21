import java.util.concurrent.Executors

import cats.effect.{IO, Resource}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext

package object test {

  abstract class BaseTest extends FlatSpec with Matchers

  implicit val cs =
    IO.contextShift(scala.concurrent.ExecutionContext.global)
  implicit val l =
    Slf4jLogger.create[IO].unsafeRunSync()

  val ecR: Resource[IO, ExecutionContext] =
    Resource(
      IO {
        val executor = Executors.newFixedThreadPool(100)
        val ec = ExecutionContext.fromExecutor(executor)
        (ec: ExecutionContext, IO(executor.shutdown()))
      }
    )

}
