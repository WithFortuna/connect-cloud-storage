package com.example.aichatterdemo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class DropboxService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${dropbox.download-url}")
    private String CONTENT_DOWNLOAD_URL;

    public ByteArrayOutputStream downloadFiles(List<String> ids, String accessToken) throws IOException, InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            HttpClient client = HttpClient.newHttpClient();
            for (String rawId : ids) {
                String path = normalizePath(rawId);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(CONTENT_DOWNLOAD_URL))
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Dropbox-API-Arg", "{\"path\":\"" + escapeForHeader(path) + "\"}")
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

                HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (resp.statusCode() != 200) {
                    String body = new String(Optional.ofNullable(resp.body()).orElse(new byte[0]), StandardCharsets.UTF_8);
                    throw new IOException("Dropbox download failed: status=" + resp.statusCode() + ", body=" + body);
                }

                String metaHeader = resp.headers().firstValue("Dropbox-API-Result").orElse("{}");
                String fileName = extractName(metaHeader);
                if (fileName == null || fileName.isBlank()) {
                    fileName = sanitizeFilename(rawId.replace("id:", ""));
                }

                ZipEntry entry = new ZipEntry(fileName);
                zip.putNextEntry(entry);
                zip.write(resp.body());
                zip.closeEntry();
            }
        }
        return out;
    }

    private static String normalizePath(String idOrPath) {
        if (idOrPath == null) return "";
        String s = idOrPath.trim();
        if (s.startsWith("id:")) return s;
        if (s.startsWith("/")) return s; // allow full path
        return "id:" + s;
    }

    private String extractName(String metadataHeader) {
        try {
            JsonNode node = objectMapper.readTree(metadataHeader);
            if (node.hasNonNull("name")) return node.get("name").asText();
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String sanitizeFilename(String s) {
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String escapeForHeader(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

