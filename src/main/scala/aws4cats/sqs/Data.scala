package aws4cats.sqs

import aws4cats.Region
import org.http4s.Uri

case class ReceiptHandle(
  repr: String
)

case class Message[M](
    receiptHandle: String,
    body: M
)

case class QueueUri(
    uri: Uri
)

object QueueUri {

  def fromParts(
      region: Region,
      accountNumber: Int,
      queueName: String
  ): QueueUri = {
    QueueUri(
      Uri.unsafeFromString(
        s"https://sqs.${region.toString}.amazonaws.com/$accountNumber/$queueName")
    )
  }
}
