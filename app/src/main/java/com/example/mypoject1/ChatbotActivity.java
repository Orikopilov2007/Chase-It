package com.example.mypoject1;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.AssetManager;
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
import com.google.android.material.button.MaterialButton;
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
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * ChatbotActivity handles the conversation between the user and the chatbot.
 * It manages conversation threads, sending messages to OpenAI's API, and loading coach data.
 */
public class ChatbotActivity extends AppCompatActivity {

    // OpenAI API configuration and constants
    private static final String OPENAI_API_KEY = "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";
    private static final String TAG = "chatbot";
    private static final int FILE_SELECT_CODE = 100;
    private static final int TOKEN_LIMIT = 16000;

    // UI elements
    private TextInputEditText userInput;
    private TextInputLayout messageInputLayout;
    private FloatingActionButton sendMessageFab;
    private RecyclerView chatRecyclerView;
    private ChatMessagesAdapter chatAdapter;
    private List<ChatMessage> messagesList = new ArrayList<>();
    private MaterialButton newThreadButton;

    // Conversation thread management
    private HashMap<String, JSONArray> threadsHistory = new HashMap<>();
    private String currentThreadId;
    private JSONArray conversationHistory;

    // State management flags
    private boolean isWaitingForResponse = false;
    private boolean isFirstResponse = true;
    private String firstName = "there";

    // Structure to hold each dataset entry from RunningCoachData
    private static class CoachEntry {
        String prompt;
        String completion;
        String combined;
    }

    // List to store parsed coach data entries from assets
    private List<CoachEntry> coachEntries = new ArrayList<>();

