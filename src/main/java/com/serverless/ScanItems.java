package com.serverless;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.*;

public class ScanItems implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {


    @Override
    public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
        try {
            Region region = Region.US_EAST_1;
            DynamoDbClient ddb = DynamoDbClient.builder()
                    .region(region)
                    .build();

            JsonNode body = new ObjectMapper().readTree((String) input.get("body"));

            String tableName = body.get("tableName").asText();
            String search_key = body.get("search_key").asText();
            String search_key_value = body.get("search_key_value").asText();

            Optional<Boolean> result = scanItems(ddb, tableName, search_key, search_key_value);
            System.err.println(result.get());
            ddb.close();
            if (result.isPresent() && result.get()) {
                return ApiGatewayResponse.builder()
                        .setStatusCode(200)
                        .setRawBody("The value: " + search_key_value + " in table: " + tableName + " was found")
                        .setHeaders(Collections.singletonMap("X-Powered-By", "AWS Lambda & Serverless"))
                        .build();
            } else {
                return ApiGatewayResponse.builder()
                        .setStatusCode(200)
                        .setRawBody("The value: " + search_key_value + " in table: " + tableName + " wasn't found")
                        .setHeaders(Collections.singletonMap("X-Powered-By", "AWS Lambda & Serverless"))
                        .build();
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
            return ApiGatewayResponse.builder()
                    .setStatusCode(517)
                    .setRawBody("Error in ListItem")
                    .setHeaders(Collections.singletonMap("X-Powered-By", "AWS Lambda & Serverless"))
                    .build();
        }
    }

    public static Optional<Boolean> scanItems(DynamoDbClient ddb, String tableName, String search_key, String search_key_value) {
        try {
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(tableName)
                    .build();

            System.err.println(tableName);
            System.err.println(search_key);
            System.err.println(search_key_value);

            List<String> searchList = new ArrayList<>();

            ScanResponse response = ddb.scan(scanRequest);
            for (Map<String, AttributeValue> item : response.items()) {
                System.err.println("Item = " + item.toString());
                Set<String> mapKeys = item.keySet();
                for (String key : mapKeys) {
                    System.err.println("key = " + key);
                    System.err.println("value = " + item.get(key).s());
                    if(key.equals(search_key))
                    searchList.add(item.get(key).s());
                }
            }

            System.err.println("Print searchList: ");
            System.err.println(searchList);
            if(searchList.contains(search_key_value)){
                return Optional.of(true);
            }
            else {
                return Optional.of(false);
            }

//                Set<String> keys = item.keySet();
//                for (String key : keys) {
//                    System.out.println("The key name is: " + key + "\n");
//                    System.out.println("The value is: " + item.get(key).s());


        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error in scanItems method");
        }
        return null;
    }
}

