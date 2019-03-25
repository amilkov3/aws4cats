package aws4cats.sqs

import cats.implicits._
import aws4cats.internal._
import cats.Show

case class QueueName(
  value: String
) {
  def copy(value: String): Either[String, QueueName] = QueueName.apply(value)
}

object QueueName {

  def unsafe(name: String): QueueName = apply(name).rethrow

  def apply(name: String): Either[String, QueueName] =
    if (name.matches("[a-zA-Z0-9-_]{1,80}"))
      (new QueueName(name)).asRight[String]
    else
      s"Queue name: $name must be alphanumeric (- and _ are allowed as well) and no more than 80 chars"
        .asLeft[QueueName]

  implicit val show: Show[QueueName] = Show.show(_.value)
}
