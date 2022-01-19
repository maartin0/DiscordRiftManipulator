import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class CommandUpdate {

    final String appID;
    final String token;
    final String headers;
    final String url;

    CommandUpdate(String appID, String token) {
        this.appID = appID;
        this.token = token;
        this.headers = String.format("{ \"Authorization\": \"Bot %s\" }", this.token);
        this.url = String.format("https://discord.com/api/v8/applications/%s/commands", appID);
    }

    String sendBody(String content) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream output = connection.getOutputStream()) {
            output.write(content.getBytes());
            output.flush();
        }

        InputStream response = connection.getInputStream();
        String result = new String(response.readAllBytes());

        connection.disconnect();

        return result;
    }

    public void updateCommands() {

    }



}
