package aws4cats.s3

import java.time.Instant

import aws4cats.internal._
import cats.implicits._
import org.http4s.DecodeFailure
import org.http4s.headers._
import software.amazon.awssdk.services.s3.model.{
  PutObjectResponse => POR,
  BucketLocationConstraint,
  Tag => T,
  ObjectLockLegalHoldStatus => OLLHS,
  ObjectLockMode => OLM,
  ReplicationStatus => RS,
  RequestCharged => RC,
  RequestPayer => RP,
  ServerSideEncryption => SSE,
  StorageClass => SC
}

sealed trait BucketCannedACL extends Product with Serializable
case object `private` extends BucketCannedACL with ObjectCannedACL
case object `public-read` extends BucketCannedACL with ObjectCannedACL
case object `public-read-write` extends BucketCannedACL with ObjectCannedACL
case object `authenticated-read` extends BucketCannedACL with ObjectCannedACL

sealed trait Grantee extends Product with Serializable { self =>

  def toHeader: String = self match {
    case Email(emailStr) => s"emailAddress=$emailStr"
    case AccountId(id)   => show"id=$id"
    case Uri(uri)        => s"uri=${uri.renderString}"
  }
}

case class Email private (toStr: String) extends Grantee
case class AccountId(id: aws4cats.AccountId) extends Grantee
case class Uri(uri: org.http4s.Uri) extends Grantee

case object Email {

  def unsafe(email: String): Email = apply(email).rethrow

  def apply(email: String): Either[String, Email] =
    if (email.matches(
          """(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])"""
        ))
      (new Email(email)).asRight[String]
    else
      s"Email: $email is invalid"
        .asLeft[Email]
}

sealed trait BucketRegion {

  val bucketLocationConstraint: BucketLocationConstraint

}
case object `ap-northeast-1` extends BucketRegion {
  override val bucketLocationConstraint: BucketLocationConstraint =
    BucketLocationConstraint.AP_NORTHEAST_1
}
case object `ap-south-1` extends BucketRegion {
  override val bucketLocationConstraint: BucketLocationConstraint =
    BucketLocationConstraint.AP_SOUTH_1
}
case object `ap-southeast-1` extends BucketRegion {
  override val bucketLocationConstraint: BucketLocationConstraint =
    BucketLocationConstraint.AP_SOUTH_1
}
case object `ap-southeast-2` extends BucketRegion {
  override val bucketLocationConstraint: BucketLocationConstraint =
    BucketLocationConstraint.AP_SOUTHEAST_2
}

case object `cn-north-1` extends BucketRegion {
  override val bucketLocationConstraint: BucketLocationConstraint =
    BucketLocationConstraint.AP_SOUTHEAST_2
}
case object EU extends BucketRegion {
  override val bucketLocationConstraint: BucketLocationConstraint =
    BucketLocationConstraint.AP_SOUTHEAST_2
}
case object `eu-central-1` extends BucketRegion {
  override val bucketLocationConstraint: BucketLocationConstraint =
    BucketLocationConstraint.AP_SOUTHEAST_2
}
case object `eu-west-1` extends BucketRegion {
  override val bucketLocationConstraint: BucketLocationConstraint =
    BucketLocationConstraint.AP_SOUTHEAST_2
}
case object `us-west-1` extends BucketRegion {
  override val bucketLocationConstraint: BucketLocationConstraint =
    BucketLocationConstraint.AP_SOUTHEAST_2
}
case object `us-west-2` extends BucketRegion {
  override val bucketLocationConstraint: BucketLocationConstraint =
    BucketLocationConstraint.AP_SOUTHEAST_2
}
case object `sa-east-1` extends BucketRegion {
  override val bucketLocationConstraint: BucketLocationConstraint =
    BucketLocationConstraint.AP_SOUTHEAST_2
}

case class GetObjectResponse[O](
  acceptRanges: String,
  body: Either[DecodeFailure, O],
  cacheControl: `Cache-Control`,
  contentDisposition: `Content-Disposition`,
  contentEncoding: `Content-Encoding`,
  contentLanguage: String,
  contentLength: `Content-Length`,
  contentRange: `Content-Range`,
  contentType: `Content-Type`,
  deleteMarker: Boolean,
  eTag: String,
  expiration: String,
  expires: Instant,
  lastModified: Instant,
  missingMeta: Int,
  objectLockLegalHoldStatus: ObjectLockLegalHoldStatus,
  objectLockMode: ObjectLockMode,
  objectLockRetainUntilDate: Instant,
  partsCount: Int,
  replicationStatus: ReplicationStatus,
  requestCharged: RequestCharged,
  restore: String,
  serverSideEncryption: ServerSideEncryption,
  sseCustomerAlgorithm: String,
  sseCustomerKeyMD5: String,
  ssekmsKeyId: String,
  storageClass: StorageClass,
  tagCount: Int,
  websiteRedirectLocation: String
)

