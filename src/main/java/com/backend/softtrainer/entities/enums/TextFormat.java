package com.backend.softtrainer.entities.enums;

/**
 * 📝 Text Format Types - Defines how text content should be rendered
 * 
 * Supports rich text rendering, markdown, and interactive elements
 * to enhance the user experience beyond plain text messages.
 */
public enum TextFormat {
    PLAIN_TEXT,          // Simple plain text (default)
    MARKDOWN,            // Markdown formatting support
    HTML,                // Rich HTML content
    FORMATTED_JSON,      // Structured JSON with formatting rules
    INTERACTIVE_TEXT     // Text with embedded interactive elements
} 