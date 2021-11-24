package com.riferrei.streaming.pacman.utils;

public interface Constants {

    public static final String BOOTSTRAP_SERVERS = System.getenv("BOOTSTRAP_SERVERS");
    public static final String CLUSTER_API_KEY = System.getenv("CLUSTER_API_KEY");
    public static final String CLUSTER_API_SECRET = System.getenv("CLUSTER_API_SECRET");
    public static final String ORIGIN_ALLOWED = System.getenv("ORIGIN_ALLOWED");

    public static final String TOPIC_KEY = "topic";
    public static final String ORIGIN_KEY = "origin";
    public static final String POST_METHOD = "POST";

    public static final String USER_GAME_TOPIC = "USER_GAME";
    public static final String USER_LOSSES_TOPIC = "USER_LOSSES";

}
