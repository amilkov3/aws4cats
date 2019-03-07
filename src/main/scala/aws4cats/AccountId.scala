package aws4cats

case class AccountId(
  id: Long
)

object AccountId {

  /*def unsafeFrom(id: Long) =
    if (id > 999999999999L) Left()
    else Right(AccountId(id))*/


}
