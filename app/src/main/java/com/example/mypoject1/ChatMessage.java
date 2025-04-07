package com.example.mypoject1;

/**
 * Represents a chat message with a role (sender) and content.
 */
public class ChatMessage {
    private String role;
    private String content;

    /**
     * Constructs a new ChatMessage with the specified role and content.
     */
    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    /**
     * Returns the role of the sender.
     */
    public String getRole() {
        return role;
    }

    /**
     * Returns the content of the message.
     */
    public String getContent() {
        return content;
    }
}
