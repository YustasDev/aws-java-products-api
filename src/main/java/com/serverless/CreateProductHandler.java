package com.serverless;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.awssdk.services.sns.paginators.ListTopicsIterable;


import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CreateProductHandler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

    //private final Logger logger = Logger.getLogger(this.getClass());
    String pre_message = "An entry has been created in the database with the name: ";
    String xiomaTopicName = "notificationXioma";
    String email = "goosseff@yandex.ru";
    String topicArn = null;


    @Override
    public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {

        try {
            Region region = Region.US_EAST_1;

    // ======================================================================
            SnsClient snsClient = SnsClient.builder()
                    .region(region)
                    .build();

            System.out.println("SNS_Client = " + snsClient.toString());
            listSNSTopics(snsClient);

            // get the 'body' from input
            JsonNode body = new ObjectMapper().readTree((String) input.get("body"));

            // create the Product object for post
            Product product = new Product();
            // product.setId(body.get("id").asText());
            product.setName(body.get("name").asText());
            product.setPrice((float) body.get("price").asDouble());
            product.save(product);

            //send msg via SNS
            String message = pre_message + body.get("name").asText();


            topicArn = createSNSTopic(snsClient, xiomaTopicName);
            System.out.println("topicArn = " + topicArn);

//==============================================================================

            boolean subEmail = subEmail(snsClient, topicArn, email);
            if (subEmail) {
                pubTopic(snsClient, message, topicArn);
            }
            snsClient.close();
            

            // send the response back
            return ApiGatewayResponse.builder()
                    .setStatusCode(200)
                    .setObjectBody(product)
                    .setHeaders(Collections.singletonMap("X-Powered-By", "AWS Lambda & Serverless"))
                    .build();

        } catch (Exception ex) {
            //logger.error("Error in saving product: " + ex);
            // send the error response back
            Response responseBody = new Response("Error in saving product: ", input);
            return ApiGatewayResponse.builder()
                    .setStatusCode(517)
                    .setObjectBody(responseBody)
                    .setHeaders(Collections.singletonMap("X-Powered-By", "AWS Lambda & Serverless"))
                    .build();
        }
    }




    public static void listSNSTopics(SnsClient snsClient) {
        ListTopicsIterable listTopics = null;
        try {
            listTopics = snsClient.listTopicsPaginator();
            listTopics.stream()
                    .flatMap(r -> r.topics().stream())
                 //   .forEach(content -> System.out.println(" Topic ARN: " + content.topicArn()));
                    .forEach(content -> {System.out.println("=================================");
                                         System.out.println(" Topic: " + content.toString());
                                         System.out.println(" Topic ARN: " + content.topicArn());
                                         System.out.println("=================================");
                    });
        } catch (SnsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.err.println("listTopics = " + listTopics);
        }
    }




    public static String createSNSTopic(SnsClient snsClient, String topicName) {

        CreateTopicResponse result = null;
        CreateTopicRequest request = null;
        try {
            request = CreateTopicRequest.builder()
                    .name(topicName)
                    .build();

            if(request != null) {
                System.out.println("CreateTopicRequest = " + request.toString());
            }
            else{System.err.println("CreateTopicRequest request = null");
            }

            result = snsClient.createTopic(request);
            if (result != null) {
                System.out.println("result createSNSTopic = " + request.toString());
            }
            else {
                System.err.println("CreateTopicResponse result = null");
            }
            return result.topicArn();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println(e.getCause());
            System.err.println("From catch_block createSNSTopic method");
            if (request != null){
                System.err.println(request.toString());
            }
            System.err.println("request in createSNSTopic is null");
        }
        return "";
    }

    public static boolean subEmail(SnsClient snsClient, String topicArn, String email) {

        try {
            SubscribeRequest request = SubscribeRequest.builder()
                    .protocol("email")
                    .endpoint(email)
                    .returnSubscriptionArn(true)
                    .topicArn(topicArn)
                    .build();

            SubscribeResponse result = snsClient.subscribe(request);
            System.out.println("Subscription ARN: " + result.subscriptionArn() + "\n\n Status is " + result.sdkHttpResponse().statusCode());
            return true;

        } catch (SnsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            return false;
        }
    }

    private void pubTopic(SnsClient snsClient, String message, String topicArn) {
        try {
            PublishRequest request = PublishRequest.builder()
                    .message(message)
                    .topicArn(topicArn)
                    .build();

            PublishResponse result = snsClient.publish(request);
            System.out
                    .println(result.messageId()
                            + " Message: " + message + " sent. Status is " + result.sdkHttpResponse().statusCode());

        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println("From catch_block pubTopic method");
            //logger.error(e.getMessage());
            //logger.error("Error in pubTopic");
        }
    }



}