package farid.aghazada.resource_processor.Service;

import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.metadata.XMPDM;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import farid.aghazada.resource_processor.Client.ResourceServiceClient;
import farid.aghazada.resource_processor.Client.SongServiceClient;
import farid.aghazada.resource_processor.DTO.SongMetadataRequestDTO;
import farid.aghazada.resource_processor.Exception.InvalidMp3FileException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class Mp3MetadataExtractor {

    private final ResourceServiceClient resourceServiceClient;
    private final SongServiceClient songServiceClient;

    public SongMetadataRequestDTO extractTags(byte[] audioData, Integer resourceId) {
        try {
            Metadata metadata = new Metadata();
            Parser parser = new AutoDetectParser();
            try (TikaInputStream stream = TikaInputStream.get(audioData)) {
                parser.parse(stream, new BodyContentHandler(-1), metadata, new ParseContext());
            }
            String name = metadata.get(TikaCoreProperties.TITLE);
            String artist = metadata.get(XMPDM.ARTIST);
            String album = metadata.get(XMPDM.ALBUM);
            String year = metadata.get(XMPDM.RELEASE_DATE);
            String duration = toMinutesSeconds(metadata.get(XMPDM.DURATION));
            return new SongMetadataRequestDTO(resourceId, name, artist, album, duration, year);
        } catch (Exception e) {
            throw new InvalidMp3FileException("Uploaded file is not a valid MP3 file");
        }
    }

    private String toMinutesSeconds(String durationSeconds) {
        double totalSeconds = Double.parseDouble(durationSeconds);
        int minutes = (int) totalSeconds / 60;
        int seconds = (int) totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Retryable(includes = { ResourceAccessException.class, HttpServerErrorException.class },
               maxRetries = 3, delay = 1000, multiplier = 2)
    public void processResource(String id) {
        Integer resourceId = Integer.valueOf(id);
        byte[] song = resourceServiceClient.getSong(resourceId);
        SongMetadataRequestDTO songMetadataRequestDTO = extractTags(song, resourceId);
        songServiceClient.saveSongMetadata(songMetadataRequestDTO);
    }
}
