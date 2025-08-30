package com.example.mypoject1;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
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
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * ChatbotActivity handles the conversation between the user and the chatbot.
 * <p>
 * This activity manages multiple conversation threads, sends messages to the OpenAI API,
 * loads coach-related context data, processes file attachments, and handles UI updates.
 * It integrates Firebase for user authentication and profile data. The class makes extensive
 * use of JSON to manage conversation history and uses OkHttp for network communication.
 * </p>
 */
public class ChatbotActivity extends AppCompatActivity {

    // OpenAI API configuration and constants
    private static final String OPENAI_API_KEY = BuildConfig.OPENAI_API_KEY;
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
    private ImageView avatarImage;
    private View mainLayout;
    private View header;
    private View subHeader;
    private FloatingActionButton attachFileFab;


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
    private HashMap<String, List<CoachEntry>> keywordIndex = new HashMap<>();

    /**
     * Called when the activity is starting.
     * <p>
     * Sets up the overall UI, initializes conversation state and threads, retrieves necessary
     * user data, loads external coach data, and starts UI animations.
     * </p>
     *
     * @param savedInstanceState Bundle containing the activity's previously saved state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout for the activity using the defined XML file
        setContentView(R.layout.activity_chatbot);

        // Setup user avatar from Firebase profile details
        setupAvatar();
        // Retrieve the user's first name from either the Intent extra or SharedPreferences
        retrieveUserFirstName();
        // Find all view components
        findViews();

        // Configure the RecyclerView for displaying chat messages
        setupChatRecyclerView();
        // Add listener to enable/disable send button based on user input
        setupUserInputListener();
        // Set up the button to start a new conversation thread
        setupNewThreadButton();
        // Start a fresh conversation thread
        startNewThread();
        // Load running coach data (coach entries) from assets for context-based responses
        loadRunningCoachData();
        // Start UI animations for a dynamic experience
        startUIAnimations();
    }

    /**
     * Finds and initializes all view components from the layout and assigns them to variables.
     * This improves code maintainability by centralizing all view lookups.
     */
    private void findViews() {
        userInput = findViewById(R.id.userInput);
        messageInputLayout = findViewById(R.id.messageInputContainer);
        sendMessageFab = findViewById(R.id.sendMessageFab);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        newThreadButton = findViewById(R.id.newThreadButton);
        avatarImage = findViewById(R.id.avatarImage);
        mainLayout = findViewById(R.id.main);
        header = findViewById(R.id.chatbotHeader);
        subHeader = findViewById(R.id.subHeader);
        attachFileFab = findViewById(R.id.attachFileFab);
    }

