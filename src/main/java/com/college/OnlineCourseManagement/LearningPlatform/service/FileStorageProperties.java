package com.college.OnlineCourseManagement.LearningPlatform.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.file-storage")
public class FileStorageProperties {

    private String basePath = "uploaded-files";
    private long maxFileSize = 10 * 1024 * 1024; // 10 MB default
    private Map<String, String> mimeTypes = new HashMap<>();
    private List<String> allowedExtensions = Arrays.asList(
            "pdf", "doc", "docx", "ppt", "pptx", "txt",
            "jpg", "jpeg", "png", "gif", "mp4", "mov", "avi"
    );

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public Map<String, String> getMimeTypes() {
        return mimeTypes;
    }

    public void setMimeTypes(Map<String, String> mimeTypes) {
        this.mimeTypes = mimeTypes;
    }

    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(List<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }
}