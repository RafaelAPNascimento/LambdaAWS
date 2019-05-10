package com.uol.estudo.requests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.document.DeleteItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.gson.Gson;
import com.uol.estudo.model.ActiveSession;

public class RequestHandler
{

    private static final String DYNAMODB_TABLE_NAME = System.getenv("TABLE_NAME");

    @SuppressWarnings("unchecked")
    public void createSession_(InputStream inputStream, OutputStream outputStream, Context context) throws IOException
    {

        context.getLogger().log(
                "================= void createSession(InputStream inputStream, OutputStream outputStream, Context context) throws IOException");

        JSONParser parser = new JSONParser();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        JSONObject responseJson = new JSONObject();

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
        DynamoDB dynamoDb = new DynamoDB(client);

        try
        {
            JSONObject event = (JSONObject) parser.parse(reader);

            Item dbItem = null;

            if (event.get("id") != null && event.get("ttl") != null)
            {

                ActiveSession session = new ActiveSession(event.toString());

                dbItem = new Item().withNumber("session", new Random().nextInt(500)).withString("ttl", session.getTtl())
                        .withString("id", session.getId());

                dynamoDb.getTable(DYNAMODB_TABLE_NAME).putItem(new PutItemSpec().withItem(dbItem));
            }

            if (dbItem != null)
            {

                JSONObject responseBody = new JSONObject();
                responseBody.put("session", dbItem.getNumber("session"));
                responseBody.put("ttl", dbItem.getString("ttl"));
                responseBody.put("id", dbItem.getString("id"));

                responseJson.put("body", responseBody.toJSONString());
            }
        }
        catch (ParseException e)
        {
            responseJson.put("statusCode", 400);
            responseJson.put("exception", e);
        }
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toJSONString());
        writer.close();
    }

    public void getSessions(InputStream inputStream, OutputStream outputStream, Context context) throws IOException
    {

        context.getLogger().log("============ void getSessions(Context context) throws IOException\n\n");

        JSONObject responseJson = new JSONObject();

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        DynamoDBMapper mapper = new DynamoDBMapper(client);

        List<ActiveSession> sessions = mapper.scan(ActiveSession.class, scanExpression);
        sessions.size();

        if (!sessions.isEmpty())
        {
            String jsonList = new Gson().toJson(sessions);
            responseJson.put("activeSessions", jsonList);
        }

        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toJSONString());
        writer.close();
    }

    public void takeDown(InputStream inputStream, OutputStream outputStream, Context context) throws IOException
    {
        context.getLogger().log("================= void takeDown()");               

        JSONParser parser = new JSONParser();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        JSONObject responseJson = new JSONObject();

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
        DynamoDB dynamoDB = new DynamoDB(client);

        Long session = null;
        String id = null;
        
        try
        {
            JSONObject event = (JSONObject) parser.parse(reader);
            
            session = Long.parseLong(event.get("session").toString());
            id = (String) event.get("id");
            
            context.getLogger().log("=============== key: "+session+"\n\n");
            
            if (session != null)
            {
                Table table = dynamoDB.getTable(DYNAMODB_TABLE_NAME);
                                     
                DeleteItemSpec del = new DeleteItemSpec()
                                            .withPrimaryKey("session",session, "id", id)
                                            .withReturnValues(ReturnValue.ALL_OLD);
                
                DeleteItemOutcome outcome = table.deleteItem(del);
                Map<String, AttributeValue> dbItem = outcome.getDeleteItemResult().getAttributes();
                
                context.getLogger().log("============ dbItem item: "+dbItem+"\n\n");
                
                if (dbItem != null) {    
                    
                    context.getLogger().log("============ deleted item: "+dbItem.toString()+"\n\n");
                                                
                    responseJson.put("body", "deleted success");
                }
            }            
        }
        catch (Exception e)
        {
            responseJson.put("Fail to Delete", session);
            responseJson.put("statusCode", 400);
            responseJson.put("exception", e);
        }
        
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toJSONString());
        writer.close();
    }
}
