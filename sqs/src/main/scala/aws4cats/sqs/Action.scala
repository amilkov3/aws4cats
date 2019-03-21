package aws4cats.sqs

sealed trait Action extends Product with Serializable
case object SendMessage extends Action
case object DeleteMessage extends Action
case object ChangeMessageVisibility extends Action
