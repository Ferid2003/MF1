package farid.aghazada.resource_processor.Service;

import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.metadata.XMPDM;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

import farid.aghazada.resource_processor.DTO.SongMetadataRequestDTO;
import farid.aghazada.resource_processor.Exception.InvalidMp3FileException;

@Service
public class Mp3MetadataExtractor {

    public SongMetadataRequestDTO extractTags(byte[] audioData) {
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
            return new SongMetadataRequestDTO(null, name, artist, album, duration, year);
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
}
