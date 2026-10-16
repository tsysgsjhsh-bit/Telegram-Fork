package org.telegram.ui.vitogram;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.VitogramApi;

public class VitogramHomeActivity extends Activity {
    private LinearLayout root, content;
    private String mobile = "";
    private int currentUserId = 0;
    private int currentPeerId = 0;
    private String currentUsername = "";
    private String currentDisplayName = "";
    private EditText phoneInput, codeInput, searchInput, messageInput;
    private final int blue = Color.rgb(35, 112, 220);

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        if (VitogramApi.getToken(this) != null) {
            showPeople();
        } else {
            showLogin();
        }
    }

    private int dp(int value) {
        return (int) (value *
                getResources().getDisplayMetrics().density + 0.5f);
    }

    private void base(String title) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(12));
        root.setBackgroundColor(Color.rgb(246, 248, 252));

        TextView heading = new TextView(this);
        heading.setText(title);
        heading.setTextSize(25);
        heading.setTextColor(Color.rgb(25, 45, 75));
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setPadding(0, dp(8), 0, dp(18));
        root.addView(heading, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private LinearLayout.LayoutParams margin() {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(10);
        return params;
    }

    private EditText edit(String hint, int inputType) {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setHint(hint);
        field.setInputType(inputType);
        field.setTextSize(16);
        field.setPadding(dp(12), dp(10), dp(12), dp(10));
        content.addView(field, margin());
        return field;
    }

    private Button button(String text, Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(blue));
        button.setOnClickListener(view -> action.run());
        content.addView(button, margin());
        return button;
    }

    private void label(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(16);
        view.setTextColor(Color.rgb(35, 48, 65));
        view.setPadding(dp(12), dp(12), dp(12), dp(12));
        content.addView(view, margin());
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    private void buttonBusy(boolean busy) {
        if (root != null) setEnabledRecursive(root, !busy);
    }

    private void setEnabledRecursive(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setEnabledRecursive(group.getChildAt(i), enabled);
            }
        }
    }

    private void showLogin() {
        base("Vitogram");
        label("ورود با شماره موبایل");
        phoneInput = edit("شماره موبایل؛ مثال 09123456789",
                InputType.TYPE_CLASS_PHONE);

        button("ارسال کد پیامکی", () -> {
            mobile = phoneInput.getText().toString().trim();
            if (!mobile.matches("09\\d{9}")) {
                toast("شماره باید ۱۱ رقم و با 09 شروع شود.");
                return;
            }

            buttonBusy(true);
            VitogramApi.sendOtp(mobile, (result, error) -> {
                buttonBusy(false);
                if (error != null) {
                    toast(error.getMessage());
                } else {
                    showCode();
                }
            });
        });
    }

    private void showCode() {
        base("تأیید شماره");
        label("کد پیامکی ارسال‌شده را وارد کن.");
        codeInput = edit("کد پیامک",
                InputType.TYPE_CLASS_NUMBER);

        button("تأیید و ورود", () -> {
            String code = codeInput.getText().toString().trim();
            if (!code.matches("\\d{5,6}")) {
                toast("کد واردشده معتبر نیست.");
                return;
            }

            buttonBusy(true);
            VitogramApi.verifyOtp(mobile, code, (result, error) -> {
                buttonBusy(false);
                if (error != null) {
                    toast(error.getMessage());
                    return;
                }

                String token = result.optString("token", "");
                if (token.isEmpty()) {
                    toast("سرور توکن ورود برنگرداند.");
                    return;
                }

                currentUserId = result.optInt(
                        "user_id", result.optInt("id", 0));
                VitogramApi.saveToken(this, token);
                showPeople();
            });
        });

        button("بازگشت", this::showLogin);
    }

    private void showPeople() {
        currentPeerId = 0;
        base("کاربران ویتوگرام");
        searchInput = edit("جست‌وجوی نام یا نام کاربری",
                InputType.TYPE_CLASS_TEXT);
        button("نمایش کاربران", () ->
                fetchUsers(searchInput.getText().toString()));
        button("گفتگوهای من", this::loadConversations);
        button("خروج از حساب", () -> {
            VitogramApi.clearToken(this);
            currentUserId = 0;
            showLogin();
        });
        fetchUsers("");
    }

    private void fetchUsers(String search) {
        VitogramApi.users(this, search, (json, error) -> {
            if (error != null) {
                toast(error.getMessage());
                return;
            }

            content.removeAllViews();
            searchInput = edit("جست‌وجوی نام یا نام کاربری",
                    InputType.TYPE_CLASS_TEXT);
            button("جست‌وجو", () ->
                    fetchUsers(searchInput.getText().toString()));
            button("گفتگوهای من", this::loadConversations);

            JSONArray users = json.optJSONArray("users");
            if (users == null || users.length() == 0) {
                label("کاربر دیگری پیدا نشد.");
                return;
            }

            String query = search == null
                    ? "" : search.trim().toLowerCase();

            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.optJSONObject(i);
                if (user == null) continue;

                int id = user.optInt("id", 0);
                if (id <= 0 || id == currentUserId) continue;

                String username = user.optString("username", "");
                if ("null".equals(username)) username = "";

                String first = user.optString("first_name", "");
                String last = user.optString("last_name", "");
                String name = (first + " " + last).trim();

                if (name.isEmpty()) {
                    name = username.isEmpty()
                            ? "کاربر " + id : username;
                }

                String searchable =
                        (name + " " + username).toLowerCase();
                if (!query.isEmpty() &&
                        !searchable.contains(query)) continue;

                final int peerId = id;
                final String peerUsername = username;
                final String peerName = name;

                String title = name;
                if (!username.isEmpty()) title += "  ·  @" + username;
                button(title, () ->
                        openChat(peerId, peerUsername, peerName));
            }
        });
    }

    private void loadConversations() {
        currentPeerId = 0;
        base("گفتگوهای من");
        button("بازگشت به کاربران", this::showPeople);

        VitogramApi.conversations(this, (json, error) -> {
            if (error != null) {
                toast(error.getMessage());
                return;
            }

            JSONArray conversations =
                    json.optJSONArray("conversations");

            if (conversations == null ||
                    conversations.length() == 0) {
                label("هنوز گفتگویی نداری؛ از بخش کاربران شروع کن.");
                return;
            }

            for (int i = 0; i < conversations.length(); i++) {
                JSONObject conversation =
                        conversations.optJSONObject(i);
                if (conversation == null) continue;

                JSONObject user = conversation.optJSONObject("user");
                JSONObject last =
                        conversation.optJSONObject("last_message");
                if (user == null) continue;

                int peerId = user.optInt("id", 0);
                String username = user.optString("username", "");
                String name = (user.optString("first_name", "") + " " +
                        user.optString("last_name", "")).trim();

                if (name.isEmpty()) {
                    name = username.isEmpty()
                            ? "کاربر " + peerId : username;
                }

                String lastText = last == null
                        ? "" : last.optString("text", "");

                final int id = peerId;
                final String un = username;
                final String display = name;

                button(name + "\n" + lastText, () ->
                        openChat(id, un, display));
            }
        });
    }

    private void openChat(int peerId, String username, String displayName) {
        if (peerId <= 0) {
            toast("شناسه کاربر معتبر نیست.");
            return;
        }

        currentPeerId = peerId;
        currentUsername = username;
        currentDisplayName = displayName;

        base(displayName);
        button("بازگشت به کاربران", this::showPeople);
        messageInput = edit("پیام…", InputType.TYPE_CLASS_TEXT);

        button("ارسال پیام", () -> {
            String text = messageInput.getText().toString().trim();
            if (text.isEmpty()) return;
            if (text.length() > 4000) {
                toast("حداکثر طول پیام ۴۰۰۰ نویسه است.");
                return;
            }

            buttonBusy(true);
            VitogramApi.sendMessage(this, currentPeerId, text,
                    (result, error) -> {
                        buttonBusy(false);
                        if (error != null) {
                            toast(error.getMessage());
                        } else {
                            openChat(peerId, username, displayName);
                        }
                    });
        });

        VitogramApi.messages(this, peerId, 100, (json, error) -> {
            if (error != null) {
                toast(error.getMessage());
                return;
            }

            JSONArray messages = json.optJSONArray("messages");
            if (messages == null || messages.length() == 0) {
                label("هنوز پیامی نیست.");
                return;
            }

            for (int i = 0; i < messages.length(); i++) {
                JSONObject message = messages.optJSONObject(i);
                if (message == null) continue;

                int senderId = message.optInt("sender_id", 0);
                String sender = senderId == currentUserId
                        ? "من" : displayName;
                label(sender + ": " +
                        message.optString("text", ""));
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (currentPeerId > 0) {
            showPeople();
        } else if (VitogramApi.getToken(this) != null) {
            showPeople();
        } else {
            showLogin();
        }
    }
}
