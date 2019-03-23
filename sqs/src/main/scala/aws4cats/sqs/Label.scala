package aws4cats.sqs

import cats.implicits._
import aws4cats.internal._
import cats.Show

case class Label(
  value: String
)

object Label {

  def unsafe(label: String): Label = apply(label).rethrow

  def apply(label: String): Either[String, Label] =
    if (label.matches("[a-zA-Z0-9-_]{1,80}"))
      (new Label(label)).asRight[String]
    else
      s"Label: $label must be alphanumeric (- and _ are allowed as well) and no more than 80 chars"
        .asLeft[Label]

  implicit val show: Show[Label] = Show.show(_.value)
}
