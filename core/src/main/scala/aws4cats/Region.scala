package aws4cats

import software.amazon.awssdk.regions.{Region => AwsRegion}

sealed trait Region extends Product with Serializable {
  val region: AwsRegion
}
case object `ap-northeast-2` extends Region {
  override val region: AwsRegion = AwsRegion.AP_NORTHEAST_2
}
case object `ap-northeast-1` extends Region {
  override val region: AwsRegion = AwsRegion.AP_NORTHEAST_1
}
case object `ap-south-1` extends Region {
  override val region: AwsRegion = AwsRegion.AP_SOUTH_1
}
case object `ap-southeast-1` extends Region {
  override val region: AwsRegion = AwsRegion.AP_SOUTHEAST_1
}
case object `ap-southeast-2` extends Region {
  override val region: AwsRegion = AwsRegion.AP_SOUTHEAST_2
}
case object `aws-cn-global` extends Region {
  override val region: AwsRegion = AwsRegion.AWS_CN_GLOBAL
}
case object `aws-global` extends Region {
  override val region: AwsRegion = AwsRegion.AWS_GLOBAL
}
case object `aws-us-gov-global` extends Region {
  override val region: AwsRegion = AwsRegion.AWS_US_GOV_GLOBAL
}
case object `ca-central-1` extends Region {
  override val region: AwsRegion = AwsRegion.CA_CENTRAL_1
}
case object `cn-north-1` extends Region {
  override val region: AwsRegion = AwsRegion.CN_NORTH_1
}
case object `cn-northwest-1` extends Region {
  override val region: AwsRegion = AwsRegion.CN_NORTHWEST_1
}
case object `eu-central-1` extends Region {
  override val region: AwsRegion = AwsRegion.EU_CENTRAL_1
}
case object `eu-north-1` extends Region {
  override val region: AwsRegion = AwsRegion.EU_NORTH_1
}
case object `eu-west-1` extends Region {
  override val region: AwsRegion = AwsRegion.EU_WEST_1
}
case object `eu-west-2` extends Region {
  override val region: AwsRegion = AwsRegion.EU_WEST_2
}
case object `eu-west-3` extends Region {
  override val region: AwsRegion = AwsRegion.EU_WEST_3
}
case object `sa-east-1` extends Region {
  override val region: AwsRegion = AwsRegion.SA_EAST_1
}
case object `us-east-1` extends Region {
  override val region: AwsRegion = AwsRegion.US_EAST_1
}
case object `us-east-2` extends Region {
  override val region: AwsRegion = AwsRegion.US_EAST_2
}
case object `us-gov-east-1` extends Region {
  override val region: AwsRegion = AwsRegion.US_GOV_EAST_1
}
case object `us-gov-west-1` extends Region {
  override val region: AwsRegion = AwsRegion.US_GOV_WEST_1
}
case object `us-west-1` extends Region {
  override val region: AwsRegion = AwsRegion.US_WEST_1
}
case object `us-west-2` extends Region {
  override val region: AwsRegion = AwsRegion.US_WEST_2
}
