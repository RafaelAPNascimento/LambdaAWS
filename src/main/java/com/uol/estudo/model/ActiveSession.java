package com.uol.estudo.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@DynamoDBTable(tableName = "ActiveSession")
public class ActiveSession {
    
	private Integer session;
	private String ttl;
	private String id;
	
	public ActiveSession()
    {
        super();
        // TODO Auto-generated constructor stub
    }

    public ActiveSession(String json) {
		
		Gson gson = new Gson();
		ActiveSession request = gson.fromJson(json, ActiveSession.class);
		this.session = request.getSession();
		this.ttl = request.getTtl();
		this.id = request.getId();
	}

	@DynamoDBHashKey(attributeName = "session")
	public Integer getSession() {
		return session;
	}

	public void setSession(Integer session) {
		this.session = session;
	}

	@DynamoDBAttribute(attributeName = "ttl")
	public String getTtl() {
		return ttl;
	}

	public void setTtl(String ttl) {
		this.ttl = ttl;
	}

	 @DynamoDBAttribute(attributeName = "id")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(this);
	}
}
