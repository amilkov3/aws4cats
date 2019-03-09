package aws4cats.sqs


case class ReceiveMessageOptions(
    numMessage: Int = 10
)

