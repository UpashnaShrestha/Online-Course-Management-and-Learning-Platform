package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Stores user-uploaded profile images on disk.
 *
 * Uploads are untrusted input, so three things are checked before anything is written:
 *
 * <ul>
 *   <li><b>Size</b> -- bounded by Spring's multipart limit, and re-checked here so the rule is
 *       enforced even if that configuration is loosened.</li>
 *   <li><b>Type</b> -- decided by sniffing the file's magic bytes, not by the {@code Content-Type}
 *       the client sent. A browser or a curl command can label anything it likes; the leading
 *       bytes are what the server can actually trust.</li>
 *   <li><b>Name</b> -- the stored filename is a freshly generated UUID plus an extension
 *       derived from the sniffed type. The client's filename is never used, so {@code ../} and
 *       absolute paths cannot escape the upload directory.</li>
 * </ul>
 *
 * Only extensions that browsers cannot execute are ever written, so serving these files back
 * cannot turn into stored XSS.
 */
@Service
@Slf4j
public class ProfileImageStorage {

    /** Magic bytes to image type. Ordered longest-first so signatures are not partially matched. */
    private static final Map<String, String> SIGNATURES = Map.of(
            "image/jpeg", "jpeg",
            "image/png", "png",
            "image/gif", "gif",
            "image/webp", "webp");

    /** Bytes read from the head of the file when sniffing. Enough for every signature above. */
    private static final int SNIFF_LENGTH = 12;

    private final Path rootDirectory;
    private final Path imageDirectory;
    private final String publicPath;
    private final long maxBytes;

    public ProfileImageStorage(
            @Value("${app.upload.dir:./uploads}") String uploadDir,
            @Value("${app.upload.profile-image-subdir:profile-images}") String subdir,
            @Value("${app.upload.public-path:/uploads}") String publicPath,
            @Value("${app.upload.max-image-size:5MB}") DataSize maxSize) {

        this.rootDirectory = Path.of(uploadDir).toAbsolutePath().normalize();
        this.imageDirectory = this.rootDirectory.resolve(subdir).normalize();
        this.publicPath = publicPath.endsWith("/")
                ? publicPath.substring(0, publicPath.length() - 1)
                : publicPath;
        // DataSize so the limit can be written "5MB" in one place and mean the same thing
        // here and in spring.servlet.multipart.max-file-size.
        this.maxBytes = maxSize.toBytes();

        // Refuse to write outside the configured root, whatever the configuration says.
        if (!imageDirectory.startsWith(rootDirectory)) {
            throw new IllegalStateException(
                    "app.upload.profile-image-subdir must stay inside app.upload.dir");
        }
    }

    /**
     * Stores an uploaded image for a user.
     *
     * @param userId       the owner, used to name the file so listings stay grouped
     * @param file         the upload; must be a real image within the size limit
     * @param previousUrl  the URL currently stored for this user, deleted on success
     * @return the public URL to persist on the user record
     */
    public String store(Long userId, MultipartFile file, String previousUrl) {
        validatePresent(file);

        if (file.getSize() > maxBytes) {
            throw new BadRequestException(
                    "Image is too large. The maximum is " + (maxBytes / (1024 * 1024)) + " MB.");
        }

        String contentType = sniffImageType(file);
        String extension = SIGNATURES.get(contentType);

        String filename = "user-" + userId + "-" + UUID.randomUUID() + "." + extension;

        try {
            Files.createDirectories(imageDirectory);

            Path target = imageDirectory.resolve(filename).normalize();
            if (!target.startsWith(imageDirectory)) {
                // Unreachable given a generated UUID, but the invariant is worth asserting.
                throw new BadRequestException("Invalid file name.");
            }

            // Copy rather than move, so a failed request leaves the source stream usable and a
            // partially written file never appears: write to a temp name, then move into place.
            Path temporary = Files.createTempFile(imageDirectory, "upload-", ".tmp");
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, temporary, StandardCopyOption.REPLACE_EXISTING);
            }

