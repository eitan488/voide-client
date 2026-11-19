package voidclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import voidclient.VoidClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Atomic JSON file IO with a .bak copy of the previous version. */
public final class JsonFiles {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private JsonFiles() {}

    public static JsonObject read(Path file) {
        if (!Files.isRegularFile(file)) return null;
        try {
            JsonElement e = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            return e.isJsonObject() ? e.getAsJsonObject() : null;
        } catch (IOException | RuntimeException ex) {
            VoidClient.LOG.error("Could not read {}, trying backup", file, ex);
            Path bak = file.resolveSibling(file.getFileName() + ".bak");
            if (Files.isRegularFile(bak)) {
                try {
                    JsonElement e = JsonParser.parseString(Files.readString(bak, StandardCharsets.UTF_8));
                    return e.isJsonObject() ? e.getAsJsonObject() : null;
                } catch (IOException | RuntimeException ignored) {
                }
            }
            return null;
        }
    }

    public static boolean write(Path file, JsonElement json) {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(json), StandardCharsets.UTF_8);
            if (Files.exists(file)) Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            VoidClient.LOG.error("Could not write {}", file, e);
            return false;
        }
    }
}
