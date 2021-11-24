package com.riferrei.streaming.pacman.alexa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;

import com.riferrei.streaming.pacman.utils.Player;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.attributes.SemanticAttributes;
import io.opentelemetry.context.Scope;
import redis.clients.jedis.Jedis;

import static com.amazon.ask.request.Predicates.intentName;
import static com.riferrei.streaming.pacman.utils.Constants.*;
import static com.riferrei.streaming.pacman.utils.SkillUtils.*;

public class AlexaPlayersHandler implements IntentRequestHandler {

    private Tracer tracer;

    public AlexaPlayersHandler(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(intentName(BEST_PLAYER_INTENT)
            .or(intentName(TOPN_PLAYERS_INTENT)));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {

        String speechText = null;
        ResourceBundle resourceBundle = getResourceBundle(input);

        Span alexaPlayersHandler = tracer.spanBuilder("alexa-players-handler")
            .setAttribute("intent-request-id", intentRequest.getRequestId())
            .setAttribute("intent-name", intentRequest.getIntent().getName())
            .setAttribute("intent-confirmation-status", intentRequest.getIntent().getConfirmationStatusAsString())
            .setAttribute("intent-slots", intentRequest.getIntent().getSlots().toString())
            .startSpan();
            
        try (Scope scope = alexaPlayersHandler.makeCurrent()) {

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

            Span rediszcard = tracer.spanBuilder("redis-zcard")
                .setAttribute(SemanticAttributes.DB_SYSTEM, "redis")
                .setAttribute(SemanticAttributes.DB_OPERATION, "zcard")
                .setAttribute(SemanticAttributes.DB_STATEMENT, "zcard")
                .setAttribute("cache-name", SCOREBOARD_CACHE)
                .startSpan();

            try (Scope childScope = rediszcard.makeCurrent()) {
                if (cacheServer.zcard(SCOREBOARD_CACHE) == 0) {
                    speechText = resourceBundle.getString(NO_PLAYERS);
                    return input.getResponseBuilder()
                        .withSpeech(speechText)
                        .build();
                }
            } finally {
                rediszcard.end();
            }
            
            if (input.matches(intentName(BEST_PLAYER_INTENT))) {

                Span bestPlayerSpan = tracer.spanBuilder("get-best-player").startSpan();
                try (Scope childScope = bestPlayerSpan.makeCurrent()) {
                    speechText = getBestPlayer(resourceBundle);
                } finally {
                    bestPlayerSpan.end();
                }

            } else if (input.matches(intentName(TOPN_PLAYERS_INTENT))) {

                Map<String, Slot> slots = intentRequest.getIntent().getSlots();
                Slot slot = slots.get(NUMBER_OF_PLAYERS_SLOT);
                int topNPlayers = 1;
                try {
                    topNPlayers = Integer.parseInt(slot.getValue());
                } catch (NumberFormatException nfe) {}

                Span topNPlayersSpan = tracer.spanBuilder("get-topn-players").startSpan();
                topNPlayersSpan.setAttribute("topNPlayers", topNPlayers);
                try (Scope childScope = topNPlayersSpan.makeCurrent()) {
                    speechText = getTopNPlayers(topNPlayers, resourceBundle);
                } finally {
                    topNPlayersSpan.end();
                }

            }

        } finally {
            alexaPlayersHandler.end();
        }

        return input.getResponseBuilder()
            .withSpeech(speechText)
            .build();

    }

    private String getBestPlayer(ResourceBundle resourceBundle) {
        
        final StringBuilder speechText = new StringBuilder();

        Set<String> bestPlayerKey = null;
        Span redisRevRange = tracer.spanBuilder("redis-revrange")
            .setAttribute(SemanticAttributes.DB_SYSTEM, "redis")
            .setAttribute(SemanticAttributes.DB_OPERATION, "zrevrange")
            .setAttribute(SemanticAttributes.DB_STATEMENT, "zrevrange")
            .setAttribute("cache-name", SCOREBOARD_CACHE)
            .startSpan();

        try (Scope scope = redisRevRange.makeCurrent()) {
            bestPlayerKey = cacheServer.zrevrange(SCOREBOARD_CACHE, 0, 0);
        } finally {
            redisRevRange.end();
        }

        String key = bestPlayerKey.iterator().next();
        String value = null;
        Span redisGet = tracer.spanBuilder("redis-get")
            .setAttribute(SemanticAttributes.DB_SYSTEM, "redis")
            .setAttribute(SemanticAttributes.DB_OPERATION, "get")
            .setAttribute(SemanticAttributes.DB_STATEMENT, "get")
            .setAttribute("string-key", key)
            .startSpan();

        try (Scope scope = redisGet.makeCurrent()) {
            value = cacheServer.get(key);
            redisGet.setAttribute("string-value", value);
        } finally {
            redisGet.end();
        }

        Player player = Player.getPlayer(key, value);
        String text = resourceBundle.getString(BEST_PLAYER);
        speechText.append(String.format(text, player.getUser()));

        return speechText.toString();

    }

    private String getTopNPlayers(int topNPlayers, ResourceBundle resourceBundle) {

        final StringBuilder speechText = new StringBuilder();
        long playersAvailable = 0;
        Span rediszcard = tracer.spanBuilder("redis-zcard")
            .setAttribute(SemanticAttributes.DB_SYSTEM, "redis")
            .setAttribute(SemanticAttributes.DB_OPERATION, "zcard")
            .setAttribute(SemanticAttributes.DB_STATEMENT, "zcard")
            .setAttribute("cache-name", SCOREBOARD_CACHE)
            .startSpan();

        try (Scope scope = rediszcard.makeCurrent()) {
            playersAvailable = cacheServer.zcard(SCOREBOARD_CACHE);
        } finally {
            rediszcard.end();
        }

        if (playersAvailable >= topNPlayers) {

            Set<String> playerKeys = null;
            Span rediszrevRange = tracer.spanBuilder("redis-zrevrange")
                .setAttribute(SemanticAttributes.DB_SYSTEM, "redis")
                .setAttribute(SemanticAttributes.DB_OPERATION, "zrevrange")
                .setAttribute(SemanticAttributes.DB_STATEMENT, "zrevrange")
                .setAttribute("cache-name", SCOREBOARD_CACHE)
                .startSpan();
                
            try (Scope scope = rediszrevRange.makeCurrent()) {
                playerKeys = cacheServer.zrevrange(SCOREBOARD_CACHE, 0, topNPlayers - 1);
            } finally {
                rediszrevRange.end();
            }

            List<Player> players = new ArrayList<>(playerKeys.size());
            Set<String> playerValues = new HashSet<>();
            Span redisGet = tracer.spanBuilder("redis-get")
                .setAttribute(SemanticAttributes.DB_SYSTEM, "redis")
                .setAttribute(SemanticAttributes.DB_OPERATION, "get")
                .setAttribute(SemanticAttributes.DB_STATEMENT, "get")
                .setAttribute("string-keys", playerKeys.toString())
                .startSpan();

            try (Scope innerScope = redisGet.makeCurrent()) {
                for (String key : playerKeys) {
                    String value = cacheServer.get(key);
                    playerValues.add(value);
                    players.add(Player.getPlayer(key, value));
                }
                redisGet.setAttribute("string-values", playerValues.toString());
            } finally {
                redisGet.end();
            }

            String and = resourceBundle.getString(AND);
            if (topNPlayers == 1) {
                Player player = players.get(0);
                String text = resourceBundle.getString(TOP_1_PLAYER);
                speechText.append(String.format(text, player.getUser()));
            } else if (topNPlayers == 2) {
                Player firstPlayer = players.get(0);
                Player secondPlayer = players.get(1);
                String text = resourceBundle.getString(TOP_N_PLAYERS);
                speechText.append(String.format(text, topNPlayers));
                speechText.append(firstPlayer.getUser());
                speechText.append(" ").append(and).append(" ");
                speechText.append(secondPlayer.getUser());
            } else {
                String text = resourceBundle.getString(TOP_N_PLAYERS);
                speechText.append(String.format(text, topNPlayers));
                for (int i = 0; i < topNPlayers; i++) {
                    Player player = players.get(i);
                    if ((i + 1) == topNPlayers) {
                        speechText.append(and).append(" ");
                        speechText.append(player.getUser());
                        speechText.append(".");
                    } else {
                        speechText.append(player.getUser());
                        speechText.append(", ");
                    }
                }
            }

        } else {
            String text = resourceBundle.getString(NOT_ENOUGH_PLAYERS);
            speechText.append(String.format(text, topNPlayers, playersAvailable));
        }

        return speechText.toString();

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
