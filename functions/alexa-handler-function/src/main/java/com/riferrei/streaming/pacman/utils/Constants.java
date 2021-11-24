package com.riferrei.streaming.pacman.utils;

public interface Constants {

    public static final String CACHE_SERVER_HOST = System.getenv("CACHE_SERVER_HOST");
    public static final String CACHE_SERVER_PORT = System.getenv("CACHE_SERVER_PORT");
    public static final String SCOREBOARD_CACHE = "scoreboard";

    public static final String HIGHEST_SCORE_FIELD = "HIGHEST_SCORE";
    public static final String HIGHEST_LEVEL_FIELD = "HIGHEST_LEVEL";
    public static final String TOTAL_LOSSES_FIELD = "TOTAL_LOSSES";

    public static final String BEST_PLAYER_INTENT = "BestPlayerIntent";
    public static final String TOPN_PLAYERS_INTENT = "TopNPlayersIntent";
    public static final String PLAYER_DETAILS_INTENT = "PlayerDetailsIntent";
    public static final String NUMBER_OF_PLAYERS_SLOT = "numberOfPlayers";
    public static final String PLAYER_NAME_SLOT = "playerName";
    public static final String POSITION_RELATIVE_SLOT = "positionRelative";
    public static final String POSITION_ABSOLUTE_SLOT = "positionAbsolute";

    public static final String SPEECH_TEXT = "SpeechText";
    public static final String ALEXA_HELP = "ALEXA_HELP";
    public static final String GOODBYE = "GOODBYE";
    public static final String NO_PLAYERS = "NO_PLAYERS";
    public static final String BEST_PLAYER = "BEST_PLAYER";
    public static final String TOP_1_PLAYER = "TOP_1_PLAYER";
    public static final String TOP_N_PLAYERS = "TOP_N_PLAYERS";
    public static final String NOT_ENOUGH_PLAYERS = "NOT_ENOUGH_PLAYERS";
    public static final String FAILED_QUESTION = "FAILED_QUESTION";
    public static final String POSITION_DOESNT_EXIST = "POSITION_DOESNT_EXIST";
    public static final String NO_ONE_WITH_THIS_NAME = "NO_ONE_WITH_THIS_NAME";
    public static final String PLAYER_DETAILS = "PLAYER_DETAILS";
    public static final String ZERO_LOSSES_DETAILS = "ZERO_LOSSES_DETAILS";
    public static final String ONE_LOSS_DETAILS = "ONE_LOSS_DETAILS";
    public static final String N_LOSSES_DETAILS = "N_LOSSES_DETAILS";
    public static final String AND = "AND";

}
