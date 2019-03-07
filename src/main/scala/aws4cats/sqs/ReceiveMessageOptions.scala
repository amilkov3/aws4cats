package aws4cats.sqs

import com.amazonaws.services.sqs.model.ReceiveMessageRequest

case class ReceiveMessageOptions(
    numMessage: Int = 10
) {

  private[sqs] def withReceiveMessageRequest(
      req: ReceiveMessageRequest): ReceiveMessageRequest = {
    val req1 = req.clone()
    req1.setMaxNumberOfMessages(numMessage)
    req1
  }
}
