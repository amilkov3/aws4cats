package aws4cats

import cats.Show
import cats.implicits._
import internal._

case class AccountId private (
  id: Long
) {
  def copy(id: Long): Either[String, AccountId] = AccountId.apply(id)
}

object AccountId {

  def unsafe(id: Long): AccountId = apply(id).rethrow

  def apply(id: Long): Either[String, AccountId] =
    if (id.toString.matches("[\\d]{12}")) (new AccountId(id)).asRight[String]
    else s"Account id: $id must be 12 digits long".asLeft[AccountId]

  implicit val show: Show[AccountId] = Show.show(_.id.toString)
}
