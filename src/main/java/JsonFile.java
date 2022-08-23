import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonFile {
    public final Path path;
    public final String pathName;
    public JsonObject data;
    public JsonFile(String path) throws IOException {
        this.pathName = path;
        this.path = Path.of(this.pathName);
        this.data = new JsonObject();
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
    public void save(Boolean pretty) throws IOException {
        Files.writeString(this.path, this.data.toString(), Charset.defaultCharset());
    }
}
