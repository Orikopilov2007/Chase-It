package com.example.mypoject1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * ChatMessagesAdapter
 * <p>
 * A RecyclerView adapter responsible for displaying chat messages exchanged between the user and the bot.
 * It differentiates between user messages and bot messages by inflating different layouts for each type.
 * This adapter handles creating ViewHolders, binding chat data to views, and providing helper methods to
 * add or remove messages dynamically.
 * </p>
 *
 * <p>
 * Key Functionalities:
 * <ul>
 *   <li>Determines the view type based on the message role (user vs. bot).</li>
 *   <li>Inflates the appropriate layout for each message type.</li>
 *   <li>Binds the chat message content to the corresponding TextView.</li>
 *   <li>Provides methods to add a new message or remove the last message in the list.</li>
 * </ul>
 * </p>
 */
public class ChatMessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // Constants to differentiate view types for user and bot messages.
    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_BOT = 1;

    // List containing all the chat messages to be displayed.
    private List<ChatMessage> messages;

    /**
     * Constructor to initialize the adapter with an initial list of chat messages.
     *
     * @param messages List of ChatMessage objects representing the conversation.
     */
    public ChatMessagesAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    /**
     * getItemViewType
     * <p>
     * Determines which view type should be used for a message at a specific position.
     * It returns VIEW_TYPE_USER if the message role is "user", otherwise it returns VIEW_TYPE_BOT.
     * </p>
     *
     * @param position The position index of the message in the list.
     * @return An integer representing the view type: 0 for user messages, 1 for bot messages.
     */
    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        // Check if the message role is "user" (case-insensitive)
        if ("user".equalsIgnoreCase(message.getRole())) {
            return VIEW_TYPE_USER;
        } else {
            return VIEW_TYPE_BOT;
        }
    }

    /**
     * onCreateViewHolder
     * <p>
     * Called when a new ViewHolder is needed. This method inflates the appropriate layout based on the view type.
     * For user messages, the layout item_user_message is used; for bot messages, item_bot_message is used.
     * </p>
     *
     * @param parent   The parent ViewGroup into which the new view will be added.
     * @param viewType The integer representing the type of view to create.
     * @return A new instance of RecyclerView.ViewHolder for the given view type.
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Obtain a LayoutInflater object from the parent context
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        // Inflate and return a UserMessageViewHolder or BotMessageViewHolder based on the view type
        if (viewType == VIEW_TYPE_USER) {
            View view = inflater.inflate(R.layout.item_user_message, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_bot_message, parent, false);
            return new BotMessageViewHolder(view);
        }
    }

    /**
     * onBindViewHolder
     * <p>
     * Binds the chat message at a given position to the appropriate ViewHolder.
     * Depending on whether the message is from the user or the bot, the corresponding bind() method is called.
     * </p>
     *
     * @param holder   The RecyclerView.ViewHolder which should be updated.
     * @param position The position index of the message in the list.
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // Retrieve the chat message for the current position
        ChatMessage message = messages.get(position);
        // Bind the message to the correct view holder based on the view type
        if (holder.getItemViewType() == VIEW_TYPE_USER) {
            ((UserMessageViewHolder) holder).bind(message);
        } else {
            ((BotMessageViewHolder) holder).bind(message);
        }
    }

    /**
     * getItemCount
     * <p>
     * Returns the total number of messages in the adapter.
     * </p>
     *
     * @return The size of the messages list.
     */
    @Override
    public int getItemCount() {
        return messages.size();
    }

    /**
     * addMessage
     * <p>
     * Adds a new chat message to the list and notifies the adapter that an item has been inserted.
     * This method enables dynamic appending of messages to the RecyclerView.
     * </p>
     *
     * @param message The ChatMessage object to be added.
     */
    public void addMessage(ChatMessage message) {
        messages.add(message);
        // Notify that a new item was inserted at the end of the list
        notifyItemInserted(messages.size() - 1);
    }

    /**
     * removeLastMessage
     * <p>
     * Removes the last chat message from the list and notifies the adapter.
     * This method is useful to remove placeholder messages (e.g., a "Thinking..." message).
     * </p>
     */
    public void removeLastMessage() {
        if (!messages.isEmpty()) {
            int lastIndex = messages.size() - 1;
            messages.remove(lastIndex);
            // Notify that the last item has been removed
            notifyItemRemoved(lastIndex);
        }
    }

    /**
     * UserMessageViewHolder
     * <p>
     * A ViewHolder subclass for displaying user messages.
     * It contains a TextView to display the user's message content.
     * </p>
     */
    class UserMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageText;

        /**
         * Constructor for UserMessageViewHolder.
         * Finds and assigns the TextView for user messages.
         *
         * @param itemView The view corresponding to a user message layout.
         */
        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            // Locate the TextView that will display the message content
            messageText = itemView.findViewById(R.id.messageText);
        }

        /**
         * bind
         * <p>
         * Binds a ChatMessage object to the UI by setting the text of the messageText TextView.
         * </p>
         *
         * @param message The ChatMessage object containing the user message.
         */
        public void bind(ChatMessage message) {
            // Display the content of the chat message in the TextView
            messageText.setText(message.getContent());
        }
    }

    /**
     * BotMessageViewHolder
     * <p>
     * A ViewHolder subclass for displaying bot messages.
     * It contains a TextView to display the bot's message content.
     * </p>
     */
    class BotMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageText;

        /**
         * Constructor for BotMessageViewHolder.
         * Finds and assigns the TextView for bot messages.
         *
         * @param itemView The view corresponding to a bot message layout.
         */
        public BotMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            // Locate the TextView that will display the bot message content
            messageText = itemView.findViewById(R.id.messageText);
        }

        /**
         * bind
         * <p>
         * Binds a ChatMessage object to the UI by setting the text of the messageText TextView.
         * </p>
         *
         * @param message The ChatMessage object containing the bot message.
         */
        public void bind(ChatMessage message) {
            // Display the content of the chat message in the TextView
            messageText.setText(message.getContent());
        }
    }
}
