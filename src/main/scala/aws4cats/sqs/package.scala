package aws4cats

import org.http4s.Uri

package object sqs {

  implicit class RichUri(val repr: Uri.type) extends AnyVal {
    def unsafeFromParts(
        region: Region,
        accountNumber: Int,
        queueName: String
    ): Uri =
      Uri.unsafeFromString(
        s"https://sqs.${region.toString}.amazonaws.com/$accountNumber/$queueName")
  }
}
