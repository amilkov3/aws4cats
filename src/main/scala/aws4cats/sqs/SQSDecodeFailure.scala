package aws4cats.sqs

import org.http4s.DecodeFailure

case class SQSDecodeFailure(decodeErr: DecodeFailure, receiptHandle: String)
