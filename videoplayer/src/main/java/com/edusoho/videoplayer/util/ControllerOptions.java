package com.edusoho.videoplayer.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by suju on 16/12/18.
 */

public class ControllerOptions {

    public static final String SEEK = "seek";
    public static final String SCREEN = "screen";
    public static final String MEDIA_LIST = "media_list";
    public static final String RATE = "rate";
    public static final String TOUCH_TO_SEEK = "touch_seek";
    public static final String TOUCH_TO_VOLUME = "touch_volume";

    private Map<String, Boolean> mOptionsMap;

    private ControllerOptions() {
        mOptionsMap = new HashMap<>();
    }

    private void addOption(String name, boolean value) {
        mOptionsMap.put(name, value);
    }

    public boolean getOption(String name) {
        if (!mOptionsMap.containsKey(name))  {
            return false;
        }
        return mOptionsMap.get(name);
    }

    public boolean getOption(String name, boolean defaultValue) {
        if (!mOptionsMap.containsKey(name))  {
            return defaultValue;
        }
        return mOptionsMap.get(name);
    }

    public static ControllerOptions getDefault() {
        ControllerOptions options = new ControllerOptions();
        options.addOption(SEEK, true);
        options.addOption(SCREEN, true);
        options.addOption(MEDIA_LIST, true);
        options.addOption(RATE, true);
        options.addOption(TOUCH_TO_SEEK, true);
        options.addOption(TOUCH_TO_VOLUME, true);
        return options;
    }

    public static class Builder {

        private ControllerOptions mControllerOptions;

        public Builder() {
            mControllerOptions = new ControllerOptions();
        }

        public Builder addOption(String name, boolean value) {
            mControllerOptions.addOption(name, value);
            return this;
        }

        public ControllerOptions build() {
            return mControllerOptions;
        }
    }
}
