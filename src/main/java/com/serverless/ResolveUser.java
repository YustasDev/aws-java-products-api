package com.serverless;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.marketplaceentitlement.AWSMarketplaceEntitlement;
import com.amazonaws.services.marketplaceentitlement.AWSMarketplaceEntitlementClient;
import com.amazonaws.services.marketplaceentitlement.AWSMarketplaceEntitlementClientBuilder;
import com.amazonaws.services.marketplaceentitlement.model.GetEntitlementsRequest;
import com.amazonaws.services.marketplaceentitlement.model.GetEntitlementsResult;
import com.amazonaws.services.marketplacemetering.AWSMarketplaceMeteringClient;
import com.amazonaws.services.marketplacemetering.model.ResolveCustomerRequest;
import com.amazonaws.services.marketplacemetering.model.ResolveCustomerResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.greengrass.model.Logger;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ResolveUser implements RequestHandler<APIGatewayV2HTTPEvent, ApiGatewayResponse> {

    private AWSMarketplaceMeteringClient marketplaceClient;

    //Logger log = Logger.builder().build();

    @Override
    public ApiGatewayResponse handleRequest(APIGatewayV2HTTPEvent gatewayV2HTTPEvent, Context context) {

        LambdaLogger log = context.getLogger();

        String token = "";
        String tableName = "UserStorage";

        String customerIdentifier = "customerIdentifier";
        String productCode = "productCode";
        String customerAWSAccountId = "customerAWSAccountId";

        String customerIdentifier_value = "";
        String productCode_value = "";
        String customerAWSAccountId_value = "";


        try {

            token = gatewayV2HTTPEvent.getQueryStringParameters().get("x-amzn-marketplace-token");
            //token = body.get("x-amzn-marketplace-token").asText("default");
            //token = request.getRegistrationToken();
            log.log("token is: " + token);

            if(token != null){
                marketplaceClient = new AWSMarketplaceMeteringClient();
                ResolveCustomerRequest resolveCustomerRequest = new ResolveCustomerRequest().withRegistrationToken(token);
                ResolveCustomerResult resultData = marketplaceClient.resolveCustomer(resolveCustomerRequest);
                customerIdentifier_value = resultData.getCustomerIdentifier();
                productCode_value = resultData.getProductCode();
                customerAWSAccountId_value = resultData.getCustomerAWSAccountId();
                log.log("The token was received: " + token);
                log.log("According to the token, data has been received ==> \n" +
                        "customerIdentifier_value = " + customerIdentifier_value +
                        "customerAWSAccountId_value = " + customerAWSAccountId_value +
                        "productCode_value = " + productCode_value);
            }

            // saving customer data in DynamoDB
            Region region = Region.US_EAST_1;
            DynamoDbClient ddb = DynamoDbClient.builder()
                    .region(region)
                    .build();

            putItemInTable(ddb, tableName,  customerIdentifier, productCode, customerAWSAccountId,
                           customerIdentifier_value, productCode_value, customerAWSAccountId_value);
            System.out.println("A client with customerID = "
                               + customerIdentifier_value + " has been recorded in DynamoDB table: " + tableName);
            ddb.close();


            // https://docs.aws.amazon.com/marketplace/latest/userguide/saas-integrate-contract.html
            //
            GetEntitlementsRequest entitlementsRequest = new GetEntitlementsRequest();
            entitlementsRequest.setProductCode(customerIdentifier_value);
            AWSMarketplaceEntitlement clientBuilder = AWSMarketplaceEntitlementClientBuilder.standard().build();
            GetEntitlementsResult entitlementsResult = clientBuilder.getEntitlements(entitlementsRequest);
            // =========================================================================================<














        }
        catch (Exception ex) {
            return ApiGatewayResponse.builder()
                    .setStatusCode(513)
                    .setObjectBody(token)
                    .setHeaders(Collections.singletonMap("X-Powered-By", "AWS Lambda & Serverless"))
                    .build();
        }

        return ApiGatewayResponse.builder()
                .setStatusCode(200)
                .setObjectBody(token)
                .setHeaders(Collections.singletonMap("X-Powered-By", "AWS Lambda & Serverless"))
                .build();
    }

    private void putItemInTable(DynamoDbClient ddb, String tableName, String customerIdentifier,
                                String productCode, String customerAWSAccountId,
                                String customerIdentifier_value, String productCode_value,
                                String customerAWSAccountId_value) {

        String timeStamp = new SimpleDateFormat("MM/dd/yyyy_HH:mm:ss").format(Calendar.getInstance().getTime());
        Map<String, AttributeValue> itemValues = new HashMap<>();
        itemValues.put(customerIdentifier, AttributeValue.builder().s(customerIdentifier_value).build());
        itemValues.put(productCode, AttributeValue.builder().s(productCode_value).build());
        itemValues.put(customerAWSAccountId, AttributeValue.builder().s(customerAWSAccountId_value).build());
        itemValues.put("timeStamp", AttributeValue.builder().s(timeStamp).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(itemValues)
                .build();

        try {
            PutItemResponse response = ddb.putItem(request);
            System.out.println(tableName + " was successfully updated. The request id is "
                    + response.responseMetadata().requestId());

        } catch (ResourceNotFoundException e) {
            System.err.format("Error: The Amazon DynamoDB table \"%s\" can't be found.\n", tableName);
            System.err.println("Be sure that it exists and that you've typed its name correctly!");
            //System.exit(1);
        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            //System.exit(1);
        }
    }
  }
