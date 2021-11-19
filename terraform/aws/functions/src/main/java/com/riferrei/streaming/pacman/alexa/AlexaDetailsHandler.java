package com.riferrei.streaming.pacman.alexa;

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

public class AlexaDetailsHandler implements IntentRequestHandler {

    private Tracer tracer;

    public AlexaDetailsHandler(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(intentName(PLAYER_DETAILS_INTENT));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {

        String speechText = null;
        ResourceBundle resourceBundle = getResourceBundle(input);

        Span alexaDetailsHandler = tracer.spanBuilder("alexa-details-handler")
            .setAttribute("intent-request-id", intentRequest.getRequestId())
            .setAttribute("intent-name", intentRequest.getIntent().getName())
            .setAttribute("intent-confirmation-status", intentRequest.getIntent().getConfirmationStatusAsString())
            .setAttribute("intent-slots", intentRequest.getIntent().getSlots().toString())
            .startSpan();

        try (Scope scope = alexaDetailsHandler.makeCurrent()) {

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
    
            String slotName = null;
            String slotValue = null;
            Map<String, Slot> slots = intentRequest.getIntent().getSlots();
            Set<String> slotKeys = slots.keySet();
            for (String slotKey : slotKeys) {
                Slot slot = slots.get(slotKey);
                if (slot.getValue() != null && slot.getValue().length() > 0) {
                    slotName = slot.getName();
                    slotValue = slot.getValue();
                }
            }
    
            if (slotName == null && slotValue == null) {
                speechText = resourceBundle.getString(FAILED_QUESTION);
            }
    
            if (slotName.equals(POSITION_RELATIVE_SLOT) ||
                slotName.equals(POSITION_ABSOLUTE_SLOT)) {
                try {
                    int position = Integer.parseInt(slotValue);
                    Span playerDetailsByPosition = tracer.spanBuilder("player-details-by-position").startSpan();
                    try (Scope childScope = playerDetailsByPosition.makeCurrent()) {
                        speechText = getPlayerDetailsByPosition(position, resourceBundle);
                    } finally {
                        playerDetailsByPosition.end();;
                    }
                } catch (NumberFormatException nfe) {}
            } else if (slotName.equals(PLAYER_NAME_SLOT)) {
                Span playerDetailsByName = tracer.spanBuilder("player-details-by-name").startSpan();
                try (Scope childScope = playerDetailsByName.makeCurrent()) {
                    speechText = getPlayerDetailsByName(slotValue, resourceBundle);
                } finally {
                    playerDetailsByName.end();
                }
            }

        } finally {
            alexaDetailsHandler.end();
        }

        return input.getResponseBuilder()
            .withSpeech(speechText)
            .build();

    }

    private String getPlayerDetailsByPosition(int position, ResourceBundle resourceBundle) {
        
        final StringBuilder speechText = new StringBuilder();

        Span rediszcard = tracer.spanBuilder("redis-zcard")
            .setAttribute(SemanticAttributes.DB_SYSTEM, "redis")
            .setAttribute(SemanticAttributes.DB_OPERATION, "zcard")
            .setAttribute(SemanticAttributes.DB_STATEMENT, "zcard")
            .setAttribute("cache-name", SCOREBOARD_CACHE)
            .startSpan();

        try (Scope childScope = rediszcard.makeCurrent()) {

            if (position <= cacheServer.zcard(SCOREBOARD_CACHE)) {
                position = position - 1;
                Set<String> playerKey = null;
                Span redisRevRange = tracer.spanBuilder("redis-revrange")
                    .setAttribute(SemanticAttributes.DB_SYSTEM, "redis")
                    .setAttribute(SemanticAttributes.DB_OPERATION, "zrevrange")
                    .setAttribute(SemanticAttributes.DB_STATEMENT, "zrevrange")
                    .setAttribute("cache-name", SCOREBOARD_CACHE)
                    .startSpan();
                try (Scope innerScope = redisRevRange.makeCurrent()) {
                    playerKey = cacheServer.zrevrange(SCOREBOARD_CACHE, position, position);
                } finally {
                    redisRevRange.end();
                }
                String key = playerKey.iterator().next();
                String value = null;
                Span redisGet = tracer.spanBuilder("redis-get")
                    .setAttribute(SemanticAttributes.DB_SYSTEM, "redis")
                    .setAttribute(SemanticAttributes.DB_OPERATION, "get")
                    .setAttribute(SemanticAttributes.DB_STATEMENT, "get")
                    .setAttribute("string-key", key)
                    .startSpan();
                try (Scope innerScope = redisGet.makeCurrent()) {
                    value = cacheServer.get(key);
                    redisGet.setAttribute("string-value", value);
                } finally {
                    redisGet.end();
                }
                Player player = Player.getPlayer(key, value);
                speechText.append(getPlayerDetails(player, resourceBundle));
            } else {
                speechText.append(resourceBundle.getString(POSITION_DOESNT_EXIST));
            }
            
        } finally {
            rediszcard.end();
        }

        return speechText.toString();

    }

    private String getPlayerDetailsByName(String playerName, ResourceBundle resourceBundle) {

        final StringBuilder speechText = new StringBuilder();

        Span redisExists = tracer.spanBuilder("redis-exists")
            .setAttribute(SemanticAttributes.DB_SYSTEM, "redis")
            .setAttribute(SemanticAttributes.DB_OPERATION, "exists")
            .setAttribute(SemanticAttributes.DB_STATEMENT, "exists")
            .startSpan();

        try (Scope childScope = redisExists.makeCurrent()) {

            if (cacheServer.exists(playerName)) {
                String value = null;
                Span redisGet = tracer.spanBuilder("redis-get")
                    .setAttribute(SemanticAttributes.DB_SYSTEM, "redis")
                    .setAttribute(SemanticAttributes.DB_OPERATION, "get")
                    .setAttribute(SemanticAttributes.DB_STATEMENT, "get")
                    .setAttribute("string-key", playerName)
                    .startSpan();
                try (Scope innerScope = redisGet.makeCurrent()) {
                    value = cacheServer.get(playerName);
                    redisGet.setAttribute("string-value", value);
                } finally {
                    redisGet.end();
                }
                Player player = Player.getPlayer(playerName, value);
                speechText.append(getPlayerDetails(player, resourceBundle));
            } else {
                String text = resourceBundle.getString(NO_ONE_WITH_THIS_NAME);
                speechText.append(String.format(text, playerName));
            }
    
        } finally {
            redisExists.end();            
        }

        return speechText.toString();

    }

    private String getPlayerDetails(Player player, ResourceBundle resourceBundle) {

        final StringBuilder speechText = new StringBuilder();

        String text = resourceBundle.getString(PLAYER_DETAILS);
        speechText.append(String.format(text, player.getUser(),
            player.getScore(), player.getLevel()));
        
        switch (player.getLosses()) {
            case 0:
                text = resourceBundle.getString(ZERO_LOSSES_DETAILS);
                speechText.append(String.format(text, player.getUser()));
                break;
            case 1:
                text = resourceBundle.getString(ONE_LOSS_DETAILS);
                speechText.append(String.format(text, player.getUser()));
                break;
            default:
                text = resourceBundle.getString(N_LOSSES_DETAILS);
                speechText.append(String.format(text, player.getUser(), player.getLosses()));
                break;
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
