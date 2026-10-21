package farid.aghazada.resource_processor.Client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import farid.aghazada.resource_processor.DTO.SongMetadataRequestDTO;

@Component
public class SongServiceClient {

    private final RestClient restClient;

    public SongServiceClient(@LoadBalanced RestClient.Builder builder, @Value("${song.service.name}") String songServiceName) {
        this.restClient = builder.baseUrl("http://" + songServiceName).build();
    }

    public void saveSongMetadata(SongMetadataRequestDTO songMetadata) {
        try {
            restClient.post()
                .uri("/songs")
                .body(songMetadata)
                .retrieve()
                .toBodilessEntity();
        } catch (HttpClientErrorException.Conflict e) {
            //  already saved, redelivery is a no-op
        }
    }
}