    /**
     * Called when the activity is starting. Sets up UI, conversation thread, and loads assets.
     *
     * @param savedInstanceState Saved instance state bundle.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        setupAvatar();
        retrieveUserFirstName();
        initViews();
        setupChatRecyclerView();
        setupUserInputListener();
        setupNewThreadButton();
        startNewThread();
        loadRunningCoachData();

        // Start animations for various UI elements
        startUIAnimations();
    }

    /**
     * Sets up the user's avatar from Firebase.
     */
    private void setupAvatar() {
        ImageView avatarImage = findViewById(R.id.avatarImage);
        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        String uid = fbAuth.getUid();
        FirebaseFirestore store = FirebaseFirestore.getInstance();
        store.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            MyUser user = documentSnapshot.toObject(MyUser.class);
            if (user != null && user.getProfileImageUri() != null && !user.getProfileImageUri().isEmpty()) {
                Glide.with(ChatbotActivity.this)
                        .load(user.getProfileImageUri())
                        .placeholder(R.drawable.ic_avatar)
                        .transform(new CircleCrop())
                        .into(avatarImage);
            } else {
                avatarImage.setImageResource(R.drawable.ic_avatar);
            }
        }).addOnFailureListener(e -> avatarImage.setImageResource(R.drawable.ic_avatar));
    }

    /**
     * Retrieves the user's first name from the intent or shared preferences.
     */
    private void retrieveUserFirstName() {
        String nameExtra = getIntent().getStringExtra("firstName");
        if (nameExtra != null && !nameExtra.isEmpty()) {
            firstName = nameExtra;
        } else {
            firstName = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                    .getString("firstName", "there");
        }
    }

    /**
     * Initializes UI view elements.
     */
    private void initViews() {
        userInput = findViewById(R.id.userInput);
        messageInputLayout = findViewById(R.id.messageInputContainer);
        sendMessageFab = findViewById(R.id.sendMessageFab);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        newThreadButton = findViewById(R.id.newThreadButton);
    }

    /**
     * Sets up the RecyclerView for chat messages.
     */
    private void setupChatRecyclerView() {
        chatAdapter = new ChatMessagesAdapter(messagesList);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
    }

    /**
     * Sets up a TextWatcher to enable/disable the send button based on user input.
     */
    private void setupUserInputListener() {
        setSendButtonState(false);
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
    }

    /**
     * Sets up the new conversation thread button.
     */
    private void setupNewThreadButton() {
        newThreadButton.setOnClickListener(v -> {
            startNewThread();
            Toast.makeText(ChatbotActivity.this, "Started new conversation", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Starts UI animations for the main layout and header elements.
     */
    private void startUIAnimations() {
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation zoomIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);

        View mainLayout = findViewById(R.id.main_layout);
        mainLayout.startAnimation(fadeIn);
        View header = findViewById(R.id.chatbotHeader);
        if (header != null) {
            header.startAnimation(zoomIn);
        }
        View subHeader = findViewById(R.id.subHeader);
        if (subHeader != null) {
            subHeader.startAnimation(slideIn);
        }
        FloatingActionButton attachFileFab = findViewById(R.id.attachFileFab);
        if (attachFileFab != null) {
            attachFileFab.startAnimation(slideIn);
        }
        sendMessageFab.startAnimation(slideIn);
    }

    /**
     * Starts a new conversation thread by resetting conversation history and updating UI.
     */
    private void startNewThread() {
        // Generate a new unique thread ID and reset conversation history
        currentThreadId = UUID.randomUUID().toString();
        conversationHistory = new JSONArray();
        threadsHistory.put(currentThreadId, conversationHistory);
        isFirstResponse = true;
        addSystemPrompt();
        messagesList.clear();
        runOnUiThread(() -> chatAdapter.notifyDataSetChanged());
    }

    /**
     * Loads and parses JSON files from the RunningCoachData asset folder into coachEntries.
     */
    private void loadRunningCoachData() {
        try {
            AssetManager assetManager = getAssets();
            String[] directories = assetManager.list("RunningCoachData");
            if (directories != null) {
                for (String directory : directories) {
                    String[] files = assetManager.list("RunningCoachData/" + directory);
                    if (files != null) {
                        for (String file : files) {
                            InputStream inputStream = assetManager.open("RunningCoachData/" + directory + "/" + file);
                            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                            StringBuilder fileContent = new StringBuilder();
                            String line;
                            // Read the file line by line
                            while ((line = reader.readLine()) != null) {
                                fileContent.append(line).append("\n");
                            }
                            reader.close();
                            try {
                                // Parse the file content as a JSON array
                                JSONArray jsonArray = new JSONArray(fileContent.toString());
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject obj = jsonArray.getJSONObject(i);
                                    CoachEntry entry = new CoachEntry();
                                    entry.prompt = obj.optString("prompt");
                                    entry.completion = obj.optString("completion");
                                    entry.combined = "Prompt: " + entry.prompt + "\nAnswer: " + entry.completion;
                                    coachEntries.add(entry);
                                }
                            } catch (JSONException je) {
                                Log.e(TAG, "Error parsing JSON in file: " + file, je);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading RunningCoachData from assets", e);
        }
    }

    /**
     * Selects relevant context from the loaded coach entries based on the user's query.
     *
     * @param userQuery The input provided by the user.
     * @return A string containing relevant context information.
     */
    private String getRelevantContext(String userQuery) {
        StringBuilder relevantContext = new StringBuilder();
        String[] queryWords = userQuery.toLowerCase().split("\\s+");
        // Evaluate each coach entry for keyword matches
        for (CoachEntry entry : coachEntries) {
            int matchScore = 0;
            String combinedLower = entry.combined.toLowerCase();
            for (String word : queryWords) {
                if (combinedLower.contains(word)) {
                    matchScore++;
                }
            }
            // Append if a minimum match threshold is reached
            if (matchScore >= 2) {
                relevantContext.append(entry.combined).append("\n\n");
            }
        }
        return relevantContext.toString();
    }

    /**
     * Handles click events for various buttons (back, send message, attach file).
     *
     * @param view The view that was clicked.
     */
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

    /**
     * Enables or disables the send button based on user input.
     *
     * @param enabled True to enable the button; false to disable.
     */
    private void setSendButtonState(boolean enabled) {
        sendMessageFab.setEnabled(enabled);
        if (enabled) {
            sendMessageFab.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.white));
        } else {
            sendMessageFab.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.grey));
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
        Intent intent = null;
        if (item.getItemId() == R.id.menu_home) {
            intent = new Intent(this, HomeActivity.class);
        } else if (item.getItemId() == R.id.menu_logout) {
            intent = new Intent(this, MainActivity.class);
            finish();
        } else if (item.getItemId() == R.id.menu_timer) {
            intent = new Intent(this, TimerActivity.class);
        } else if (item.getItemId() == R.id.menu_userdetails) {
            intent = new Intent(this, UserDetailsActivity.class);
        } else if(item.getItemId() == R.id.menu_ChatBot){
            intent = new Intent(this, ChatbotActivity.class);
        }
        startActivity(intent);
        return super.onOptionsItemSelected(item);
    }

    /**
     * Adds a system prompt to the conversation history for initializing context.
     */
    private void addSystemPrompt() {
        try {
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "You are an expert running coach and nutrition specialist with in-depth knowledge of training methodologies, recovery strategies, and nutritional science. Provide detailed, evidence-based advice on running techniques, endurance and strength training, injury prevention, and recovery. For all questions, ensure your responses are comprehensive and actionable, and whenever possible, seamlessly integrate relevant insights on running, exercise, and nutrition.");
            conversationHistory.put(systemMessage);
        } catch (JSONException e) {
            Log.e(TAG, "Error adding system prompt", e);
        }
    }


    /**
     * Checks the total token count of the conversation and summarizes older messages if necessary.
     */
    private void checkTokenLimitAndSummarize() {
        int totalTokens = 0;
        // Estimate token count based on content length divided by 4 (approximation)
        for (int i = 0; i < conversationHistory.length(); i++) {
            try {
                JSONObject msg = conversationHistory.getJSONObject(i);
                String content = msg.optString("content");
                totalTokens += content.length() / 4;
            } catch (JSONException e) {
                Log.e(TAG, "Error calculating token count", e);
            }
        }
        if (totalTokens > TOKEN_LIMIT) {
            // Summarize the latter part of the conversation (e.g., last 30%)
            int summaryStart = (int) (conversationHistory.length() * 0.7);
            StringBuilder summaryBuilder = new StringBuilder();
            for (int i = summaryStart; i < conversationHistory.length(); i++) {
                try {
                    JSONObject msg = conversationHistory.getJSONObject(i);
                    summaryBuilder.append(msg.optString("role"))
                            .append(": ")
                            .append(msg.optString("content"))
                            .append("\n");
                } catch (JSONException e) {
                    Log.e(TAG, "Error summarizing conversation", e);
                }
            }
            String summary = "Summary of previous conversation:\n" + summaryBuilder.toString();
            // Reset conversation history with a system prompt and summary message
            conversationHistory = new JSONArray();
            addSystemPrompt();
            try {
                JSONObject summaryMsg = new JSONObject();
                summaryMsg.put("role", "system");
                summaryMsg.put("content", summary);
                conversationHistory.put(summaryMsg);
            } catch (JSONException e) {
                Log.e(TAG, "Error adding summary message", e);
            }
            // Notify user via chat UI and make the New Conversation button visible
            runOnUiThread(() -> {
                addMessage(new ChatMessage("assistant", "The conversation is getting very long. Consider starting a new conversation for best performance."));
                newThreadButton.setVisibility(View.VISIBLE);
            });
        }
    }

    /**
     * Sends the user's message along with dynamically selected context to the chatbot.
     */
    private void sendMessage() {
        // Get and sanitize user input
        String message = userInput.getText().toString().trim();
        message = sanitizeMessage(message);
        if (!message.isEmpty() && !isWaitingForResponse) {
            // Append user message to chat and conversation history
            ChatMessage chatMessage = new ChatMessage("user", message);
            addMessage(chatMessage);
            appendToConversation("user", message);
            userInput.setText("");

            // Append any relevant context from coach data
            String relevantContext = getRelevantContext(message);
            if (!relevantContext.isEmpty()) {
                appendToConversation("system", "Relevant info:\n" + relevantContext);
            }
            // Check token limit and summarize if necessary
            checkTokenLimitAndSummarize();

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

    /**
     * Adds a chat message to the RecyclerView and scrolls to the latest message.
     *
     * @param message The ChatMessage object to add.
     */
    private void addMessage(ChatMessage message) {
        runOnUiThread(() -> {
            chatAdapter.addMessage(message);
            chatRecyclerView.smoothScrollToPosition(messagesList.size() - 1);
        });
    }

    /**
     * Appends a message to the current thread's conversation history.
     *
     * @param role    The role of the message sender (e.g., "user" or "assistant").
     * @param content The message content.
     */
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

    /**
     * Opens the file chooser to allow the user to attach a file.
     */
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
        // Handle file selection result
        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                try {
                    String fileContent = readFileContent(fileUri);
                    // Sanitize file content as well
                    fileContent = sanitizeMessage(fileContent);
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

    /**
     * Reads the content of a file given its URI.
     *
     * @param uri The URI of the file.
     * @return The file content as a string.
     * @throws IOException If an error occurs during reading.
     */
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

    /**
     * Sends the conversation history for the current thread to OpenAI's API.
     *
     * @throws IOException   If a network error occurs.
     * @throws JSONException If an error occurs while constructing the JSON payload.
     */
    private void sendToOpenAIUsingHistory() throws IOException, JSONException {
        OkHttpClient client = new OkHttpClient();

        // Prepare JSON payload with model and conversation history
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("model", "gpt-3.5-turbo");
        jsonBody.put("messages", conversationHistory);

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + OPENAI_API_KEY)
                .post(body)
                .build();

        // Execute API call asynchronously
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
                            // Append personalized greeting if it's the first response
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

    /**
     * Sanitizes input messages by removing potentially dangerous characters.
     *
     * This method removes characters such as '<', '>', '{', '}', and backticks.
     * Adjust the regular expression if you wish to allow or disallow other characters.
     *
     * @param message The input message to sanitize.
     * @return A sanitized version of the message.
     */
    private String sanitizeMessage(String message) {
        if (message == null) return "";
        return message.replaceAll("[<>\\{\\}`]", "");
    }
}
