package com.example.mypoject1;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatbotActivity extends AppCompatActivity {

    private static final String OPENAI_API_KEY = "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";
    private static final String TAG = "chatbot";
    private static final int FILE_SELECT_CODE = 100;

    private TextInputEditText userInput;
    private TextInputLayout messageInputLayout;
    private FloatingActionButton sendMessageFab;
    private RecyclerView chatRecyclerView;
    private ChatMessagesAdapter chatAdapter;
    private List<ChatMessage> messagesList = new ArrayList<>();

    // Conversation history for the API
    private JSONArray conversationHistory = new JSONArray();
    // To keep track of the "Thinking..." placeholder
    private boolean isWaitingForResponse = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        // Initialize back button and set its click listener
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ChatbotActivity.this, HomeActivity.class));
            }
        });

        // Initialize views
        userInput = findViewById(R.id.userInput);
        messageInputLayout = findViewById(R.id.messageInputContainer);
        sendMessageFab = findViewById(R.id.sendMessageFab);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);

        // Set up RecyclerView with the adapter
        chatAdapter = new ChatMessagesAdapter(messagesList);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Initially disable send button
        setSendButtonState(false);

        // Add a TextWatcher to update the send button tint and enabled state
        userInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s.length() > 0;
                setSendButtonState(hasText);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed
            }
        });

        // Add system prompt to conversation history
        addSystemPrompt();

        // Send button click handler
        sendMessageFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        // Attachment icon click handler from TextInputLayout
        messageInputLayout.setStartIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openFileChooser();
            }
        });
    }

    // Method to update the send button's enabled state and tint color
    private void setSendButtonState(boolean enabled) {
        sendMessageFab.setEnabled(enabled);
        if (enabled) {
            // White tint when enabled
            sendMessageFab.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, android.R.color.white)
            );
        } else {
            // Grey tint when disabled (adjust to your preferred grey)
            sendMessageFab.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.grey)
            );
        }
    }

    // Inflate menu for file attachment if needed (optional)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    // Handle menu selections (back/home actions if any)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_file) {
            openFileChooser();
            return true;
        } else if (id == R.id.menu_home) {
            startActivity(new Intent(this, HomeActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Add an initial system prompt for context
    private void addSystemPrompt() {
        try {
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "You are a knowledgeable running coach. Answer every question with an emphasis on training practices for running. If the user asks general questions, answer them and gently steer the conversation back to running.");
            conversationHistory.put(systemMessage);
        } catch (JSONException e) {
            Log.e(TAG, "Error adding system prompt", e);
        }
    }

    // Sends the user message and adds a "Thinking..." placeholder
    private void sendMessage() {
        String message = userInput.getText().toString().trim();
        if (!message.isEmpty() && !isWaitingForResponse) {
            // Add user message
            ChatMessage chatMessage = new ChatMessage("user", message);
            addMessage(chatMessage);
            appendToConversation("user", message);
            userInput.setText("");

            // Add a "Thinking..." placeholder from the bot
            isWaitingForResponse = true;
            ChatMessage thinkingMessage = new ChatMessage("assistant", "Thinking...");
            addMessage(thinkingMessage);

            try {
                sendToOpenAIUsingHistory();
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error sending message to OpenAI", e);
            }
        } else if (isWaitingForResponse) {
            Toast.makeText(this, "Please wait for the bot to reply.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
        }
    }

    // Add a message to the RecyclerView
    private void addMessage(ChatMessage message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chatAdapter.addMessage(message);
                chatRecyclerView.smoothScrollToPosition(messagesList.size() - 1);
            }
        });
    }

    // Append a message to the conversation history JSON array
    private void appendToConversation(String role, String content) {
        try {
            JSONObject messageObj = new JSONObject();
            messageObj.put("role", role);
            messageObj.put("content", content);
            conversationHistory.put(messageObj);
        } catch (JSONException e) {
            Log.e(TAG, "Error appending to conversation history", e);
        }
    }

    // Opens a file chooser for text or PDF files
    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"text/plain", "application/pdf"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(Intent.createChooser(intent, "Select File"), FILE_SELECT_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if(fileUri != null) {
                try {
                    String fileContent = readFileContent(fileUri);
                    ChatMessage fileMessage = new ChatMessage("user", "File:\n" + fileContent);
                    addMessage(fileMessage);
                    appendToConversation("user", fileContent);
                    sendToOpenAIUsingHistory();
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "Error reading file", e);
                }
            }
        }
    }

    // Reads file content from a given Uri
    private String readFileContent(Uri uri) throws IOException {
        StringBuilder builder = new StringBuilder();
        ContentResolver cr = getContentResolver();
        try (InputStream inputStream = cr.openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        }
        return builder.toString();
    }

    // Sends the conversation history to the OpenAI API and handles the response.
    // Replaces the "Thinking..." placeholder with the actual bot reply.
    private void sendToOpenAIUsingHistory() throws IOException, JSONException {
        OkHttpClient client = new OkHttpClient();
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("model", "gpt-3.5-turbo");
        jsonBody.put("messages", conversationHistory);

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + OPENAI_API_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Request failed: " + e.getMessage(), e);
                isWaitingForResponse = false;
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                isWaitingForResponse = false;
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        JSONArray choices = jsonResponse.getJSONArray("choices");
                        if (choices.length() > 0) {
                            JSONObject messageObject = choices.getJSONObject(0).getJSONObject("message");
                            String botReply = messageObject.getString("content");
                            appendToConversation("assistant", botReply);
                            // Remove the "Thinking..." placeholder and add the actual reply
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    chatAdapter.removeLastMessage();
                                    addMessage(new ChatMessage("assistant", botReply));
                                }
                            });
                        } else {
                            Log.e(TAG, "No choices found in response");
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON response: " + e.getMessage(), e);
                    }
                } else {
                    Log.e(TAG, "Response not successful: " + response.code());
                }
            }
        });
    }
}