case class PutObjectResponse(
  expiration: String,
  serverSideEncryption: ServerSideEncryption,
  sseCustomerKeyMD5: String,
  ssekmsKeyId: String,
  eTag: String,
  requestCharged: RequestCharged
)

sealed trait ServerSideEncryption extends Product with Serializable {
  val serverSideEncryption: SSE
}

object ServerSideEncryption {

  def fromEnum(x: SSE): ServerSideEncryption = x match {
    case SSE.AES256 | SSE.UNKNOWN_TO_SDK_VERSION => AES256
    case SSE.AWS_KMS                             => `aws::kms`
  }
}
case object AES256 extends ServerSideEncryption {
  override val serverSideEncryption: SSE = SSE.AES256
}
case object `aws::kms` extends ServerSideEncryption {
  override val serverSideEncryption: SSE = SSE.AWS_KMS
}

sealed trait ObjectLockLegalHoldStatus extends Product with Serializable {
  val objectLockLegalHoldStatus: OLLHS
}

object ObjectLockLegalHoldStatus {

  def fromEnum(x: OLLHS): ObjectLockLegalHoldStatus = x match {
    case OLLHS.OFF | OLLHS.UNKNOWN_TO_SDK_VERSION => OFF
    case OLLHS.ON                                 => ON
  }
}
case object ON extends ObjectLockLegalHoldStatus {
  override val objectLockLegalHoldStatus: OLLHS = OLLHS.ON
}
case object OFF extends ObjectLockLegalHoldStatus {
  override val objectLockLegalHoldStatus: OLLHS = OLLHS.OFF
}

sealed trait ObjectLockMode extends Product with Serializable {
  val objectLockMode: OLM
}

object ObjectLockMode {

  def fromEnum(olm: OLM): ObjectLockMode = olm match {
    case OLM.GOVERNANCE | OLM.UNKNOWN_TO_SDK_VERSION => GOVERNANCE
    case OLM.COMPLIANCE                              => COMPLIANCE
  }
}
case object GOVERNANCE extends ObjectLockMode {
  override val objectLockMode: OLM = OLM.GOVERNANCE
}
case object COMPLIANCE extends ObjectLockMode {
  override val objectLockMode: OLM = OLM.COMPLIANCE
}

sealed trait ReplicationStatus extends Product with Serializable

object ReplicationStatus {

  def fromEnum(rs: RS): ReplicationStatus = rs match {
    case RS.COMPLETE | RS.UNKNOWN_TO_SDK_VERSION => COMPLETE
    case RS.PENDING                              => PENDING
    case RS.FAILED                               => FAILED
    case RS.REPLICA                              => REPLICA
  }
}
case object COMPLETE extends ReplicationStatus
case object PENDING extends ReplicationStatus
case object FAILED extends ReplicationStatus
case object REPLICA extends ReplicationStatus

sealed trait RequestCharged extends Product with Serializable

object RequestCharged {

  def fromEnum(x: RC): RequestCharged = x match {
    case RC.REQUESTER | RC.UNKNOWN_TO_SDK_VERSION => requester
  }
}

sealed trait RequestPayer extends Product with Serializable {
  val requestPayer: RP
}
case object requester extends RequestCharged with RequestPayer {
  override val requestPayer: RP = RP.REQUESTER
}

sealed trait StorageClass extends Product with Serializable {
  val storageClass: SC
}

object StorageClass {

  def fromEnum(sc: SC): StorageClass = sc match {
    case SC.STANDARD | SC.UNKNOWN_TO_SDK_VERSION => STANDARD
    case SC.REDUCED_REDUNDANCY                   => REDUCED_REDUNDANCY
    case SC.STANDARD_IA                          => STANDARD_IA
    case SC.ONEZONE_IA                           => ONEZONE_IA
    case SC.INTELLIGENT_TIERING                  => INTELLIGENT_TIERING
    case SC.GLACIER                              => GLACIER
  }
}

case object STANDARD extends StorageClass {
  override val storageClass: SC = SC.STANDARD
}
case object REDUCED_REDUNDANCY extends StorageClass {
  override val storageClass: SC = SC.REDUCED_REDUNDANCY
}
case object STANDARD_IA extends StorageClass {
  override val storageClass: SC = SC.STANDARD_IA
}
case object ONEZONE_IA extends StorageClass {
  override val storageClass: SC = SC.ONEZONE_IA
}
case object INTELLIGENT_TIERING extends StorageClass {
  override val storageClass: SC = SC.INTELLIGENT_TIERING
}
case object GLACIER extends StorageClass {
  override val storageClass: SC = SC.GLACIER
}

sealed trait ObjectCannedACL extends Product with Serializable
case object `aws-exec-read` extends ObjectCannedACL
case object `bucket-owner-read` extends ObjectCannedACL
case object `bucket-owner-full-control` extends ObjectCannedACL

case class Tag private (toTag: T)

object Tag {

  def apply(key: String, value: String): Tag =
    new Tag(T.builder().key(key).value(value).build())
}
