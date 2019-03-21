package aws4cats.sqs

import software.amazon.awssdk.services.sqs.model.{
  QueueAttributeName => AwsQueueAttributeName
}

sealed trait QueueAttributeName extends Product with Serializable {
  val queueAttributeName: AwsQueueAttributeName
}
case object All extends QueueAttributeName {
  val queueAttributeName: AwsQueueAttributeName = AwsQueueAttributeName.ALL
}
case object ApproximateNumberOfMessages extends QueueAttributeName {

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES
}
case object ApproximateNumberOfMessageDelayed extends QueueAttributeName {

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED
}
case object ApproximateNumberOfMessagesNotVisible extends QueueAttributeName {

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE
}
case object ContentBasedDeduplication extends QueueAttributeName {

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.CONTENT_BASED_DEDUPLICATION
}
case object CreatedTimestamp extends QueueAttributeName {

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.CREATED_TIMESTAMP
}
case object DelaySeconds extends QueueAttributeName {

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.DELAY_SECONDS
}
case object FifoQueue extends QueueAttributeName {

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.FIFO_QUEUE
}
case object KmsDataKeyReusePeriodSeconds extends QueueAttributeName {

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.KMS_DATA_KEY_REUSE_PERIOD_SECONDS
}
case object KmsMasterKeyId extends QueueAttributeName {

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.KMS_MASTER_KEY_ID
}
case object LastModifiedTimestamp extends QueueAttributeName {

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.LAST_MODIFIED_TIMESTAMP
}
case object MaximumMessageSize extends QueueAttributeName {

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.MAXIMUM_MESSAGE_SIZE
}
case object MessageRetentionPeriod extends QueueAttributeName {

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.MESSAGE_RETENTION_PERIOD
}
case object Policy extends QueueAttributeName {
  val queueAttributeName: AwsQueueAttributeName = AwsQueueAttributeName.POLICY
}
case object QueueArn extends QueueAttributeName {

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.QUEUE_ARN
}
case object ReceiveMessageWaitTimeSeconds extends QueueAttributeName {

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS
}
case object RedrivePolicy extends QueueAttributeName {

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.REDRIVE_POLICY
}
case object VisibilityTimeout extends QueueAttributeName {

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.VISIBILITY_TIMEOUT
}
