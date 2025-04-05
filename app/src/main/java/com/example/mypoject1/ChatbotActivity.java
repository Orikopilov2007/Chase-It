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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

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
    private JSONArray conversationHistory = new JSONArray();     // Conversation history for the API
    private boolean isWaitingForResponse = false;
    private boolean isFirstResponse = true;
    private String firstName = "there";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        ImageView avatarImage = findViewById(R.id.avatarImage);

        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        String uid = fbAuth.getUid();
        FirebaseFirestore store = FirebaseFirestore.getInstance();

        store.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            MyUser user = documentSnapshot.toObject(MyUser.class);
            if (user != null && user.getProfileImageUri() != null && !user.getProfileImageUri().isEmpty()) {
                // Load the user's profile image using Glide
                Glide.with(ChatbotActivity.this)
                        .load(user.getProfileImageUri())
                        .placeholder(R.drawable.ic_avatar) // fallback if image is loading
                        .transform(new CircleCrop())
                        .into(avatarImage);
            } else {
                avatarImage.setImageResource(R.drawable.ic_avatar);
            }
        }).addOnFailureListener(e -> {
            avatarImage.setImageResource(R.drawable.ic_avatar);
        });

        // Retrieve the user's first name from intent extras or SharedPreferences
        String nameExtra = getIntent().getStringExtra("firstName");
        if (nameExtra != null && !nameExtra.isEmpty()) {
            firstName = nameExtra;
        } else {
            firstName = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                    .getString("firstName", "there");
        }

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

        // Update send button state when text changes
        userInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setSendButtonState(s.length() > 0);
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });

        // Add the system prompt to the conversation history
        addSystemPrompt();

        // Apply animations (make sure these XML files exist in res/anim)
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation zoomIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);

        // Animate the main layout
        View mainLayout = findViewById(R.id.main_layout);
        mainLayout.startAnimation(fadeIn);

        // Animate header and sub-header
        View header = findViewById(R.id.chatbotHeader);
        if (header != null) {
            header.startAnimation(zoomIn);
        }
        View subHeader = findViewById(R.id.subHeader);
        if (subHeader != null) {
            subHeader.startAnimation(slideIn);
        }

        // Animate the bottom bar buttons (Attach and Send)
        FloatingActionButton attachFileFab = findViewById(R.id.attachFileFab);
        if (attachFileFab != null) {
            attachFileFab.startAnimation(slideIn);
        }
        sendMessageFab.startAnimation(slideIn);
    }

    // Common onClick handler for back, send, and attach file buttons
    public void handleButtonClick(View view) {
        int id = view.getId();
        Animation buttonPress = AnimationUtils.loadAnimation(this, R.anim.button_press);
        view.startAnimation(buttonPress);
        buttonPress.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) {
                if (id == R.id.backButton) {
                    startActivity(new Intent(ChatbotActivity.this, HomeActivity.class));
                } else if (id == R.id.sendMessageFab) {
                    sendMessage();
                } else if (id == R.id.attachFileFab) {
                    openFileChooser();
                }
            }
            @Override
            public void onAnimationRepeat(Animation animation) { }
        });
    }

    // Method to update the send button's enabled state and tint color
    private void setSendButtonState(boolean enabled) {
        sendMessageFab.setEnabled(enabled);
        if (enabled) {
            sendMessageFab.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, android.R.color.white)
            );
        } else {
            sendMessageFab.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, R.color.grey)
            );
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_home) {
            startActivity(new Intent(this, HomeActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Add an initial system prompt to set the context for the bot
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

    // Sends the user's message and shows a "Thinking..." placeholder while waiting for the bot's response
    private void sendMessage() {
        String message = userInput.getText().toString().trim();
        if (!message.isEmpty() && !isWaitingForResponse) {
            ChatMessage chatMessage = new ChatMessage("user", message);
            addMessage(chatMessage);
            appendToConversation("user", message);
            userInput.setText("");
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

    // Adds a message to the RecyclerView
    private void addMessage(ChatMessage message) {
        runOnUiThread(() -> {
            chatAdapter.addMessage(message);
            chatRecyclerView.smoothScrollToPosition(messagesList.size() - 1);
        });
    }

    // Appends a message to the conversation history JSON array
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
        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
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

    // Reads file content from the given Uri
    private String readFileContent(Uri uri) throws IOException {
        StringBuilder builder = new StringBuilder();
        ContentResolver cr = getContentResolver();
        try (InputStream inputStream = cr.openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        }
        return builder.toString();
    }

    // Sends the conversation history to the OpenAI API and handles the bot's response
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
                            // On the first response, prepend a personalized greeting
                            if (isFirstResponse) {
                                botReply = "Hello " + firstName + ", " + botReply;
                                isFirstResponse = false;
                            }
                            appendToConversation("assistant", botReply);
                            String finalBotReply = botReply;
                            runOnUiThread(() -> {
                                chatAdapter.removeLastMessage();
                                addMessage(new ChatMessage("assistant", finalBotReply));
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
