package aws4cats

import com.amazonaws.regions._

sealed trait Region
case object `us-east-1` extends Region
case object `us-east-2` extends Region
