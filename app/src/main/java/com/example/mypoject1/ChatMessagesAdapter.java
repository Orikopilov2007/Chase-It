package com.example.mypoject1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * RecyclerView Adapter to display chat messages between user and bot.
 * Differentiates between user messages and bot messages by inflating different layouts.
 */
public class ChatMessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // Constants to differentiate view types.
    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_BOT = 1;

    // List of chat messages to display.
    private List<ChatMessage> messages;

    /**
     * Constructor to initialize the adapter with a list of messages.
     *
     * @param messages List of ChatMessage objects.
     */
    public ChatMessagesAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    /**
     * Determines the view type for a given position in the list.
     * User messages use VIEW_TYPE_USER and others use VIEW_TYPE_BOT.
     *
     * @param position The position of the message in the list.
     * @return Integer representing the view type.
     */
    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        if ("user".equalsIgnoreCase(message.getRole())) {
            return VIEW_TYPE_USER;
        } else {
            return VIEW_TYPE_BOT;
        }
    }

    /**
     * Creates appropriate ViewHolder based on view type.
     *
     * @param parent   The parent ViewGroup.
     * @param viewType The type of view to create.
     * @return A new ViewHolder instance.
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_USER) {
            View view = inflater.inflate(R.layout.item_user_message, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_bot_message, parent, false);
            return new BotMessageViewHolder(view);
        }
    }

    /**
     * Binds the data to the ViewHolder based on the message's role.
     *
     * @param holder   The ViewHolder to bind data to.
     * @param position The position of the message in the list.
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder.getItemViewType() == VIEW_TYPE_USER) {
            ((UserMessageViewHolder) holder).bind(message);
        } else {
            ((BotMessageViewHolder) holder).bind(message);
        }
    }

    /**
     * Returns the total number of messages.
     *
     * @return The size of the messages list.
     */
    @Override
    public int getItemCount() {
        return messages.size();
    }

    /**
     * Adds a new message to the list and notifies the adapter.
     *
     * @param message The ChatMessage to add.
     */
    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    /**
     * Removes the last message from the list and notifies the adapter.
     */
    public void removeLastMessage() {
        if (!messages.isEmpty()) {
            int lastIndex = messages.size() - 1;
            messages.remove(lastIndex);
            notifyItemRemoved(lastIndex);
        }
    }

    /**
     * ViewHolder for user messages.
     */
    class UserMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageText;

        /**
         * Initializes the UserMessageViewHolder by finding the relevant TextView.
         *
         * @param itemView The view for the user message.
         */
        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
        }

        /**
         * Binds a ChatMessage to the view.
         *
         * @param message The ChatMessage object containing the user message.
         */
        public void bind(ChatMessage message) {
            messageText.setText(message.getContent());
        }
    }

    /**
     * ViewHolder for bot messages.
     */
    class BotMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageText;

        /**
         * Initializes the BotMessageViewHolder by finding the relevant TextView.
         *
         * @param itemView The view for the bot message.
         */
        public BotMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
        }

        /**
         * Binds a ChatMessage to the view.
         *
         * @param message The ChatMessage object containing the bot message.
         */
        public void bind(ChatMessage message) {
            messageText.setText(message.getContent());
        }
    }
}
