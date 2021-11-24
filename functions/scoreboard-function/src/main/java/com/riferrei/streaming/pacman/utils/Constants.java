package com.riferrei.streaming.pacman.utils;

public interface Constants {

    public static final String ORIGIN_ALLOWED = System.getenv("ORIGIN_ALLOWED");
    public static final String CACHE_SERVER_HOST = System.getenv("CACHE_SERVER_HOST");
    public static final String CACHE_SERVER_PORT = System.getenv("CACHE_SERVER_PORT");
    public static final String SCOREBOARD_CACHE = "scoreboard";

    public static final String PLAYER_KEY = "player";
    public static final String POST_METHOD = "POST";

    public static final String SCOREBOARD_FIELD = "scoreboard";
    public static final String HIGHEST_SCORE_FIELD = "HIGHEST_SCORE";
    public static final String HIGHEST_LEVEL_FIELD = "HIGHEST_LEVEL";
    public static final String TOTAL_LOSSES_FIELD = "TOTAL_LOSSES";

}
