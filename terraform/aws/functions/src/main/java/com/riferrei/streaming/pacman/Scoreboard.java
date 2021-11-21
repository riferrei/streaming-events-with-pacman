package com.riferrei.streaming.pacman;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.attributes.SemanticAttributes;
import io.opentelemetry.context.Scope;

import redis.clients.jedis.Jedis;
import com.riferrei.streaming.pacman.utils.Player;
import static com.riferrei.streaming.pacman.utils.Constants.*;

public class Scoreboard implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Tracer tracer =
        GlobalOpenTelemetry.getTracer("scoreboard-api-tracer");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        
        final APIGatewayProxyResponseEvent response =
            new APIGatewayProxyResponseEvent();

        response.setHeaders(Map.of(
            "Access-Control-Allow-Headers", "*",
            "Access-Control-Allow-Methods", POST_METHOD,
            "Access-Control-Allow-Origin", ORIGIN_ALLOWED));
    
        String player = null;
        if (event.getQueryStringParameters() != null) {
            if (event.getQueryStringParameters().containsKey(PLAYER_KEY)) {
                player = event.getQueryStringParameters().get(PLAYER_KEY);
            }
        }

        Span getScoreboard = tracer.spanBuilder("getScoreboard")
            .setAttribute("player", player != null ? player : null)
            .startSpan();
        try (Scope scope = getScoreboard.makeCurrent()) {
            response.setBody(getScoreboard(player));
        } finally {
            getScoreboard.end();
        }

        return response;

    }

    private static String getScoreboard(String player) {

        Span redisConnect = tracer.spanBuilder("redis-connect")
            .setAttribute(SemanticAttributes.DB_SYSTEM, "redis")
            .setAttribute(SemanticAttributes.DB_OPERATION, "connect")
            .setAttribute(SemanticAttributes.DB_STATEMENT, "connect")
            .startSpan();

        try (Scope childScope = redisConnect.makeCurrent()) {
            cacheServer.connect();
        } finally {
            redisConnect.end();
        }

        JsonObject rootObject = new JsonObject();

        if (player != null) {

            JsonObject playerEntry = new JsonObject();

            Span redisExists = tracer.spanBuilder("redis-exists")
                .setAttribute(SemanticAttributes.DB_SYSTEM, "redis")
                .setAttribute(SemanticAttributes.DB_OPERATION, "exists")
                .setAttribute(SemanticAttributes.DB_STATEMENT, "exists")
                .startSpan();

            try (Scope childScope = redisExists.makeCurrent()) {

                if (cacheServer.exists(player)) {

                    String value = null;
                    Span redisGet = tracer.spanBuilder("redis-get")
                        .setAttribute(SemanticAttributes.DB_SYSTEM, "redis")
                        .setAttribute(SemanticAttributes.DB_OPERATION, "get")
                        .setAttribute(SemanticAttributes.DB_STATEMENT, "get")
                        .setAttribute("string-key", player)
                        .startSpan();

                    try (Scope innerScope = redisGet.makeCurrent()) {
                        value = cacheServer.get(player);
                        redisGet.setAttribute("string-value", value);
                    } finally {
                        redisGet.end();
                    }
                    
                    Player _player = Player.getPlayer(player, value);
                    playerEntry.addProperty(Player.USER, _player.getUser());
                    playerEntry.addProperty(Player.SCORE, _player.getScore());
                    playerEntry.addProperty(Player.LEVEL, _player.getLevel());
                    playerEntry.addProperty(Player.LOSSES, _player.getLosses());
                }

            } finally {
                redisExists.end();
            }

            rootObject.add(SCOREBOARD_FIELD, playerEntry);

        } else {

            JsonArray playerEntries = new JsonArray();
            long playersAvailable = 0;

            Span rediszcard = tracer.spanBuilder("redis-zcard")
                .setAttribute(SemanticAttributes.DB_SYSTEM, "redis")
                .setAttribute(SemanticAttributes.DB_OPERATION, "zcard")
                .setAttribute(SemanticAttributes.DB_STATEMENT, "zcard")
                .setAttribute("cache-name", SCOREBOARD_CACHE)
                .startSpan();

            try (Scope childScope = rediszcard.makeCurrent()) {
                playersAvailable = cacheServer.zcard(SCOREBOARD_CACHE);
            } finally {
                rediszcard.end();
            }

            if (playersAvailable > 0) {

                Set<String> playerKeys = null;
                Span rediszrevRange = tracer.spanBuilder("redis-zrevrange")
                    .setAttribute(SemanticAttributes.DB_SYSTEM, "redis")
                    .setAttribute(SemanticAttributes.DB_OPERATION, "zrevrange")
                    .setAttribute(SemanticAttributes.DB_STATEMENT, "zrevrange")
                    .setAttribute("cache-name", SCOREBOARD_CACHE)
                    .startSpan();

                try (Scope innerScope = rediszrevRange.makeCurrent()) {
                    playerKeys = cacheServer.zrevrange(SCOREBOARD_CACHE, 0, -1);
                } finally {
                    rediszrevRange.end();
                }

                Span redisGet = tracer.spanBuilder("redis-get")
                    .setAttribute(SemanticAttributes.DB_SYSTEM, "redis")
                    .setAttribute(SemanticAttributes.DB_OPERATION, "get")
                    .setAttribute(SemanticAttributes.DB_STATEMENT, "get")
                    .setAttribute("string-keys", playerKeys.toString())
                    .startSpan();

                try (Scope innerScope = redisGet.makeCurrent()) {

                    Set<String> playerValues = new HashSet<>();
                    for (String key : playerKeys) {
                        String value = cacheServer.get(key);
                        playerValues.add(value);
                        Player _player = Player.getPlayer(key, value);
                        JsonObject playerEntry = new JsonObject();
                        playerEntry.addProperty(Player.USER, _player.getUser());
                        playerEntry.addProperty(Player.SCORE, _player.getScore());
                        playerEntry.addProperty(Player.LEVEL, _player.getLevel());
                        playerEntry.addProperty(Player.LOSSES, _player.getLosses());
                        playerEntries.add(playerEntry);
                    }
                    redisGet.setAttribute("string-values", playerValues.toString());

                } finally {
                    redisGet.end();
                }
                
            }
    
            rootObject.add(SCOREBOARD_FIELD, playerEntries);

        }

        return new Gson().toJson(rootObject);

    }

    private static Jedis cacheServer;

    static {
        cacheServer = new Jedis(CACHE_SERVER_HOST, Integer.parseInt(CACHE_SERVER_PORT));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (cacheServer != null) {
                cacheServer.disconnect();
            }
        }));
    }

}
