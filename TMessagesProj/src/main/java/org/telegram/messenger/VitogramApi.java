package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class VitogramApi {
    private static final String BASE_URL =
            "https://vitogram.ariyant.shop/api/";
    private static final String PREFS = "vitogram_session";
    private static final String TOKEN_KEY = "token";
    private static final Handler MAIN =
            new Handler(Looper.getMainLooper());

    private VitogramApi() {}

    public interface Callback {
        void onResult(JSONObject result, Exception error);
    }

    public static void saveToken(Context context, String token) {
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(TOKEN_KEY, token).apply();
    }

    public static String getToken(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(TOKEN_KEY, null);
    }

    public static void clearToken(Context context) {
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().remove(TOKEN_KEY).apply();
    }

    public static void sendOtp(String mobile, Callback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("mobile", mobile);
            request("POST", "send-code", body, null, callback);
        } catch (Exception e) {
            deliver(callback, null, e);
        }
    }

    public static void verifyOtp(
            String mobile, String code, Callback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("mobile", mobile);
            body.put("code", code);
            request("POST", "verify-code", body, null, callback);
        } catch (Exception e) {
            deliver(callback, null, e);
        }
    }

    public static void users(Context context, String search,
                             Callback callback) {
        request("GET", "users", null, getToken(context), callback);
    }

    public static void conversations(Context context,
                                     Callback callback) {
        request("GET", "conversations", null,
                getToken(context), callback);
    }

    public static void messages(Context context, int peerId,
                                int limit, Callback callback) {
        try {
            String query = "?with_user_id=" + peerId + "&after_id=0";
            request("GET", "messages" + query, null,
                    getToken(context), callback);
        } catch (Exception e) {
            deliver(callback, null, e);
        }
    }

    public static void sendMessage(Context context, int recipientId,
                                   String text, Callback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("recipient_id", recipientId);
            body.put("text", text);
            request("POST", "messages", body,
                    getToken(context), callback);
        } catch (Exception e) {
            deliver(callback, null, e);
        }
    }

    private static void request(
            String method, String path, JSONObject body,
            String token, Callback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection)
                        new URL(BASE_URL + path).openConnection();

                connection.setRequestMethod(method);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty(
                        "Accept", "application/json");

                if (token != null && !token.isEmpty()) {
                    connection.setRequestProperty(
                            "Authorization", "Bearer " + token);
                }

                if (body != null) {
                    connection.setDoOutput(true);
                    connection.setRequestProperty(
                            "Content-Type",
                            "application/json; charset=UTF-8");

                    try (OutputStream out =
                                 connection.getOutputStream()) {
                        out.write(body.toString().getBytes(
                                StandardCharsets.UTF_8));
                    }
                }

                int status = connection.getResponseCode();
                InputStream stream =
                        status >= 200 && status < 400
                                ? connection.getInputStream()
                                : connection.getErrorStream();

                StringBuilder response = new StringBuilder();
                if (stream != null) {
                    try (BufferedReader reader =
                                 new BufferedReader(
                                         new InputStreamReader(
                                                 stream,
                                                 StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }
                }

                JSONObject json = response.length() == 0
                        ? new JSONObject()
                        : new JSONObject(response.toString());

                if (status < 200 || status >= 300) {
                    String message = json.optString(
                            "message", "خطای سرور: " + status);
                    throw new Exception(message);
                }

                deliver(callback, json, null);
            } catch (Exception e) {
                deliver(callback, null, e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }, "vitogram-api").start();
    }

    private static void deliver(
            Callback callback, JSONObject result, Exception error) {
        if (callback != null) {
            MAIN.post(() -> callback.onResult(result, error));
        }
    }
}
