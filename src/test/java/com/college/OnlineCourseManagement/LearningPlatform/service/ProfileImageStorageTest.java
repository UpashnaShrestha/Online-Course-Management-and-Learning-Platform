package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Uploaded files are untrusted input. These tests pin the three rules that keep that from
 * turning into a path-traversal bug, a stored-XSS bug, or a memory-exhaustion bug.
 */
class ProfileImageStorageTest {

    private static final long MAX_BYTES = 1024 * 1024;

    private ProfileImageStorage storageIn(Path dir) {
        return new ProfileImageStorage(dir.toString(), "profile-images", "/uploads", DataSize.ofBytes(MAX_BYTES));
    }

    // Minimal but genuinely valid magic-byte headers, followed by filler.
    private static byte[] png() {
        return withHeader(new byte[] {
                (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13 });
    }

    private static byte[] jpeg() {
        return withHeader(new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0 });
    }

    private static byte[] gif() {
        return withHeader(new byte[] { 'G', 'I', 'F', '8', '9', 'a', 0, 0, 0, 0, 0, 0 });
    }

    private static byte[] webp() {
        return withHeader(new byte[] { 'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P' });
    }

    private static byte[] withHeader(byte[] header) {
        byte[] full = new byte[256];
        System.arraycopy(header, 0, full, 0, header.length);
        for (int i = header.length; i < full.length; i++) {
            full[i] = (byte) i;
        }
        return full;
    }

    private static MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, null, content);
    }

    @Test
    @DisplayName("stores a PNG and returns a public URL under the configured prefix")
    void storesPng(@TempDir Path dir) {
        String url = storageIn(dir).store(7L, file("photo.png", png()), null);

        assertThat(url).startsWith("/uploads/profile-images/user-7-").endsWith(".png");
        assertThat(Files.exists(dir.resolve("profile-images")
                .resolve(url.substring(url.lastIndexOf('/') + 1)))).isTrue();
    }

    @Test
    @DisplayName("the extension comes from the sniffed type, not the client's filename")
    void extensionComesFromContent(@TempDir Path dir) {
        // A PNG named "evil.txt" must not be stored with a .txt extension.
        String url = storageIn(dir).store(1L, file("evil.txt", png()), null);

        assertThat(url).endsWith(".png");
    }

    @Test
    @DisplayName("recognises JPEG, PNG, GIF and WebP")
    void acceptsEverySupportedFormat(@TempDir Path dir) {
        ProfileImageStorage storage = storageIn(dir);

        assertThat(storage.store(1L, file("a.png", png()), null)).endsWith(".png");
        assertThat(storage.store(2L, file("b.jpeg", jpeg()), null)).endsWith(".jpeg");
        assertThat(storage.store(3L, file("c.gif", gif()), null)).endsWith(".gif");
        assertThat(storage.store(4L, file("d.webp", webp()), null)).endsWith(".webp");
    }

    @Test
    @DisplayName("rejects content that is not an image, whatever Content-Type is claimed")
    void rejectsNonImage(@TempDir Path dir) {
        // Declared as image/png, but the bytes are a shell script.
        byte[] script = "#!/bin/sh\nrm -rf /\n".getBytes(StandardCharsets.UTF_8);
        var disguised = new MockMultipartFile("file", "payload.png", "image/png", script);

        assertThatThrownBy(() -> storageIn(dir).store(1L, disguised, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a supported image");
    }

    @Test
    @DisplayName("rejects an SVG, which can carry script")
    void rejectsSvg(@TempDir Path dir) {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script/></svg>"
                .getBytes(StandardCharsets.UTF_8);
        var file = new MockMultipartFile("file", "x.svg", "image/svg+xml", svg);

        assertThatThrownBy(() -> storageIn(dir).store(1L, file, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("ignores the client's filename entirely, so paths cannot be traversed")
    void ignoresClientFilename(@TempDir Path dir) {
        String traversal = "../../../../etc/passwd.png";
        String url = storageIn(dir).store(1L, file(traversal, png()), null);

        assertThat(url).doesNotContain("..").doesNotContain("etc");
        // And the file really is inside the upload directory.
        assertThat(Path.of(url.substring(url.lastIndexOf('/') + 1)).getFileName().toString())
                .isNotEqualTo("passwd.png");
    }

    @Test
    @DisplayName("rejects a file larger than the limit")
    void rejectsOversized(@TempDir Path dir) {
        ProfileImageStorage tiny = new ProfileImageStorage(
                dir.toString(), "profile-images", "/uploads", DataSize.ofBytes(16L));

        assertThatThrownBy(() -> tiny.store(1L, file("big.png", png()), null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("too large");
    }

    @Test
    @DisplayName("rejects an empty upload")
    void rejectsEmpty(@TempDir Path dir) {
        // MultipartFile.isEmpty() is true for zero bytes, so this is caught before sniffing.
        assertThatThrownBy(() -> storageIn(dir).store(1L, file("empty.png", new byte[0]), null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("choose an image");
    }

    @Test
    @DisplayName("rejects a file too short to carry any image signature")
    void rejectsTooShortToIdentify(@TempDir Path dir) {
        // Two bytes: not empty, so it reaches the sniffer, which cannot identify it.
        assertThatThrownBy(() -> storageIn(dir).store(1L, file("tiny.png", new byte[2]), null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("too small");
    }

    @Test
    @DisplayName("replacing an image deletes the previous file")
    void replacingDeletesTheOldFile(@TempDir Path dir) throws IOException {
        ProfileImageStorage storage = storageIn(dir);

        String first = storage.store(1L, file("a.png", png()), null);
        Path firstPath = dir.resolve("profile-images")
                .resolve(first.substring(first.lastIndexOf('/') + 1));
        assertThat(Files.exists(firstPath)).isTrue();

        String second = storage.store(1L, file("b.png", png()), first);

        assertThat(second).isNotEqualTo(first);
        assertThat(Files.exists(firstPath))
                .as("the replaced image must not be left on disk")
                .isFalse();
    }

    @Test
    @DisplayName("deleting an externally hosted URL is a no-op, never a file delete")
    void deletingForeignUrlIsSafe(@TempDir Path dir) {
        ProfileImageStorage storage = storageIn(dir);

        // A path that would resolve inside the upload directory if it were trusted.
        storage.deleteQuietly("https://example.com/avatar.png");
        storage.deleteQuietly("/uploads/profile-images/../../application.yaml");
        storage.deleteQuietly(null);

        assertThat(Files.exists(dir.resolve("application.yaml"))).isFalse();
    }

    @Test
    @DisplayName("deleteQuietly only touches files under the image directory")
    void deletesOnlyOwnFiles(@TempDir Path dir) throws IOException {
        ProfileImageStorage storage = storageIn(dir);
        Path outside = dir.resolve("keep-me.txt");
        Files.writeString(outside, "important");

        storage.deleteQuietly("/uploads/profile-images/../keep-me.txt");

        assertThat(Files.exists(outside))
                .as("traversal in a stored URL must not delete files outside the upload directory")
                .isTrue();
    }

    @Test
    @DisplayName("refuses a sub-directory that would escape the upload root")
    void rejectsEscapingSubdirectory(@TempDir Path dir) {
        assertThatThrownBy(() -> new ProfileImageStorage(
                dir.resolve("uploads").toString(), "../../etc", "/uploads", DataSize.ofBytes(MAX_BYTES)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must stay inside");
    }
}