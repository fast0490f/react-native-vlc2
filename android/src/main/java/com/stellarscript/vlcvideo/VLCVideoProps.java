package com.stellarscript.vlcvideo;

final class VLCVideoProps {

    static final String MEDIA_PROP = "media";
    static final String MEDIA_SOURCE_URL_PROP = "sourceUrl";
    static final String MEDIA_START_TIME_PROP = "startTime";
    static final int MEDIA_START_TIME_DEFAULT_VALUE = 0;
    static final String MEDIA_AUTOPLAY_PROP = "autoplay";
    static final boolean MEDIA_AUTOPLAY_DEFAULT_VALUE = true;
    static final String KEY_CONTROL_ENABLED_PROP = "keyControlEnabled";
    static final boolean KEY_CONTROL_ENABLED_DEFAULT_VALUE = false;

    static final String PLAY_COMMAND_NAME = "play";
    static final int PLAY_COMMAND_ID = 1;
    static final String PAUSE_COMMAND_NAME = "pause";
    static final int PAUSE_COMMAND_ID = 2;
    static final String SEEK_COMMAND_NAME = "seek";
    static final int SEEK_COMMAND_ID = 3;
    static final int SEEK_COMMAND_TIME_ARGUMENT_INDEX = 0;
    static final String SET_FOCUSABLE_COMMAND_NAME = "setFocusable";
    static final int SET_FOCUSABLE_COMMAND_ID = 4;
    static final int SET_FOCUSABLE_FOCUSABLE_ARGUMENT_INDEX = 0;

}
