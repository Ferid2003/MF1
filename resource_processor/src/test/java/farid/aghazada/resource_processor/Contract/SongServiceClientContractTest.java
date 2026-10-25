package farid.aghazada.resource_processor.Contract;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties.StubsMode;
import org.springframework.web.client.HttpClientErrorException;

import farid.aghazada.resource_processor.Client.SongServiceClient;
import farid.aghazada.resource_processor.DTO.SongMetadataRequestDTO;

/** Consumer side of the HTTP contract: real client vs. WireMock stubs generated from song_service's contracts. */
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.loadbalancer.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "song.service.name=localhost:8081"
})
@AutoConfigureStubRunner(ids = "farid.aghazada:song_service:+:stubs:8081", stubsMode = StubsMode.LOCAL)
class SongServiceClientContractTest {

    @Autowired
    SongServiceClient client;

    @Test
    void saveSongMetadata_newSong_isAccepted() {
        assertThatCode(() -> client.saveSongMetadata(
                new SongMetadataRequestDTO(1, "Song", "Artist", "Album", "03:45", "2020")))
                .doesNotThrowAnyException();
    }

    @Test
    void saveSongMetadata_existingSong_conflictIsSwallowed() {
        assertThatCode(() -> client.saveSongMetadata(
                new SongMetadataRequestDTO(2, "Song", "Artist", "Album", "03:45", "2020")))
                .doesNotThrowAnyException();
    }

    @Test
    void saveSongMetadata_unknownRequestShape_isRejectedByStub() {
        // no contract covers id 3, so the stub answers 404: proves the stub really constrains the client
        assertThatThrownBy(() -> client.saveSongMetadata(
                new SongMetadataRequestDTO(3, "Song", "Artist", "Album", "03:45", "2020")))
                .isInstanceOf(HttpClientErrorException.NotFound.class);
    }
}
