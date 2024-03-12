package com.serverless;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;

public class CreateNewTable implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

    String tableName = "sns_topicArn";
    String key = "ID";


    @Override
    public ApiGatewayResponse handleRequest(Map<String, Object> stringObjectMap, Context context) {
        try {
            System.err.println("handleRequest_1");
            Region region = Region.US_EAST_1;
            System.err.println("handleRequest_2");
            DynamoDbClient ddb = DynamoDbClient.builder()
                    .region(region)
                    .build();
            System.err.println("handleRequest_3");
            String result = createTable(ddb, tableName, key);
            System.err.println("handleRequest_4");
            System.out.println("New table is: " + result);
            ddb.close();

            return ApiGatewayResponse.builder()
                    .setStatusCode(200)
                    .setRawBody("New table is: " + result)
                    .setHeaders(Collections.singletonMap("X-Powered-By", "AWS Lambda & Serverless"))
                    .build();
        }
        catch (Exception e){
            System.err.println(e.getMessage());
            System.err.println(e.getCause());
            return ApiGatewayResponse.builder()
                    .setStatusCode(513)
                    .setRawBody("Error creating 'sns_topicArn' table")
                    .setHeaders(Collections.singletonMap("X-Powered-By", "AWS Lambda & Serverless"))
                    .build();
        }
    }

    public static String createTable(DynamoDbClient ddb, String tableName, String key) {
        DynamoDbWaiter dbWaiter = ddb.waiter();
        CreateTableRequest request = CreateTableRequest.builder()
                .attributeDefinitions(AttributeDefinition.builder()
                        .attributeName(key)
                        .attributeType(ScalarAttributeType.S)
                        .build())
                .keySchema(KeySchemaElement.builder()
                        .attributeName(key)
                        .keyType(KeyType.HASH)
                        .build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(10L)
                        .writeCapacityUnits(10L)
                        .build())
                .tableName(tableName)
                .build();
        String newTable;
        try {
            System.err.println("createTable_1");
            CreateTableResponse response = ddb.createTable(request);
            System.err.println("createTable_2");
            DescribeTableRequest tableRequest = DescribeTableRequest.builder()
                    .tableName(tableName)
                    .build();
            System.err.println("createTable_3");
            // Wait until the Amazon DynamoDB table is created.
            WaiterResponse<DescribeTableResponse> waiterResponse = dbWaiter.waitUntilTableExists(tableRequest);
            System.err.println("createTable_4");
            waiterResponse.matched().response().ifPresent(System.out::println);
            System.err.println("createTable_5");
            newTable = response.tableDescription().tableName();
            System.err.println("createTable_6");
            return newTable;

        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            return "Error from the 'createTable' method";
        }

    }



}