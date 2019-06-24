import java.util.concurrent.Executors

import cats.effect.{IO, Resource}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext

package object test {

  implicit val cs =
    IO.contextShift(scala.concurrent.ExecutionContext.global)
  implicit val l =
    Slf4jLogger.create[IO].unsafeRunSync()
  implicit val t =
    IO.timer(scala.concurrent.ExecutionContext.global)

  val ecR: Resource[IO, ExecutionContext] =
    Resource(
      IO {
        val executor = Executors.newFixedThreadPool(100)
        val ec = ExecutionContext.fromExecutor(executor)
        (ec: ExecutionContext, IO(executor.shutdown()))
      }
    )

}
