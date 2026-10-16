package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

public final class VitogramSession {

    private static final String PREFS = "vitogram_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_MOBILE = "mobile";
    private static final String KEY_FIRST_NAME = "first_name";
    private static final String KEY_LAST_NAME = "last_name";

    private VitogramSession() {
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE
        );
    }

    public static void save(
            String token,
            long userId,
            String mobile,
            String firstName,
            String lastName
    ) {
        prefs().edit()
                .putString(KEY_TOKEN, token)
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_MOBILE, mobile)
                .putString(KEY_FIRST_NAME, firstName)
                .putString(KEY_LAST_NAME, lastName)
                .apply();
    }

    public static boolean isLoggedIn() {
        String token = prefs().getString(KEY_TOKEN, "");
        return token != null && !token.isEmpty()
                && prefs().getLong(KEY_USER_ID, 0) > 0;
    }

    public static String getToken() {
        return prefs().getString(KEY_TOKEN, "");
    }

    public static long getUserId() {
        return prefs().getLong(KEY_USER_ID, 0);
    }

    public static String getMobile() {
        return prefs().getString(KEY_MOBILE, "");
    }

    public static String getFirstName() {
        return prefs().getString(KEY_FIRST_NAME, "");
    }

    public static String getLastName() {
        return prefs().getString(KEY_LAST_NAME, "");
    }

    public static void clear() {
        prefs().edit().clear().apply();
    }
}
