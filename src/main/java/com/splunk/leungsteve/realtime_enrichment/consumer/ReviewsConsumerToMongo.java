package com.splunk.leungsteve.realtime_enrichment.consumer;

import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.bson.Document;
import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

public class ReviewsConsumerToMongo {

    public static JSONObject prefixJSONKeys(JSONObject json1, String prefix) {
        JSONObject newJSON = new JSONObject();
//        Iterator<String> keys = json1.keys();
        json1.keySet().forEach(key -> {
            if ("business_id".equals(key)) {
                newJSON.put(key, json1.get(key));
            }else if ("review_id".equals(key)) {
                newJSON.put(key, json1.get(key));
            }else if ("user_id".equals(key)) {
                newJSON.put(key, json1.get(key));
            }
            else {
                newJSON.put(prefix +"_" + key, json1.get(key));
            }
        });
        return newJSON;
    }

    public static JSONObject mergeJSONObjects(JSONObject json1, JSONObject json2) {
        JSONObject mergedJSON = new JSONObject();
        try {
            mergedJSON = new JSONObject(json1, JSONObject.getNames(json1));
            for (String crunchifyKey : JSONObject.getNames(json2)) {
                mergedJSON.put(crunchifyKey, json2.get(crunchifyKey));
            }
        } catch (JSONException e) {
            throw new RuntimeException("JSON Exception" + e);
        }
        return mergedJSON;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Logger logger = LoggerFactory.getLogger(ReviewsConsumerToMongo.class.getName());
        //Establish connection to MongoDB
        String connectionString = "mongodb://mongodb/O11y";
        try (MongoClient mongoClient = MongoClients.create(connectionString)) {
            List<Document> databases = mongoClient.listDatabases().into(new ArrayList<>());
            databases.forEach(db -> logger.info(db.toJson()));
            MongoCredential credential; // Creating Credentials
            credential = MongoCredential.createCredential("O11y", "O11y", "O11y".toCharArray());
            MongoDatabase O11yDB = mongoClient.getDatabase("O11y"); // Accessing the database
            MongoCollection O11yCollection = O11yDB.getCollection("O11yCollection");
            // Set up Kafka Connection
            String bootstrapServers = "kafka-0.kafka-headless.default.svc.cluster.local:9092";
            String groupId = "reviews_to_db";
            String topic = "reviews";
            Properties properties = new Properties();
            properties.setProperty("bootstrap.servers", bootstrapServers);
            properties.setProperty("key.deserializer", StringDeserializer.class.getName());
            properties.setProperty("value.deserializer", StringDeserializer.class.getName());
            properties.setProperty("group.id", groupId);
            properties.setProperty("auto.offset.reset", "earliest");
            KafkaConsumer<String, String> consumer = new KafkaConsumer(properties);
            consumer.subscribe(Arrays.asList(topic));

            // Consume continuously from Reviews topic
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100L));
                Iterator var8 = records.iterator();
                ConsumerRecord record;
                int var_partition;
                var8 = records.iterator();
                while (var8.hasNext()) {
                    record = (ConsumerRecord) var8.next();
                    PrintStream var10000 = System.out;
                    var_partition = record.partition();
//                    var10000.println("Partition: " + var_partition + ", Offset:" + record.offset());
                    logger.info("Partition: " + var_partition + ", Offset:" + record.offset());

                    //extract business id
                    JSONObject review_json = new JSONObject(record.value().toString());
                    //extract user and business id
                    String business_id = review_json.getString("business_id");
                    String user_id = review_json.getString("user_id");
                    JSONObject prefixed_review_json = prefixJSONKeys(review_json, "review");

                    //get sentiment
                    HttpClient sentimentclient = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://sentiment:5001")).build();
                    HttpResponse<String> sentiment = sentimentclient.send(request, HttpResponse.BodyHandlers.ofString());
                    //get business details
                    HttpClient businessclient = HttpClient.newHttpClient();
                    HttpRequest businessrequest = HttpRequest.newBuilder().uri(URI.create("http://businessLookup:5002/business_lookup?business_id=" + business_id)).build();
                    HttpResponse<String> business_deets = businessclient.send(businessrequest, HttpResponse.BodyHandlers.ofString());
                    JSONObject business_json = new JSONObject(business_deets.body());
                    JSONObject prefixed_business_json = prefixJSONKeys(business_json, "business");
                    //get user details
                    HttpClient userclient = HttpClient.newHttpClient();
                    HttpRequest userrequest = HttpRequest.newBuilder().uri(URI.create("http://userLookup:5003/user_lookup?user_id=" + user_id)).build();
                    HttpResponse<String> user_deets = userclient.send(userrequest, HttpResponse.BodyHandlers.ofString());
                    JSONObject user_json = new JSONObject(user_deets.body());
                    JSONObject prefixed_user_json = prefixJSONKeys(user_json, "user");

                    //Create a combined json
                    JSONObject json_business = mergeJSONObjects(prefixed_review_json, prefixed_business_json);
                    JSONObject json_business_user = mergeJSONObjects(json_business, prefixed_user_json);

                    logger.info(review_json.toString());
                    logger.info(json_business_user.toString());

                    //Insert into MongoDB
                    Document myDoc = Document.parse(json_business_user.toString());
                    myDoc.append("sentiment", sentiment.body());
                    O11yCollection.insertOne(myDoc);
                }
            }
        }
    }
}