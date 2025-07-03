package com.backend.softtrainer.entities.messages;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 📸 Enhanced Media Message - Advanced media content with interactive features
 * 
 * Supported Media Types:
 * - IMAGE: Photos, diagrams, infographics with annotations
 * - VIDEO: Training videos with interactive elements
 * - AUDIO: Voice messages, pronunciation guides
 * - DOCUMENT: PDFs, presentations with highlighting
 * - INTERACTIVE_MEDIA: 360° images, VR content
 * - CAROUSEL: Multiple media items in sequence
 */
@Entity
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EnhancedMediaMessage extends Message {
    
    @Column(name = "media_type")
    private String mediaType; // IMAGE, VIDEO, AUDIO, DOCUMENT, INTERACTIVE_MEDIA, CAROUSEL
    
    @Column(name = "media_url")
    private String mediaUrl;
    
    @Column(name = "thumbnail_url")
    private String thumbnailUrl;
    
    @Column(name = "alt_text")
    private String altText; // For accessibility
    
    @Column(length = 1000)
    private String caption;
    
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;
    
    @Column(name = "duration_seconds")
    private Integer durationSeconds; // For video/audio
    
    @Column(name = "width_pixels")
    private Integer widthPixels;
    
    @Column(name = "height_pixels")
    private Integer heightPixels;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "media_metadata")
    private JsonNode mediaMetadata;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "interactive_elements")
    private JsonNode interactiveElements; // Hotspots, annotations, clickable areas
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "accessibility_features")
    private JsonNode accessibilityFeatures;
    
    @Column(name = "enable_lazy_loading")
    private Boolean enableLazyLoading = true;
    
    @Column(name = "enable_progressive_loading")
    private Boolean enableProgressiveLoading = true;
    
    @Column(name = "auto_play")
    private Boolean autoPlay = false;
    
    @Column(name = "loop_playback")
    private Boolean loopPlayback = false;
    
    @Column(name = "controls_visible")
    private Boolean controlsVisible = true;
    
    @Column(name = "download_allowed")
    private Boolean downloadAllowed = false;
    
    @Column(name = "fullscreen_enabled")
    private Boolean fullscreenEnabled = true;
    
    @Column(name = "zoom_enabled")
    private Boolean zoomEnabled = true;
    
    @Column(name = "annotation_enabled")
    private Boolean annotationEnabled = false;
    
    @Column(name = "quality_adaptive")
    private Boolean qualityAdaptive = true;
    
    @Column(name = "mobile_optimized")
    private Boolean mobileOptimized = true;
    
    @Column(name = "compression_level")
    private String compressionLevel = "BALANCED"; // LOW, BALANCED, HIGH
    
    @Column(name = "streaming_protocol")
    private String streamingProtocol; // HLS, DASH, Progressive
    
    /**
     * 📸 Check if this is an image
     */
    public boolean isImage() {
        return "IMAGE".equals(mediaType);
    }
    
    /**
     * 📹 Check if this is a video
     */
    public boolean isVideo() {
        return "VIDEO".equals(mediaType);
    }
    
    /**
     * 🔊 Check if this is audio
     */
    public boolean isAudio() {
        return "AUDIO".equals(mediaType);
    }
    
    /**
     * 📄 Check if this is a document
     */
    public boolean isDocument() {
        return "DOCUMENT".equals(mediaType);
    }
    
    /**
     * 🎠 Check if this is a carousel
     */
    public boolean isCarousel() {
        return "CAROUSEL".equals(mediaType);
    }
    
    /**
     * 🎮 Check if this has interactive elements
     */
    public boolean hasInteractiveElements() {
        return interactiveElements != null && !interactiveElements.isEmpty();
    }
    
    /**
     * ♿ Check if accessibility features are enabled
     */
    public boolean hasAccessibilityFeatures() {
        return accessibilityFeatures != null && !accessibilityFeatures.isEmpty();
    }
    
    /**
     * ⚡ Check if lazy loading is enabled
     */
    public boolean isLazyLoadingEnabled() {
        return enableLazyLoading != null && enableLazyLoading;
    }
    
    /**
     * ▶️ Check if auto-play is enabled
     */
    public boolean isAutoPlayEnabled() {
        return autoPlay != null && autoPlay;
    }
    
    /**
     * 📱 Check if optimized for mobile
     */
    public boolean isMobileOptimized() {
        return mobileOptimized != null && mobileOptimized;
    }
    
    /**
     * 🔍 Check if zoom is enabled
     */
    public boolean isZoomEnabled() {
        return zoomEnabled != null && zoomEnabled;
    }
    
    /**
     * 📝 Check if annotation is enabled
     */
    public boolean isAnnotationEnabled() {
        return annotationEnabled != null && annotationEnabled;
    }
    
    /**
     * 📐 Get aspect ratio
     */
    public double getAspectRatio() {
        if (widthPixels != null && heightPixels != null && heightPixels > 0) {
            return (double) widthPixels / heightPixels;
        }
        return 16.0 / 9.0; // Default aspect ratio
    }
    
    /**
     * 📊 Get file size in MB
     */
    public double getFileSizeMB() {
        if (fileSizeBytes != null) {
            return fileSizeBytes / (1024.0 * 1024.0);
        }
        return 0.0;
    }
    
    /**
     * 🏗️ Create a simple image message
     */
    public static EnhancedMediaMessage createImage(String mediaUrl, String altText, String caption) {
        return EnhancedMediaMessage.builder()
            .mediaType("IMAGE")
            .mediaUrl(mediaUrl)
            .altText(altText)
            .caption(caption)
            .enableLazyLoading(true)
            .fullscreenEnabled(true)
            .zoomEnabled(true)
            .downloadAllowed(false)
            .mobileOptimized(true)
            .compressionLevel("BALANCED")
            .build();
    }
    
    /**
     * 📹 Create a training video message
     */
    public static EnhancedMediaMessage createVideo(String mediaUrl, String thumbnailUrl, String caption) {
        return EnhancedMediaMessage.builder()
            .mediaType("VIDEO")
            .mediaUrl(mediaUrl)
            .thumbnailUrl(thumbnailUrl)
            .caption(caption)
            .autoPlay(false)
            .controlsVisible(true)
            .fullscreenEnabled(true)
            .qualityAdaptive(true)
            .enableProgressiveLoading(true)
            .mobileOptimized(true)
            .streamingProtocol("HLS")
            .build();
    }
    
    /**
     * 🔊 Create an audio message
     */
    public static EnhancedMediaMessage createAudio(String mediaUrl, String caption) {
        return EnhancedMediaMessage.builder()
            .mediaType("AUDIO")
            .mediaUrl(mediaUrl)
            .caption(caption)
            .autoPlay(false)
            .controlsVisible(true)
            .downloadAllowed(false)
            .enableProgressiveLoading(true)
            .mobileOptimized(true)
            .build();
    }
    
    /**
     * 📸 Create an interactive image with hotspots
     */
    public static EnhancedMediaMessage createInteractiveImage(String mediaUrl, String altText, String caption) {
        return EnhancedMediaMessage.builder()
            .mediaType("IMAGE")
            .mediaUrl(mediaUrl)
            .altText(altText)
            .caption(caption)
            .annotationEnabled(true)
            .zoomEnabled(true)
            .fullscreenEnabled(true)
            .enableLazyLoading(true)
            .mobileOptimized(true)
            .build();
    }
    
    /**
     * 🎠 Create a media carousel
     */
    public static EnhancedMediaMessage createCarousel(String caption) {
        return EnhancedMediaMessage.builder()
            .mediaType("CAROUSEL")
            .caption(caption)
            .enableLazyLoading(true)
            .fullscreenEnabled(true)
            .zoomEnabled(true)
            .mobileOptimized(true)
            .build();
    }
    
    /**
     * 📄 Create a document viewer
     */
    public static EnhancedMediaMessage createDocument(String mediaUrl, String caption) {
        return EnhancedMediaMessage.builder()
            .mediaType("DOCUMENT")
            .mediaUrl(mediaUrl)
            .caption(caption)
            .annotationEnabled(true)
            .fullscreenEnabled(true)
            .downloadAllowed(true)
            .enableProgressiveLoading(true)
            .mobileOptimized(true)
            .build();
    }
    
    /**
     * 📹 Create an interactive video with annotations
     */
    public static EnhancedMediaMessage createInteractiveVideo(String mediaUrl, String thumbnailUrl, String caption) {
        return EnhancedMediaMessage.builder()
            .mediaType("VIDEO")
            .mediaUrl(mediaUrl)
            .thumbnailUrl(thumbnailUrl)
            .caption(caption)
            .annotationEnabled(true)
            .autoPlay(false)
            .controlsVisible(true)
            .fullscreenEnabled(true)
            .qualityAdaptive(true)
            .enableProgressiveLoading(true)
            .mobileOptimized(true)
            .streamingProtocol("HLS")
            .build();
    }
} 