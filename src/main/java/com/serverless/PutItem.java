package com.serverless;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PutItem implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

    String key = "ID";
    String topicArnNumber = "topicArnNumber";
    String client_email = "client_email";


    @Override
    public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
        try {

        Region region = Region.US_EAST_1;
        DynamoDbClient ddb = DynamoDbClient.builder()
                .region(region)
                .build();

        JsonNode body = new ObjectMapper().readTree((String) input.get("body"));
        String key_value = body.get("key_value").asText();
        String tableName = body.get("tableName").asText();
        String topicArnNumber_value = body.get("topicArnNumber_value").asText();
        String client_email_value = body.get("client_email_value").asText();

        putItemInTable(ddb, tableName, key, key_value, topicArnNumber, topicArnNumber_value,
                       client_email, client_email_value);
        System.out.println("PutItem is Done");
        ddb.close();
            return ApiGatewayResponse.builder()
                    .setStatusCode(200)
                    .setRawBody("PutItem is Done")
                    .setHeaders(Collections.singletonMap("X-Powered-By", "AWS Lambda & Serverless"))
                    .build();

        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println("Error in PutItem");
            return ApiGatewayResponse.builder()
                    .setStatusCode(513)
                    .setRawBody("Error in PutItem")
                    .setHeaders(Collections.singletonMap("X-Powered-By", "AWS Lambda & Serverless"))
                    .build();
        }
    }

    public static void putItemInTable(DynamoDbClient ddb,
                                      String tableName,
                                      String key,
                                      String key_value,
                                      String topicArnNumber,
                                      String topicArnNumber_value,
                                      String client_email,
                                      String client_email_value) {

        HashMap<String, AttributeValue> itemValues = new HashMap<>();
        itemValues.put(key, AttributeValue.builder().s(key_value).build());
        itemValues.put(topicArnNumber, AttributeValue.builder().s(topicArnNumber_value).build());
        itemValues.put(client_email, AttributeValue.builder().s(client_email_value).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(itemValues)
                .build();

        try {
            PutItemResponse response = ddb.putItem(request);
            System.out.println(tableName + " was successfully updated. The request id is " + response.responseMetadata().requestId());

        } catch (ResourceNotFoundException e) {
            System.err.format("Error: The Amazon DynamoDB table \"%s\" can't be found.\n", tableName);
            System.err.println("Be sure that it exists and that you've typed its name correctly!");

        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            System.err.println("It's DynamoDbException");
        }
    }







}
