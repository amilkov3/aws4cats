package aws4cats

import org.http4s.Uri
import cats.syntax.either._
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined._
import eu.timepit.refined.api.Refined

package object sqs {

  implicit class RichUri(val repr: Uri.type) extends AnyVal {

    def unsafeFromParts(
      region: Region,
      accountId: AccountId,
      queueName: QueueName
    ): Uri =
      Uri.unsafeFromString(
        s"https://sqs.${region.toString}.amazonaws.com/${accountId.value}/${queueName.value}"
      )
  }

  private[sqs] type QueueAndLabelRefine =
    MatchesRegex[W.`"[a-zA-Z0-9-_]{1,80}"`.T]

  import aws4cats.internal._

  type QueueName = String Refined QueueAndLabelRefine

  object QueueName {

    def unsafe(name: String): QueueName =
      apply(name).rethrow

    def apply(name: String): Either[String, QueueName] =
      refineV[QueueAndLabelRefine](name).leftMap(
        _ =>
          s"Queue name: $name must be alphanumeric (- and _ are allowed as well) and no more than 80 chars"
      )

  }

  type Label = String Refined QueueAndLabelRefine

  object Label {

    def unsafe(label: String): Label =
      apply(label).rethrow

    def apply(label: String): Either[String, Label] =
      refineV[QueueAndLabelRefine](label).leftMap(
        _ =>
          s"Label: $label must be alphanumeric (- and _ are allowed as well) and no more than 80 chars"
      )
  }

}
