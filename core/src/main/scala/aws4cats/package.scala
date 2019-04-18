import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined._
import cats.implicits._
import aws4cats.internal._

package object aws4cats {

  type AccountId = String Refined AccountId.Spec

  object AccountId {

    private[aws4cats] type Spec = MatchesRegex[W.`"[a-zA-Z0-9-_]{1,80}"`.T]

    def unsafe(id: String): AccountId =
      apply(id).rethrow

    def apply(id: String): Either[String, AccountId] =
      refineV[Spec](id).leftMap(
        _ => s"Account id: $id must be 12 digits long"
      )

  }

}
