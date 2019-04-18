## aws4cats

`cats-effect`, `http4s-core`, `fs2` driver for various AWS tools. Wraps the
new 2.0 Java AWS SDK

### Supported tooling

* SQS
* S3 (wip)
* DynamoDB (wip)

### Installation

```scala
"ml.milkov" %% "aws4cats-sqs" % "0.3.0"
```

### Authenticating against AWS

The clients you instantiate in this library use the [`software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider`](https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/auth/credentials/DefaultCredentialsProvider.html)
logic chain by default to resolve AWS credentials 

See [here](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html){:target="_blank"}
for how to setup credentials

The way I would recommend is to add credentials for
a profile that has permissions to perform the actions 
you will be performing with whichever client you're using 
in this library. In `~/.aws/credentials` add:
```
[Some-Profile]
output = json
region = <default-region>
aws_access_key_id = <id>
aws_secret_access_key = <secret>
aws_session_token = <token>
```
and then point to this profile via `export AWS_PROFILE=Some-Profile`


### Development

#### Running tests

```
sbt:root> test
```

#### Building and previewing the microsite

```
sbt:docs> makeMicrosite
sbt:docs> previewSite
```
