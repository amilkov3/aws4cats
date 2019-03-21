package aws4cats.sqs

import org.http4s.DecodeFailure
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName

case class ReceiveMessageResponse[M](
  receiptHandle: ReceiptHandle,
  attributes: Map[MessageSystemAttributeName, String],
  body: Either[DecodeFailure, M]
)