    /**
     * Sets up the user's avatar by fetching the profile image from Firebase Firestore.
     * <p>
     * If a profile image URI is available from the user's data, it uses Glide to display the image
     * with a circle crop transformation; otherwise, a default avatar is displayed.
     * </p>
     */
    private void setupAvatar() {
        // Get reference to the avatar ImageView
        FirebaseAuth fbAuth = FirebaseAuth.getInstance();
        String uid = fbAuth.getUid();
        FirebaseFirestore store = FirebaseFirestore.getInstance();

        // Retrieve the user's document and load profile image (if available)
        store.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            MyUser user = documentSnapshot.toObject(MyUser.class);
            // If a profile image URI exists, load it using Glide with a circle crop transformation
            if (user != null && user.getProfileImageUri() != null && !user.getProfileImageUri().isEmpty()) {
                Glide.with(ChatbotActivity.this)
                        .load(user.getProfileImageUri())
                        .placeholder(R.drawable.ic_avatar)
                        .transform(new CircleCrop())
                        .into(avatarImage);
            } else {
                // Otherwise, set the default avatar image
                avatarImage.setImageResource(R.drawable.ic_avatar);
            }
        }).addOnFailureListener(e -> avatarImage.setImageResource(R.drawable.ic_avatar)); // On failure, use the default avatar image
    }


    /**
     * Retrieves the user's first name.
     * <p>
     * The first name is retrieved first from the Intent extra. If the Intent extra is missing or empty,
     * it falls back to retrieving the value from SharedPreferences using a default value.
     * </p>
     */
    private void retrieveUserFirstName() {
        // Check if the firstName was passed via the Intent
        String nameExtra = getIntent().getStringExtra("firstName");
        if (nameExtra != null && !nameExtra.isEmpty()) {
            firstName = nameExtra;
        } else {
            // Otherwise, retrieve it from SharedPreferences with a default value
            firstName = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                    .getString("firstName", "there");
        }
    }

    /**
     * Configures the RecyclerView for displaying chat messages.
     * <p>
     * Sets up a LinearLayoutManager and attaches the ChatMessagesAdapter to the RecyclerView.
     * </p>
     */
    private void setupChatRecyclerView() {
        // Create the chat messages adapter with the current message list
        chatAdapter = new ChatMessagesAdapter(messagesList);
        // Set the layout manager and adapter for the RecyclerView
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
    }

    /**
     * Sets up a TextWatcher on the user input field to monitor text changes and enable or disable the send button.
     * <p>
     * The send button is enabled only when there is input text.
     * </p>
     */
    private void setupUserInputListener() {
        // Disable the send button by default
        setSendButtonState(false);
        // Add a text change listener to the user input field
        userInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Enable send button if the text length is greater than zero
                setSendButtonState(s.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    /**
     * Sets up the new conversation thread button.
     * <p>
     * When the new thread button is clicked, a new conversation thread is started and a toast
     * message is shown to the user.
     * </p>
     */
    private void setupNewThreadButton() {
        newThreadButton.setOnClickListener(v -> {
            startNewThread();
            Toast.makeText(ChatbotActivity.this, "Started new conversation", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Starts UI animations on critical layout elements to improve the user experience.
     * <p>
     * Loads predefined animations from resources and applies them to the main layout,
     * header, sub-header, and key interactive UI components.
     * </p>
     */
    private void startUIAnimations() {
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation zoomIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);

        mainLayout.startAnimation(fadeIn);

        if (header != null) {
            header.startAnimation(zoomIn);
        }

        if (subHeader != null) {
            subHeader.startAnimation(slideIn);
        }

        if (attachFileFab != null) {
            attachFileFab.startAnimation(slideIn);
        }

        sendMessageFab.startAnimation(slideIn);
    }


    /**
     * Starts a new conversation thread.
     * <p>
     * Generates a new unique thread ID, resets the conversation history and flags,
     * appends the default system prompt, and clears any existing messages from the UI.
     * </p>
     */
    private void startNewThread() {
        // Generate a new unique thread ID
        currentThreadId = UUID.randomUUID().toString();
        // Initialize new conversation history for the thread
        conversationHistory = new JSONArray();
        threadsHistory.put(currentThreadId, conversationHistory);
        // Reset flag to send a personalized greeting for the first response
        isFirstResponse = true;
        // Append the system prompt to set up conversation context
        addSystemPrompt();
        // Clear all current messages and update the UI
        messagesList.clear();
        runOnUiThread(() -> chatAdapter.notifyDataSetChanged());
    }

    /**
     * Loads and parses JSON files containing running coach data from the asset folder.
     * <p>
     * Iterates through directories and files in "RunningCoachData", reads their content,
     * parses JSON into coach entries, and indexes keywords for efficient lookup when providing context.
     * </p>
     */
    private void loadRunningCoachData() {
        try {
            // Get the asset manager and list directories inside RunningCoachData
            AssetManager assetManager = getAssets();
            String[] directories = assetManager.list("RunningCoachData");
            if (directories != null) {
                // Iterate through each directory
                for (String directory : directories) {
                    String[] files = assetManager.list("RunningCoachData/" + directory);
                    if (files != null) {
                        // Iterate through each file in the directory
                        for (String file : files) {
                            // Open and read file content
                            InputStream inputStream = assetManager.open("RunningCoachData/" + directory + "/" + file);
                            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                            StringBuilder fileContent = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                fileContent.append(line).append("\n");
                            }
                            reader.close();

                            try {
                                // Parse file content as JSON array
                                JSONArray jsonArray = new JSONArray(fileContent.toString());
                                // Iterate through all JSON objects (coach entries)
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject obj = jsonArray.getJSONObject(i);
                                    CoachEntry entry = new CoachEntry();
                                    // Extract prompt and completion text
                                    entry.prompt = obj.optString("prompt");
                                    entry.completion = obj.optString("completion");
                                    // Combine both fields to create a comprehensive text sample
                                    entry.combined = "Prompt: " + entry.prompt + "\nAnswer: " + entry.completion;
                                    coachEntries.add(entry);

                                    // Tokenize and index keywords for quick searching later
                                    String[] words = (entry.prompt + " " + entry.completion).toLowerCase().split("\\W+");
                                    for (String word : words) {
                                        if (word.length() < 3) continue; // Skip short/common words
                                        keywordIndex.computeIfAbsent(word, k -> new ArrayList<>()).add(entry);
                                    }
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
     * Selects relevant context from the loaded coach data based on the user's query.
     * <p>
     * Analyzes the user query by breaking it into words, scores coach entries based on keyword frequency,
     * and then returns a summary constructed from the top three matching entries.
     * </p>
     *
     * @param userQuery The input query provided by the user.
     * @return A summary string containing contextual information from matching coach entries.
     */
    private String getRelevantContext(String userQuery) {
        // Split the user query into individual lowercase words
        String[] queryWords = userQuery.toLowerCase().split("\\W+");
        HashMap<CoachEntry, Integer> entryScores = new HashMap<>();

        // Score each coach entry based on keyword matches
        for (String word : queryWords) {
            List<CoachEntry> entries = keywordIndex.get(word);
            if (entries != null) {
                for (CoachEntry entry : entries) {
                    entryScores.put(entry, entryScores.getOrDefault(entry, 0) + 1);
                }
            }
        }

        // Sort entries by score (number of keyword hits) in descending order
        List<CoachEntry> topMatches = new ArrayList<>(entryScores.keySet());
        topMatches.sort((a, b) -> entryScores.get(b) - entryScores.get(a));

        // Return a summary from the top 3 matching entries (limit summary to ~100 words each)
        StringBuilder contextBuilder = new StringBuilder();
        int count = 0;
        for (CoachEntry entry : topMatches) {
            String[] words = entry.combined.split("\\s+");
            int limit = Math.min(words.length, 100); // limit to a maximum of 100 words
            StringBuilder shortEntry = new StringBuilder();
            for (int i = 0; i < limit; i++) {
                shortEntry.append(words[i]).append(" ");
            }
            contextBuilder.append(shortEntry.toString().trim()).append("...\n\n");
            if (++count >= 3) break;
        }
        return contextBuilder.toString();
    }

    /**
     * Handles click events for various buttons including back, sending a message, and attaching a file.
     * <p>
     * When a button is clicked, a button press animation is started, then based on the clicked view's ID,
     * the appropriate action is performed.
     * </p>
     *
     * @param view The view that received the click event.
     */
    public void onClick(View view) {
        // Get the clicked view's ID and start a button press animation
        int id = view.getId();
        Animation buttonPress = AnimationUtils.loadAnimation(this, R.anim.button_press);
        view.startAnimation(buttonPress);
        buttonPress.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Navigate based on the view that was clicked
                if (id == R.id.backButton) {
                    // Back button: go to HomeActivity
                    startActivity(new Intent(ChatbotActivity.this, HomeActivity.class));
                } else if (id == R.id.sendMessageFab) {
                    // Send button: execute sendMessage function
                    sendMessage();
                } else if (id == R.id.attachFileFab) {
                    // Attach file button: open file chooser
                    openFileChooser();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) { }
        });
    }

    /**
     * Enables or disables the send button based on user input.
     * <p>
     * When enabled, the button's background tint is set to white, and when disabled, it is set to grey.
     * </p>
     *
     * @param enabled True to enable the button; false to disable.
     */
    private void setSendButtonState(boolean enabled) {
        sendMessageFab.setEnabled(enabled);
        // Change button color depending on enabled/disabled state
        if (enabled) {
            sendMessageFab.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.white));
        } else {
            sendMessageFab.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.grey));
        }
    }

    /**
     * Inflates the activity's menu.
     * <p>
     * The menu items defined in main_menu.xml are added to the toolbar.
     * </p>
     *
     * @param menu The Menu object in which items are placed.
     * @return true to display the menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu resource to create menu items in the toolbar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * Handles selection events for menu items.
     * <p>
     * Depending on the selected menu item, navigates to the corresponding activity or calls logout.
     * </p>
     *
     * @param item The selected menu item.
     * @return A boolean indicating whether the event was consumed.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Determine which menu item was selected and perform the corresponding action
        Intent intent = null;
        if (item.getItemId() == R.id.menu_home) {
            intent = new Intent(this, HomeActivity.class);
        } else if (item.getItemId() == R.id.menu_logout) {
            // Call logout method to clear shared preferences and sign out
            logout();
        } else if (item.getItemId() == R.id.menu_timer) {
            intent = new Intent(this, RunningActivity.class);
        } else if (item.getItemId() == R.id.menu_userdetails) {
            intent = new Intent(this, SettingsActivity.class);
        } else if (item.getItemId() == R.id.menu_ChatBot) {
            intent = new Intent(this, ChatbotActivity.class);
        }
        // Launch the selected activity if applicable
        if (intent != null) {
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Adds a system prompt to the conversation history to initialize the chatbot context.
     * <p>
     * This method creates a JSON object containing detailed instructions for the chatbot
     * and appends it to the conversation history.
     * </p>
     */
    private void addSystemPrompt() {
        try {
            // Create a JSON object for the system message
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content",
                    "You are RunWell Coach, a professional AI coach trained in three core domains: running training, nutritional planning, and general health & recovery.\n\n" +
                            "Your primary task is to assist users as a personal trainer, nutritionist, and wellness guide. You are friendly, clear, supportive, and motivating — but never robotic or vague.\n\n" +
                            "🔍 IMPORTANT: How You Find Information\n" +
                            "Always follow this search order when responding:\n" +
                            "1. Search the uploaded files for answers — especially for data on running, nutrition, and health trends.\n" +
                            "2. If no useful answer is found, fall back to your internal knowledge and best practices.\n" +
                            "3. If still unclear, suggest retrieving updated info online (only when appropriate).\n\n" +
                            "🏃 RUNNING COACH DUTIES:\n" +
                            "• Create custom running plans for all fitness levels.\n" +
                            "• Tailor plans to user goals (5K, 10K, half marathon, weight loss, stamina).\n" +
                            "• Include rest days, warm-ups, cool-downs, and cross-training suggestions.\n" +
                            "• Provide technique advice (cadence, posture, stride, breathing).\n" +
                            "• Help prevent injuries.\n\n" +
                            "🥗 NUTRITIONIST DUTIES:\n" +
                            "• Give meal suggestions based on user goals.\n" +
                            "• Adapt to dietary needs (vegan, gluten-free, etc.).\n" +
                            "• Recommend pre/post-workout meals and hydration strategies.\n\n" +
                            "🩺 HEALTH & RECOVERY DUTIES:\n" +
                            "• Teach foam rolling, HRV, sleep tracking, recovery.\n" +
                            "• Encourage consistent habits and mental wellness.\n" +
                            "• Always prioritize long-term health over short-term gains.\n\n" +
                            "❗ RULES & ETHICS:\n" +
                            "• Always ask clarifying questions if needed.\n" +
                            "• NEVER diagnose medical conditions.\n" +
                            "• Do not recommend unsafe diets or supplements.\n\n" +
                            "You are always helpful, encouraging, evidence-based, and focused on sustainable success."
            );
            // Append the system message to the conversation history
            conversationHistory.put(systemMessage);
        } catch (JSONException e) {
            Log.e(TAG, "Error adding system prompt", e);
        }
    }

    /**
     * Checks the total token count of the conversation history and summarizes older messages if necessary.
     * <p>
     * The method estimates the token count using the length of each message. If the total exceeds the
     * TOKEN_LIMIT, it builds a summary from approximately the latest 30% of the conversation, resets the history,
     * and appends the summary as a new system message.
     * </p>
     */
    private void checkTokenLimitAndSummarize() {
        int totalTokens = 0;

        // Estimate total token count based on each message's content length
        for (int i = 0; i < conversationHistory.length(); i++) {
            try {
                JSONObject msg = conversationHistory.getJSONObject(i);
                String content = msg.optString("content");
                totalTokens += content.length() / 4; // Rough token estimate
            } catch (JSONException e) {
                Log.e(TAG, "Error calculating token count", e);
            }
        }

        // If the estimated tokens exceed the defined limit, summarize the conversation
        if (totalTokens > TOKEN_LIMIT) {
            // Show a toast to inform the user about the summarization
            Toast.makeText(this, "Conversation is getting too long. Summarizing to save space.", Toast.LENGTH_SHORT).show();

            int summaryStart = (int) (conversationHistory.length() * 0.7); // Start from ~70% mark
            StringBuilder summaryBuilder = new StringBuilder();

            // Build a summary using the later part of the conversation
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

            // Reset the conversation history and add a system prompt and the summary
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

            // Optionally, update UI or show a "new thread" button here if needed
            runOnUiThread(() -> {
                // Example: showNewThreadButton.setVisibility(View.VISIBLE);
            });
        }
    }

    /**
     * Estimates the token count for the provided conversation history.
     * <p>
     * Uses a rough estimation by dividing the length of each message's content by 4.
     * </p>
     *
     * @param history A JSONArray containing the conversation history.
     * @return The estimated token count as an integer.
     */
    private int estimateTokenCount(JSONArray history) {
        int total = 0;
        for (int i = 0; i < history.length(); i++) {
            try {
                String content = history.getJSONObject(i).optString("content");
                total += content.length() / 4; // Rough token estimation formula
            } catch (JSONException e) {
                Log.e(TAG, "Token estimation failed", e);
            }
        }
        return total;
    }

    /**
     * Sends the user's message along with context to the chatbot.
     * <p>
     * First, it sanitizes the input, adds the user's message to the conversation history and UI,
     * optionally appends additional relevant context retrieved from coach data, checks token limits,
     * and then triggers the asynchronous API call to OpenAI.
     * </p>
     */
    private void sendMessage() {
        // Get and sanitize the user's input message
        String message = userInput.getText().toString().trim();
        message = sanitizeInput(message);
        if (!message.isEmpty() && !isWaitingForResponse) {
            // Create a ChatMessage object for the user's message, add to UI and conversation history
            ChatMessage chatMessage = new ChatMessage("user", message);
            addMessage(chatMessage);
            appendToConversation("user", message);
            // Clear the input field after sending
            userInput.setText("");

            // Append additional relevant context from coach data, if available
            String relevantContext = getRelevantContext(message);
            if (!relevantContext.isEmpty()) {
                appendToConversation("system", "Relevant info:\n" + relevantContext);
                addMessage(new ChatMessage("assistant", "I found some relevant info from your uploaded data to help guide this answer."));
            }

            // Check and summarize conversation history if token limit is exceeded
            Log.d(TAG, "Token estimation: " + estimateTokenCount(conversationHistory));
            checkTokenLimitAndSummarize();

            // Set flag to indicate awaiting the chatbot's response
            isWaitingForResponse = true;
            // Show a temporary "Thinking..." message in the UI
            ChatMessage thinkingMessage = new ChatMessage("assistant", "Thinking...");
            addMessage(thinkingMessage);
            try {
                // Send the complete conversation history to OpenAI
                sendToOpenAIUsingHistory();
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error sending message to OpenAI", e);
            }
        } else if (isWaitingForResponse) {
            // If still waiting for a response, notify the user to wait
            Toast.makeText(this, "Please wait for the bot to reply.", Toast.LENGTH_SHORT).show();
        } else {
            // Notify the user to enter a message if input is empty
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Adds a chat message to the RecyclerView and scrolls to display the latest entry.
     * <p>
     * This method updates the UI on the main thread by adding the message to the adapter
     * and initiating a smooth scroll.
     * </p>
     *
     * @param message The ChatMessage object representing the message to be added.
     */
    private void addMessage(ChatMessage message) {
        runOnUiThread(() -> {
            // Add the new message to the adapter and scroll to it
            chatAdapter.addMessage(message);
            chatRecyclerView.smoothScrollToPosition(messagesList.size() - 1);
        });
    }

    /**
     * Appends a message to the conversation history.
     * <p>
     * Wraps the message content and its sender role in a JSON object, then adds it to the ongoing conversation.
     * </p>
     *
     * @param role    The sender role (e.g., "user" or "assistant").
     * @param content The content of the message.
     */
    private void appendToConversation(String role, String content) {
        try {
            // Create a JSON object representing the message and append it to conversation history
            JSONObject messageObj = new JSONObject();
            messageObj.put("role", role);
            messageObj.put("content", content);
            conversationHistory.put(messageObj);
        } catch (JSONException e) {
            Log.e(TAG, "Error appending to conversation history", e);
        }
    }

    /**
     * Opens the file chooser for file attachment.
     * <p>
     * Creates an Intent with action OPEN_DOCUMENT and restricts file types to plain text and PDF.
     * If MIME type is not detected, the file is handled as plain text by default.
     * </p>
     */
    private void openFileChooser() {
        // Create an intent to select a file (any type, restricted to specified MIME types)
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"text/plain", "application/pdf"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        // Start the activity for file selection
        startActivityForResult(Intent.createChooser(intent, "Select File"), FILE_SELECT_CODE);
    }

    /**
     * Handles the result from the file chooser activity.
     * <p>
     * Processes the returned file URI, reads its content, sanitizes it, and appends the file
     * content to the conversation history and UI as a user message.
     * </p>
     *
     * @param requestCode The request code for identification.
     * @param resultCode  The result code indicating success or failure.
     * @param data        The returned Intent data containing the file URI.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Check if the result is from the file selector and is successful
        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                try {
                    // Determine the MIME type of the selected file
                    String mimeType = getContentResolver().getType(fileUri);
                    String fileName = fileUri.getLastPathSegment();

                    if (mimeType != null) {
                        if (mimeType.equals("text/plain")) {
                            // For plain text, simply read the file content on the main thread
                            String fileContent = readFileContent(fileUri);
                            fileContent = sanitizeInput(fileContent);
                            ChatMessage fileMessage = new ChatMessage("user", "File (" + fileName + "):\n" + fileContent);
                            addMessage(fileMessage);
                            appendToConversation("user", "File attachment (" + fileName + "):\n" + fileContent);
                            sendToOpenAIUsingHistory();
                        } else {
                            // Unsupported file type: show toast and do not process
                            Toast.makeText(this, "Unsupported file type: " + mimeType, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } else {
                        // If MIME type is not available, default to reading as plain text
                        String fileContent = readFileContent(fileUri);
                        fileContent = sanitizeInput(fileContent);
                        ChatMessage fileMessage = new ChatMessage("user", "File (" + fileName + "):\n" + fileContent);
                        addMessage(fileMessage);
                        appendToConversation("user", "File attachment (" + fileName + "):\n" + fileContent);
                        sendToOpenAIUsingHistory();
                    }
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "Error processing file", e);
                    Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * Reads the complete content of a file from its URI.
     * <p>
     * Uses a BufferedReader to read the file line by line and returns the accumulated content as a String.
     * </p>
     *
     * @param uri The URI of the file to read.
     * @return A String containing the entire content of the file.
     * @throws IOException If an error occurs during file reading.
     */
    private String readFileContent(Uri uri) throws IOException {
        StringBuilder builder = new StringBuilder();
        ContentResolver cr = getContentResolver();
        // Use a BufferedReader to read the file line by line
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
     * Sends the conversation history to OpenAI's API for generating a response.
     * <p>
     * Configures an OkHttpClient with custom timeouts, constructs the JSON payload including
     * the model and conversation history, and makes an asynchronous POST request to the API.
     * On success, it parses the response and updates the conversation history and UI accordingly.
     * </p>
     *
     * @throws IOException   If a network error occurs during the API call.
     * @throws JSONException If an error occurs while building the JSON payload.
     */
    private void sendToOpenAIUsingHistory() throws IOException, JSONException {
        // Configure the OkHttpClient with specified timeouts
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS) // Connection establishment timeout
                .writeTimeout(30, TimeUnit.SECONDS)   // Request write timeout
                .readTimeout(90, TimeUnit.SECONDS)    // Response read timeout
                .callTimeout(120, TimeUnit.SECONDS)   // Overall call timeout
                .build();

        // Construct the JSON payload with the chosen model and conversation history
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("model", "gpt-4o");
        jsonBody.put("messages", conversationHistory);

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + OPENAI_API_KEY)
                .post(body)
                .build();

        // Execute the API call asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Log the error and reset waiting flag on failure
                Log.e(TAG, "Request failed: " + e.getMessage(), e);
                isWaitingForResponse = false;
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                isWaitingForResponse = false;
                if (response.isSuccessful()) {
                    try {
                        // Process the response from OpenAI
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        JSONArray choices = jsonResponse.getJSONArray("choices");
                        if (choices.length() > 0) {
                            // Extract the chatbot's reply from the response
                            JSONObject messageObject = choices.getJSONObject(0).getJSONObject("message");
                            String botReply = messageObject.getString("content");
                            // Prepend a personalized greeting if it's the first response
                            if (isFirstResponse) {
                                botReply = "Hello " + firstName + ", " + botReply;
                                isFirstResponse = false;
                            }
                            // Append the assistant's reply to conversation history and update UI
                            appendToConversation("assistant", botReply);
                            String finalBotReply = botReply;
                            runOnUiThread(() -> {
                                // Remove the "Thinking..." placeholder message and add the final reply
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
     * Sanitizes a given message by stripping potentially unsafe characters.
     * <p>
     * This method helps to prevent JSON formatting issues or security risks by removing characters
     * such as angle brackets, curly braces, or backticks.
     * </p>
     *
     * @param message The original message string.
     * @return A sanitized version of the message.
     */
    private String sanitizeInput(String message) {
        if (message == null) return "";
        // Remove characters that might interfere with JSON or pose security issues
        return message.replaceAll("[<>\\{\\}`]", "");
    }

    /**
     * Logs out the user by clearing stored data and redirecting to the login screen.
     * <p>
     * Clears the SharedPreferences to remove user-specific data, signs out of Firebase, and then navigates back
     * to the MainActivity (login screen) while finishing the current activity.
     * </p>
     */
    private void logout() {
        // Clear stored preferences to remove user-specific data
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut();
        // Navigate back to the login/main activity
        Intent intent = new Intent(ChatbotActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
