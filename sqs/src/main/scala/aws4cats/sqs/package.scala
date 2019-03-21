package aws4cats

import org.http4s.Uri
import cats.implicits._

package object sqs {

  implicit class RichUri(val repr: Uri.type) extends AnyVal {

    def unsafeFromParts(
      region: Region,
      accountId: AccountId,
      queueName: QueueName
    ): Uri =
      Uri.unsafeFromString(
        show"https://sqs.${region.toString}.amazonaws.com/$accountId/$queueName")
  }
}
