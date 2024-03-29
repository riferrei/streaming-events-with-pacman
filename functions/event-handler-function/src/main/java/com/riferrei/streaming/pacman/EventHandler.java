package com.riferrei.streaming.pacman;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import static com.riferrei.streaming.pacman.utils.Constants.*;
import static com.riferrei.streaming.pacman.utils.KafkaUtils.*;

public class EventHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {

        final APIGatewayProxyResponseEvent response =
            new APIGatewayProxyResponseEvent();

        response.setHeaders(Map.of(
            "Access-Control-Allow-Headers", "*",
            "Access-Control-Allow-Methods", POST_METHOD,
            "Access-Control-Allow-Origin", ORIGIN_ALLOWED));

        if (event.getHeaders() == null || event.getHeaders().isEmpty()) {
            response.setBody("Thanks for waking me up");
            return response;
        }

        if (event.getHeaders().containsKey(ORIGIN_KEY)) {
            String origin = event.getHeaders().get(ORIGIN_KEY);
            if (origin.equals(ORIGIN_ALLOWED)) {
                if (event.getQueryStringParameters() != null && event.getBody() != null) {
                    String eventData = event.getBody();
                    String topic = event.getQueryStringParameters().get(TOPIC_KEY);
                    String user = extractUserFromEvent(eventData);
                    ProducerRecord<String, String> record = new ProducerRecord<>(topic, user, eventData);
                    producer.send(record, (metadata, exception) -> {
                        StringBuilder message = new StringBuilder();
                        message.append("Event sent successfully to topic '");
                        message.append(metadata.topic()).append("' on the ");
                        message.append("partition ").append(metadata.partition());
                        response.setBody(message.toString());
                    });
                    producer.flush();
                }
            }
        }

        return response;

    }

    private String extractUserFromEvent(String payload) {
        JsonElement root = JsonParser.parseString(payload);
        return root.getAsJsonObject().get("user").getAsString();
    }

    private static KafkaProducer<String, String> producer;

    static {
        producer = createProducer();
        createTopics(Map.of(USER_GAME_TOPIC, 6,
                            USER_LOSSES_TOPIC, 6));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (producer != null) {
                producer.close();
            }
        }));
    }

}
