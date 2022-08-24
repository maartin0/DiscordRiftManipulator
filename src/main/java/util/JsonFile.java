package util;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

class FileObject {
    public JsonObject data;
    public FileObject(JsonObject data) {
        this.data = data;
    }
    public FileObject get(String key) {
        return new FileObject(data.get(key).getAsJsonObject());
    }
    public String getString(String key) {
        return data.get(key).getAsString();
    }
}

public class JsonFile extends FileObject {
    public final Path path;
    public final String pathName;
    public JsonFile(String path) {
        super(new JsonObject());
        this.pathName = path;
        this.path = Path.of(this.pathName);
    }
    public void forceLoad() {
        try {
            load();
        } catch (IOException ignored) {
        }
    }
    public void load() throws IOException {
        this.data = JsonParser.parseString(
                Files.readString(this.path)
        ).getAsJsonObject();
    }
    public void save() throws IOException {
        save(false);
    }
    public void save(Boolean pretty) throws IOException {;
        Files.writeString(
            this.path,
            (pretty
                    ? new GsonBuilder().setPrettyPrinting()
                    : new GsonBuilder()
            ).create()
             .toJson(this.data),
            Charset.defaultCharset()
        );
    }
}
