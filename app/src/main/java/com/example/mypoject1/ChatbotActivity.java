package com.example.mypoject1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;

public class ChatbotActivity extends AppCompatActivity {

    private static final String OPENAI_API_KEY = "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";

    private EditText userInput;
    private LinearLayout chatMessagesLayout;
    private Button sendMessageBtn;
    private DrawerLayout drawerLayout;

    private static final String TAG = "chatbot";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        // Set up views
        userInput = findViewById(R.id.userInput);
        chatMessagesLayout = findViewById(R.id.chatMessages);
        sendMessageBtn = findViewById(R.id.sendMessageBtn);

        Log.d(TAG, "Views are set up");

        sendMessageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Send Message button clicked");
                sendMessage();
            }
        });
    }

    private void sendMessage() {
        String message = userInput.getText().toString().trim();
        Log.d(TAG, "Message entered: " + message);

        if (!message.isEmpty()) {
            addMessageToChat("You: " + message);
            userInput.setText("");
            try {
                sendToOpenAI(message);
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error sending message to OpenAI", e);
            }
        } else {
            Log.d(TAG, "Message is empty, not sending");
        }
    }

    private void addMessageToChat(String message) {
        Log.d(TAG, "Adding message to chat: " + message);
        TextView messageView = new TextView(this);
        messageView.setText(message);
        chatMessagesLayout.addView(messageView);
    }

    private void sendToOpenAI(String message) throws IOException, JSONException {
        Log.d(TAG, "Preparing to send message to OpenAI");

        OkHttpClient client = new OkHttpClient();
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("model", "gpt4o");

        try {
            JSONArray messagesArray = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", message);
            messagesArray.put(userMessage);
            jsonBody.put("messages", messagesArray);

            Log.d(TAG, "Request JSON body: " + jsonBody.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON for request", e);
            throw e;
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + OPENAI_API_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Request failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "Response received: " + responseBody);
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        JSONArray choices = jsonResponse.getJSONArray("choices");
                        if (choices.length() > 0) {
                            JSONObject messageObject = choices.getJSONObject(0).getJSONObject("message");
                            String botReply = messageObject.getString("content");
                            runOnUiThread(() -> {
                                Log.d(TAG, "Bot reply: " + botReply);
                                addMessageToChat("Bot: " + botReply);
                            });
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON response", e);
                    }
                } else {
                    Log.e(TAG, "Response not successful, status code: " + response.code());
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Log.d(TAG, "Menu item selected: " + id);

        if (id == R.id.menu_home) {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
        } else if (id == R.id.menu_logout) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } else if (id == R.id.menu_camera) {
            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
        } else if (id == R.id.menu_timer) {
            Intent intent = new Intent(this, TimerActivity.class);
            startActivity(intent);
        } else if (id == R.id.menu_userdetails) {
            Intent intent = new Intent(this, UserDetailsActivity.class);
            startActivity(intent);
        } else if (id == R.id.menu_ForgotPassword) {
            Intent intent = new Intent(this, ForgotPasswordActivity.class);
            startActivity(intent);
        } else if (id == R.id.menu_ChatBot) {
            Intent intent = new Intent(this, ChatbotActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