            // Belt and braces: re-check the size of what actually landed on disk.
            long written = Files.size(temporary);
            if (written > maxBytes) {
                throw new BadRequestException(
                        "Image is too large. The maximum is " + (maxBytes / (1024 * 1024)) + " MB.");
            }

            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);

            // Only once the new image is safely stored is the old one removed.
            deleteQuietly(previousUrl);

            log.info("Stored profile image for user {} as {}", userId, filename);
            return publicPath + "/" + subdir() + "/" + filename;

        } catch (IOException ex) {
            throw new BadRequestException("Could not save the image. Please try again.");
        }
    }

    /**
     * Removes a previously stored image.
     *
     * Silently ignores anything that is not one of our own files, so an externally hosted URL
     * left over from the old URL-only field is never touched.
     */
    public void deleteQuietly(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return;
        }

        String prefix = publicPath + "/" + subdir() + "/";
        if (!publicUrl.startsWith(prefix)) {
            return;
        }

        String filename = publicUrl.substring(prefix.length());
        if (filename.isBlank() || filename.contains("/") || filename.contains("..")) {
            return;
        }

        try {
            Files.deleteIfExists(imageDirectory.resolve(filename).normalize());
        } catch (IOException ex) {
            // A leftover file is untidy, not broken. Do not fail the request over it.
            log.warn("Could not delete old profile image {}: {}", filename, ex.getMessage());
        }
    }

    private void validatePresent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please choose an image to upload.");
        }
    }

    /**
     * Identifies the image by its leading bytes.
     *
     * @return the detected content type
     * @throws BadRequestException when the content is not a recognised image
     */
    private String sniffImageType(MultipartFile file) {
        byte[] head = new byte[SNIFF_LENGTH];

        try (InputStream in = file.getInputStream()) {
            int read = in.readNBytes(head, 0, head.length);
            if (read < SNIFF_LENGTH) {
                throw new BadRequestException("That file is empty or too small to be an image.");
            }
        } catch (IOException ex) {
            throw new BadRequestException("Could not read the uploaded image.");
        }

        if (isJpeg(head)) return "image/jpeg";
        if (isPng(head)) return "image/png";
        if (isGif(head)) return "image/gif";
        if (isWebp(head)) return "image/webp";

        throw new BadRequestException(
                "That file is not a supported image. Use a JPEG, PNG, GIF or WebP file.");
    }

    private boolean isJpeg(byte[] b) {
        return (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] b) {
        return (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && (b[4] & 0xFF) == 0x0D && (b[5] & 0xFF) == 0x0A
                && (b[6] & 0xFF) == 0x1A && (b[7] & 0xFF) == 0x0A;
    }

    private boolean isGif(byte[] b) {
        return b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8'
                && (b[4] == '7' || b[4] == '9') && b[5] == 'a';
    }

    private boolean isWebp(byte[] b) {
        return b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
    }

    /** The configured sub-directory, used to build public URLs. */
    private String subdir() {
        Path relative = rootDirectory.relativize(imageDirectory);
        return relative.toString().replace('\\', '/');
    }

    /** Exposed for the resource handler so both use the same directory. */
    public Path imageDirectory() {
        return imageDirectory;
    }

    /**
     * The upload root, which is what the public path maps onto.
     *
     * <p>Not the image sub-directory: the handler pattern already contains the sub-directory
     * ("/uploads/**" plus the stored "/profile-images/x.png"), so pointing the location at the
     * sub-directory would make Spring look for "profile-images/profile-images/x.png".
     */
    public Path rootDirectory() {
        return rootDirectory;
    }

    /** The URL prefix uploads are served under. */
    public String publicPath() {
        return publicPath;
    }

    /** Normalises a declared type for comparison; kept for callers that log it. */
    static String normalise(String contentType) {
        if (contentType == null) {
            return "";
        }
        int parameters = contentType.indexOf(';');
        String base = parameters > 0 ? contentType.substring(0, parameters) : contentType;
        return base.trim().toLowerCase(Locale.ROOT);
    }
}