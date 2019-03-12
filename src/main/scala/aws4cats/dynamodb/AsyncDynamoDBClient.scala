package aws4cats.dynamodb

import aws4cats.ExecutorServiceWrapper
import aws4cats.internal._
import cats.effect.{Async, Resource}
import io.chrisdavenport.log4cats.Logger
import software.amazon.awssdk.auth.credentials.{
  AwsCredentialsProvider,
  DefaultCredentialsProvider
}
import software.amazon.awssdk.core.client.config.{
  ClientAsyncConfiguration,
  SdkAdvancedAsyncClientOption
}
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

sealed abstract class AsyncDynamoDBClient[F[_]](client: DynamoDbAsyncClient)(
    implicit A: Async[F],
    L: Logger[F]
) {

  def createTable(
      req: CreateTableRequest
  ): F[TableDescription] =
    A.async { cb =>
      client.createTable(req).handleResult(cb, _.tableDescription())
    }

  def deleteItem(
      req: DeleteItemRequest
  ): F[DeleteItemResponse] =
    A.async { cb =>
      client.deleteItem(req).handleResult(cb, identity)
    }

  def deleteTable(tableName: String): F[TableDescription] =
    A.async { cb =>
      client
        .deleteTable(DeleteTableRequest.builder().tableName(tableName).build)
        .handleResult(cb, _.tableDescription())
    }

  def getItem(
      req: GetItemRequest
  ): F[Map[String, AttributeValue]] =
    A.async { cb =>
      client.getItem(req).handleResult(cb, _.item.asScala.toMap)
    }

  def putItem(
      req: PutItemRequest
  ): F[PutItemResponse] =
    A.async { cb =>
      client.putItem(req).handleResult(cb, identity)
    }

  def updateItem(
      req: UpdateItemRequest
  ): F[UpdateItemResponse] =
    A.async { cb =>
      client.updateItem(req).handleResult(cb, identity)
    }

}

object AsyncDynamoDBClient {

  def apply[F[_]: Async: Logger](
      ecR: Resource[F, ExecutionContext],
      credentialsProvider: AwsCredentialsProvider =
        DefaultCredentialsProvider.create()
  ): Resource[F, AsyncDynamoDBClient[F]] =
    ecR.map(
      ec =>
        new AsyncDynamoDBClient(
          DynamoDbAsyncClient
            .builder()
            .credentialsProvider(credentialsProvider)
            .asyncConfiguration(
              ClientAsyncConfiguration
                .builder()
                .advancedOption(
                  SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR,
                  new ExecutorServiceWrapper(ec)
                )
                .build()
            )
            .build()) {})

}
