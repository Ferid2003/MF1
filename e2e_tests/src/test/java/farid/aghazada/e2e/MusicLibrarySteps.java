package farid.aghazada.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class MusicLibrarySteps {

    private static final String BASE = System.getProperty("e2e.base-url", "http://localhost:8085");

    private final HttpClient http = HttpClient.newHttpClient();
    private HttpResponse<String> response;
    private int resourceId;

    private HttpResponse<String> call(HttpRequest.Builder b, String path) throws Exception {
        return http.send(b.uri(URI.create(BASE + path)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private void upload(byte[] body, String contentType) throws Exception {
        response = call(HttpRequest.newBuilder().header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body)), "/resources");
    }

    @When("I upload the sample MP3")
    public void uploadSample() throws Exception {
        try (var in = getClass().getResourceAsStream("/sample.mp3")) {
            upload(in.readAllBytes(), "audio/mpeg");
        }
    }

    @When("I upload a file with content type {string}")
    public void uploadWithContentType(String contentType) throws Exception {
        upload("not an mp3".getBytes(), contentType);
    }

    @Then("the upload succeeds and returns a resource id")
    public void uploadReturnsId() {
        assertThat(response.statusCode()).isEqualTo(200);
        Matcher m = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(response.body());
        assertThat(m.find()).isTrue();
        resourceId = Integer.parseInt(m.group(1));
    }

    @Then("the song metadata for that resource eventually appears")
    public void metadataAppears() throws Exception {
        // processing is asynchronous: RabbitMQ -> resource_processor -> song_service
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        do {
            response = call(HttpRequest.newBuilder().GET(), "/songs/" + resourceId);
            if (response.statusCode() == 200) {
                return;
            }
            Thread.sleep(500);
        } while (System.nanoTime() < deadline);
        assertThat(response.statusCode()).as("song metadata for resource %d", resourceId).isEqualTo(200);
    }

    @Then("the song name is {string} and the artist is {string}")
    public void nameAndArtist(String name, String artist) {
        assertThat(response.body()).contains("\"name\":\"" + name + "\"").contains("\"artist\":\"" + artist + "\"");
    }

    @When("I delete the resource")
    public void deleteResource() throws Exception {
        response = call(HttpRequest.newBuilder().DELETE(), "/resources?id=" + resourceId);
        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Then("the resource can no longer be retrieved")
    public void resourceGone() throws Exception {
        assertThat(call(HttpRequest.newBuilder().GET(), "/resources/" + resourceId).statusCode()).isEqualTo(404);
    }

    @Then("the song metadata can no longer be retrieved")
    public void songGone() throws Exception {
        assertThat(call(HttpRequest.newBuilder().GET(), "/songs/" + resourceId).statusCode()).isEqualTo(404);
    }

    @Then("the response status is {int}")
    public void status(int expected) {
        assertThat(response.statusCode()).isEqualTo(expected);
    }
}
