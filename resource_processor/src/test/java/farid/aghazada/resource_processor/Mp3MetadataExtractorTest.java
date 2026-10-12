package farid.aghazada.resource_processor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import farid.aghazada.resource_processor.DTO.SongMetadataRequestDTO;
import farid.aghazada.resource_processor.Exception.InvalidMp3FileException;
import farid.aghazada.resource_processor.Service.Mp3MetadataExtractor;

class Mp3MetadataExtractorTest {

    private final Mp3MetadataExtractor extractor = new Mp3MetadataExtractor();

    @Test
    void extractsTagsFromValidMp3() throws Exception {
        byte[] mp3 = new ClassPathResource("sample.mp3").getInputStream().readAllBytes();
        SongMetadataRequestDTO tags = extractor.extractTags(mp3);
        assertNotNull(tags.name());
        assertNotNull(tags.duration());
    }

    @Test
    void rejectsGarbage() {
        assertThrows(InvalidMp3FileException.class, () -> extractor.extractTags(new byte[] {1, 2, 3}));
    }
}
