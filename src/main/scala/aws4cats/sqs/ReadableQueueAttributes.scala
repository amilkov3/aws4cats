package aws4cats.sqs

import scala.collection.JavaConverters._

sealed trait ReadableQueueAttributes extends Product with Serializable
case object All extends ReadableQueueAttributes
case object ApproximateNumberOfMessages extends ReadableQueueAttributes
case object ApproximateNumberOfMessagesDelayed extends ReadableQueueAttributes
case object ApproximateNumberOfMessagesNotVisible
    extends ReadableQueueAttributes
case object CreatedTimestamp extends ReadableQueueAttributes
case object DelaySeconds extends ReadableQueueAttributes
case object LastModifiedTimestamp extends ReadableQueueAttributes
case object MaximumMessageSize extends ReadableQueueAttributes
case object MessageRetentionPeriod extends ReadableQueueAttributes
case object Policy extends ReadableQueueAttributes
case object QueueArn extends ReadableQueueAttributes
case object ReceiveMessageWaitTimeSeconds extends ReadableQueueAttributes
case object RedrivePolicy extends ReadableQueueAttributes
case object VisibilityTimeout extends ReadableQueueAttributes

object ReadableQueueAttributes {

  def asJavaList(
      attributes: List[ReadableQueueAttributes]): java.util.List[String] =
    attributes.map(_.toString).asJava
}

/*object WritableQueueAttributes {
  def asJavaMap(
      attributes: Map[WritableQueueAttributes, WritableQueueAttributes#R]
  )
}*/
