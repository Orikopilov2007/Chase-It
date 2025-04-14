package com.example.mypoject1;

import android.net.Uri;

/**
 * ChatMessage
 * <p>
 * A model class representing a single message in a chat conversation.
 * Each message includes information about who sent it (role), the content
 * of the message, the type of the message (text or file), and an optional file URI
 * if the message is a file.
 * </p>
 *
 * <p>
 * This class supports both plain text messages and file messages, using overloaded constructors
 * to handle each case.
 * </p>
 */
public class ChatMessage {

    /**
     * Enum to represent the type of message.
     * TEXT - plain message
     * FILE - message containing a file
     */
    public enum MessageType {
        TEXT,
        FILE
    }

    // The sender of the message, e.g., "user" or "bot"
    private String role;

    // The textual content of the message, or description if it's a file
    private String content;

    // The type of the message (text or file)
    private MessageType type;

    // URI pointing to the file, applicable only for file messages
    private Uri fileUri;

    /**
     * Constructs a new ChatMessage representing a text message.
     *
     * @param role    The sender of the message ("user" or "bot").
     * @param content The textual content of the message.
     */
    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.type = MessageType.TEXT;
        this.fileUri = null; // No file URI for text messages
    }

    /**
     * Constructs a new ChatMessage representing a file message.
     *
     * @param role     The sender of the message ("user" or "bot").
     * @param content  The description or label for the file.
     * @param fileUri  URI of the file associated with the message.
     */
    public ChatMessage(String role, String content, Uri fileUri) {
        this.role = role;
        this.content = content;
        this.type = MessageType.FILE;
        this.fileUri = fileUri;
    }

    /**
     * Returns the role (sender) of the message.
     *
     * @return A string indicating who sent the message.
     */
    public String getRole() {
        return role;
    }

    /**
     * Returns the content of the message.
     *
     * @return The message text or description.
     */
    public String getContent() {
        return content;
    }

    /**
     * Returns the type of the message (TEXT or FILE).
     *
     * @return MessageType enum value.
     */
    public MessageType getType() {
        return type;
    }

    /**
     * Returns the URI of the file if the message is of type FILE.
     *
     * @return Uri of the file, or null if not a file message.
     */
    public Uri getFileUri() {
        return fileUri;
    }
}
