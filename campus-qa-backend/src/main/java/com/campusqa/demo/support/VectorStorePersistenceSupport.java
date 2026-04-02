package com.campusqa.demo.support;

import org.springframework.ai.document.Document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;

/**
 * SimpleVectorStore 本地持久化辅助类。
 */
public final class VectorStorePersistenceSupport {

    private static final Path VECTOR_STORE_BASE_DIR = Paths.get(System.getProperty("user.dir"), "data", "vector-store");

    private VectorStorePersistenceSupport() {
    }

    public static Path resolveStoreFile(String fileName) {
        return VECTOR_STORE_BASE_DIR.resolve(fileName);
    }

    public static Path resolveSignatureFile(String fileName) {
        return VECTOR_STORE_BASE_DIR.resolve(fileName + ".sha256");
    }

    public static void ensureBaseDirectory() throws IOException {
        Files.createDirectories(VECTOR_STORE_BASE_DIR);
    }

    public static boolean signatureMatches(Path signatureFile, String currentSignature) throws IOException {
        if (!Files.exists(signatureFile)) {
            return false;
        }
        String savedSignature = Files.readString(signatureFile, StandardCharsets.UTF_8).trim();
        return savedSignature.equals(currentSignature);
    }

    public static void writeSignature(Path signatureFile, String signature) throws IOException {
        ensureBaseDirectory();
        Files.writeString(signatureFile, signature, StandardCharsets.UTF_8);
    }

    public static String calculateSignature(List<Document> documents) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            documents.stream()
                    .sorted(Comparator.comparing(Document::getId))
                    .forEach(document -> updateDigest(digest, document));
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256", exception);
        }
    }

    private static void updateDigest(MessageDigest digest, Document document) {
        updatePart(digest, document.getId());
        updatePart(digest, document.getText());
    }

    private static void updatePart(MessageDigest digest, String value) {
        digest.update((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
        digest.update((byte) '\n');
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
