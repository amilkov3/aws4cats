package aws4cats

import java.util.concurrent.CompletableFuture

package object internal {

  implicit class RichCompletableFuture[A](val repr: CompletableFuture[A])
    extends AnyVal {

    def handleVoidResult(
      cb: Either[Throwable, Unit] => Unit): CompletableFuture[Unit] =
      repr.handle[Unit]((_, err) =>
        Option(err).fold(cb(Right(())))(err => cb(Left(err))))

    def handleResult[R](
      cb: Either[Throwable, R] => Unit,
      f: A => R): CompletableFuture[Unit] =
      repr.handle[Unit]((res, err) =>
        Option(res).fold(cb(Left(err)))(res => cb(Right(f(res)))))

    def handleResultE[R](
      cb: Either[Throwable, R] => Unit,
      f: A => Either[Throwable, R]): CompletableFuture[Unit] =
      repr.handle[Unit](
        (res, err) =>
          Option(res).fold(cb(Left(err)))(res =>
            f(res).fold(err => cb(Left(err)), res => cb(Right(res))))
      )
  }

  implicit class RichEither[R](val repr: Either[String, R]) extends AnyVal {
    def rethrow: R = repr.fold(err => throw new Exception(err), identity)
  }

}
