package aws4cats.sqs

case class SendMessageResponse(
  md5OfMessageAttributes: String,
  md5OfMessageBody: String,
  messageId: String,
  // only for FIFO queues
  sequenceNumber: Option[String]
)
