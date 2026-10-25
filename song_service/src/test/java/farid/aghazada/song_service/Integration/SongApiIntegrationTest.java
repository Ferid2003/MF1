package farid.aghazada.song_service.Integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Controller + service + JPA against a real Postgres. */
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Testcontainers
class SongApiIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void db(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    MockMvc mvc;

    private static String song(int id, String duration) {
        return """
                {"id":%d,"name":"Song","artist":"Artist","album":"Album","duration":"%s","year":"2020"}"""
                .formatted(id, duration);
    }

    @Test
    void createGetDelete_roundTripsThroughDatabase() throws Exception {
        mvc.perform(post("/songs").contentType(MediaType.APPLICATION_JSON).content(song(1, "03:45")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        mvc.perform(get("/songs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Song"));

        mvc.perform(delete("/songs").param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ids[0]").value(1));

        mvc.perform(get("/songs/1")).andExpect(status().isNotFound());
    }

    @Test
    void create_duplicateId_returns409() throws Exception {
        mvc.perform(post("/songs").contentType(MediaType.APPLICATION_JSON).content(song(2, "03:45")))
                .andExpect(status().isOk());
        mvc.perform(post("/songs").contentType(MediaType.APPLICATION_JSON).content(song(2, "03:45")))
                .andExpect(status().isConflict());
    }

    @Test
    void create_invalidDuration_returns400() throws Exception {
        mvc.perform(post("/songs").contentType(MediaType.APPLICATION_JSON).content(song(3, "3:45")))
                .andExpect(status().isBadRequest());
    }
}
