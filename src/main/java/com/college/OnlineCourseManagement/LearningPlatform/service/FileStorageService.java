package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.entity.Submission;
import com.college.OnlineCourseManagement.LearningPlatform.exception.FileStorageException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileStorageService {

    private final Path storageDirectory;
    private final long maxFileSize;
    private final Set<String> allowedExtensions;
    private final Map<String, String> mimeTypes;

    @Autowired
    public FileStorageService(FileStorageProperties properties) {
        this.storageDirectory = Paths.get(properties.getBasePath()).toAbsolutePath().normalize();
        this.maxFileSize = properties.getMaxFileSize();
        this.allowedExtensions = new HashSet<>(properties.getAllowedExtensions());
        this.mimeTypes = properties.getMimeTypes();
        initStorageDirectory();
    }

    private void initStorageDirectory() {
        try {
            if (!Files.exists(storageDirectory)) {
                Files.createDirectories(storageDirectory);
                log.info("Created file storage directory: {}", storageDirectory);
            }
        } catch (IOException e) {
            throw new FileStorageException("Could not create storage directory: " + storageDirectory, e);
        }
    }

    /**
     * Stores an uploaded file and returns the storage path metadata.
     * Generates a unique filename to prevent conflicts.
     *
     * @param file the MultipartFile to store
     * @return the relative path where the file is stored
     */
    public String storeFile(MultipartFile file) {
        return storeFile(file, maxFileSize, allowedExtensions);
    }

    public String storeFile(MultipartFile file, long requestedMaxFileSize, Set<String> requestedExtensions) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("No file provided");
        }

        // Validate file size
        if (file.getSize() > requestedMaxFileSize) {
            throw new FileStorageException(
                    "File size exceeds the maximum limit of " + formatBytes(requestedMaxFileSize)
            );
        }

        // Validate file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new FileStorageException("File has no name");
        }

        String extension = getFileExtension(originalFilename);
        Set<String> extensions = requestedExtensions == null || requestedExtensions.isEmpty()
                ? allowedExtensions
                : requestedExtensions;
        if (!extensions.contains(extension.toLowerCase())) {
            String allowed = extensions.stream().collect(Collectors.joining(", "));
            throw new FileStorageException(
                    "File type not allowed. Allowed types: " + allowed
            );
        }

        // Validate MIME type if configured
        String mimeType = file.getContentType();
        if (mimeType != null && !mimeTypes.isEmpty()) {
            String expectedMime = mimeTypes.get(extension.toLowerCase());
            if (expectedMime != null && !mimeType.equals(expectedMime)) {
                log.warn("MIME type mismatch: expected {} for extension {}, got {}", expectedMime, extension, mimeType);
            }
        }

        // Generate unique filename: timestamp_randomoriginal
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomPart = UUID.randomUUID().toString().substring(0, 8);
        String baseName = Pattern.matches(".*\\.[a-zA-Z0-9]+$", originalFilename)
                ? originalFilename.substring(0, originalFilename.lastIndexOf('.'))
                : originalFilename;
        String safeBaseName = baseName.replaceAll("[^a-zA-Z0-9]", "_");
        String uniqueFilename = timestamp + "_" + randomPart + "_" + safeBaseName + "." + extension;

        Path targetLocation = storageDirectory.resolve(uniqueFilename);

        try {
            file.transferTo(targetLocation);
            log.info("File stored at: {}", targetLocation);
            return uniqueFilename;
        } catch (IOException e) {
            throw new FileStorageException("Could not store file: " + originalFilename, e);
        }
    }

    /**
     * Retrieves a stored file as a Spring Resource.
     *
     * @param filename the stored filename (unique name generated by storeFile)
     * @return Resource representing the file
     */
    public Resource getFileAsResource(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new ResourceNotFoundException("File not found");
        }
        Path filePath = storageDirectory.resolve(filename).normalize();
        if (!filePath.startsWith(storageDirectory)) {
            throw new ResourceNotFoundException("File not found");
        }
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new FileStorageException("Could not load file: " + filename, e);
        }
    }

    /**
     * Gets the full download URL for a file.
     *
     * @param filename the stored filename
     * @return ResponseEntity with the file resource
     */
    public ResponseEntity<Resource> downloadFile(String filename) {
        return serveFile(filename, true, filename);
    }

    public ResponseEntity<Resource> serveFile(String filename, boolean download, String displayName) {
        Resource resource = getFileAsResource(filename);

        MediaType contentType = guessContentType(filename);

        try {
            return ResponseEntity.ok()
                    .contentType(contentType != null
                            ? contentType
                            : MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            (download ? "attachment" : "inline") + "; filename=\""
                                    + (displayName == null || displayName.isBlank() ? filename : displayName) + "\"")
                    .body(resource);

        } catch (Exception e) {
            throw new FileStorageException(
                    "Could not download file: " + filename, e);
        }
    }

    /**
     * Checks if a student owns a specific submission file.
     *
     * @param submission the submission to check
     * @param studentId  the student's ID
     * @return true if the student owns this submission
     */
    public boolean ownsFile(Submission submission, Long studentId) {
        return submission.getStudent().getId().equals(studentId);
    }

    /**
     * Gets the file extension from a filename.
     *
     * @param filename the filename
     * @return the file extension (without the dot), or empty string if none
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1024 * 1024 * 1024) {
            return String.format("%.2f GB", (double) bytes / (1024 * 1024 * 1024));
        } else if (bytes >= 1024 * 1024) {
            return String.format("%.2f MB", (double) bytes / (1024 * 1024));
        } else if (bytes >= 1024) {
            return String.format("%.2f KB", (double) bytes / 1024);
        } else {
            return bytes + " bytes";
        }
    }

    private org.springframework.http.MediaType guessContentType(String filename) {
        if (filename == null || filename.isEmpty()) {
            return org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
        }
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return Optional.ofNullable(mimeTypes.get(extension))
                .map(org.springframework.http.MediaType::valueOf)
                .orElseGet(() -> {
                    switch (extension) {
                        case "pdf":
                            return org.springframework.http.MediaType.APPLICATION_PDF;
                        case "jpg":
                        case "jpeg":
                            return org.springframework.http.MediaType.IMAGE_JPEG;
                        case "png":
                            return org.springframework.http.MediaType.IMAGE_PNG;
                        case "gif":
                            return org.springframework.http.MediaType.IMAGE_GIF;
                        case "txt":
                            return org.springframework.http.MediaType.TEXT_PLAIN;
                        default:
                            return org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
                    }
                });
    }
}