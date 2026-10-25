package farid.aghazada.song_service.Component;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class SongSteps {

    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    static {
        postgres.start(); // ponytail: one container per JVM, Ryuk cleans it up
    }

    @DynamicPropertySource
    static void db(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    int port;

    private final HttpClient http = HttpClient.newHttpClient();
    private HttpResponse<String> response;

    private HttpResponse<String> send(HttpRequest.Builder b, String path) throws Exception {
        return http.send(b.uri(URI.create("http://localhost:" + port + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private void create(int id, String name, String duration) throws Exception {
        String body = """
                {"id":%d,"name":"%s","artist":"Artist","album":"Album","duration":"%s","year":"2020"}"""
                .formatted(id, name, duration);
        response = send(HttpRequest.newBuilder().header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)), "/songs");
    }

    @Given("a song with id {int} exists")
    public void songExists(int id) throws Exception {
        create(id, "Existing", "01:00");
        assertThat(response.statusCode()).isEqualTo(200);
    }

    @When("I create a song with id {int}, name {string} and duration {string}")
    public void iCreate(int id, String name, String duration) throws Exception {
        create(id, name, duration);
    }

    @When("I get the song with id {int}")
    public void iGet(int id) throws Exception {
        response = send(HttpRequest.newBuilder().GET(), "/songs/" + id);
    }

    @Then("the response status is {int}")
    public void status(int expected) {
        assertThat(response.statusCode()).isEqualTo(expected);
    }

    @Then("the song name is {string}")
    public void name(String name) {
        assertThat(response.body()).contains("\"name\":\"" + name + "\"");
    }
}
