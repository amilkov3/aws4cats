package aws4cats.sqs

import software.amazon.awssdk.services.sqs.model.{
  QueueAttributeName => AwsQueueAttributeName
}
import eu.timepit.refined._
import eu.timepit.refined.api.{Inference, Refined, Validate}
import eu.timepit.refined.numeric._
import eu.timepit.refined.auto._
import shapeless.nat._

import scala.concurrent.duration._

sealed trait QueueAttributeName extends Product with Serializable {
  val queueAttributeName: AwsQueueAttributeName
}

final class Pair private[sqs] (val repr: (WritableQueueAttributeName, String))
  extends AnyVal
sealed trait WritableQueueAttributeName extends QueueAttributeName { self =>

  /** Associate a [[WritableQueueAttributeName]] with a valid value */
  def ~>[V](v: V)(implicit assoc: Assoc[self.type, V], show: ShowV[V]): Pair =
    new Pair(self -> show.showV(v))

  /** Unsafe associate a [[WritableQueueAttributeName]] with an unvalidated value */
  def ~!>[V, P](v: V)(
    implicit assoc: Assoc[self.type, V Refined P],
    show: ShowV[V Refined P],
    validate: Validate[V, P]): Pair =
    new Pair(
      self -> show.showV(
        refineV[P](v).fold(err => throw new Exception(err), identity)))

}

object QueueAttributeName {

  import AwsQueueAttributeName._

  def fromEnum(name: AwsQueueAttributeName): QueueAttributeName =
    // TODO: handle UNKNOWN_TO_SDK_VERSION
    (name: @unchecked) match {
      case ALL => All
      case APPROXIMATE_NUMBER_OF_MESSAGES =>
        ApproximateNumberOfMessages
      case APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED =>
        ApproximateNumberOfMessageDelayed
      case APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE =>
        ApproximateNumberOfMessagesNotVisible
      case CONTENT_BASED_DEDUPLICATION       => ContentBasedDeduplication
      case CREATED_TIMESTAMP                 => CreatedTimestamp
      case DELAY_SECONDS                     => DelaySeconds
      case FIFO_QUEUE                        => FifoQueue
      case KMS_DATA_KEY_REUSE_PERIOD_SECONDS => KmsDataKeyReusePeriodSeconds
      case KMS_MASTER_KEY_ID                 => KmsMasterKeyId
      case LAST_MODIFIED_TIMESTAMP           => LastModifiedTimestamp
      case MAXIMUM_MESSAGE_SIZE              => MaximumMessageSize
      case MESSAGE_RETENTION_PERIOD          => MessageRetentionPeriod
      case POLICY                            => Policy
      case QUEUE_ARN                         => QueueArn
      case RECEIVE_MESSAGE_WAIT_TIME_SECONDS => ReceiveMessageWaitTimeSeconds
      case REDRIVE_POLICY                    => RedrivePolicy
      case VISIBILITY_TIMEOUT                => VisibilityTimeout
    }
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
case object ContentBasedDeduplication extends WritableQueueAttributeName {

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.CONTENT_BASED_DEDUPLICATION
}
case object CreatedTimestamp extends QueueAttributeName {

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.CREATED_TIMESTAMP
}
case object DelaySeconds extends WritableQueueAttributeName {

  type Refine = Interval.Closed[W.`0`.T, W.`900`.T]

  type Repr = Int Refined Refine

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.DELAY_SECONDS
}
case object FifoQueue extends QueueAttributeName {

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.FIFO_QUEUE
}
case object KmsDataKeyReusePeriodSeconds extends WritableQueueAttributeName {

  type Refine = Interval.Closed[W.`60`.T, W.`86400`.T]

  type Repr = Int Refined Refine

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.KMS_DATA_KEY_REUSE_PERIOD_SECONDS
}
case object KmsMasterKeyId extends WritableQueueAttributeName {

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.KMS_MASTER_KEY_ID
}
case object LastModifiedTimestamp extends QueueAttributeName {

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.LAST_MODIFIED_TIMESTAMP
}
case object MaximumMessageSize extends WritableQueueAttributeName {

  type Refine = Interval.Closed[W.`1024`.T, W.`262144`.T]

  type Repr = Int Refined Refine

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.MAXIMUM_MESSAGE_SIZE
}
case object MessageRetentionPeriod extends WritableQueueAttributeName {

  type Refine = Interval.Closed[W.`60`.T, W.`1209600`.T]

  type Repr = Int Refined Refine

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.MESSAGE_RETENTION_PERIOD
}
case object Policy extends WritableQueueAttributeName {
  val queueAttributeName: AwsQueueAttributeName = AwsQueueAttributeName.POLICY
}
case object QueueArn extends QueueAttributeName {

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.QUEUE_ARN
}
case object ReceiveMessageWaitTimeSeconds extends WritableQueueAttributeName {

  type Refine = Interval.Closed[W.`0`.T, W.`20`.T]

  type Repr = Int Refined Refine

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS
}
case object RedrivePolicy extends WritableQueueAttributeName {

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.REDRIVE_POLICY
}
case object VisibilityTimeout extends WritableQueueAttributeName {

  type Refine = Interval.Closed[W.`0`.T, W.`43200`.T]

  type Repr = Int Refined Refine

  val queueAttributeName: AwsQueueAttributeName =
    AwsQueueAttributeName.VISIBILITY_TIMEOUT
}

sealed class Assoc[K <: WritableQueueAttributeName, V: ShowV]

sealed trait ShowV[V] {
  def showV(v: V): String
}

object ShowV {

  private def instance[V](enc: V => String): ShowV[V] =
    new ShowV[V] {
      override def showV(v: V): String = enc(v)
    }

  implicit def refined[T: ShowV, P]: ShowV[T Refined P] =
    instance(_.value.toString)
  implicit val string: ShowV[String] = instance(identity)
  implicit val int: ShowV[Int] = instance(_.toString)
  implicit val bool: ShowV[Boolean] = instance(_.toString)

}

object Assoc {

  implicit val contentBasedDeduplication
    : Assoc[ContentBasedDeduplication.type, Boolean] = new Assoc
  implicit val delaySeconds: Assoc[DelaySeconds.type, DelaySeconds.Repr] =
    new Assoc
  // TODO: define type
  implicit val kmsMasterKeyId: Assoc[KmsMasterKeyId.type, String] = new Assoc
  implicit val kmsReuse: Assoc[
    KmsDataKeyReusePeriodSeconds.type,
    KmsDataKeyReusePeriodSeconds.Repr] = new Assoc
  implicit val maximumMessageSize
    : Assoc[MaximumMessageSize.type, MaximumMessageSize.Repr] =
    new Assoc
  implicit val messageRetentionPeriod
    : Assoc[MessageRetentionPeriod.type, MessageRetentionPeriod.Repr] =
    new Assoc
  // TODO: define type
  implicit val policy: Assoc[Policy.type, String] = new Assoc
  implicit val receiveMessageWaitTimeSeconds: Assoc[
    ReceiveMessageWaitTimeSeconds.type,
    ReceiveMessageWaitTimeSeconds.Repr] =
    new Assoc
  // TODO: define type
  implicit val redrivePolicy: Assoc[RedrivePolicy.type, String] = new Assoc
  implicit val visibilityTimeout
    : Assoc[VisibilityTimeout.type, VisibilityTimeout.Repr] =
    new Assoc

}
