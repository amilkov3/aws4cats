package aws4cats

sealed trait Region
case object `us-east-1` extends Region
case object `us-east-2` extends Region
