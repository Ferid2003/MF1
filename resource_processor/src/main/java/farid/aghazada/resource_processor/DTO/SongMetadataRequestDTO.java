package farid.aghazada.resource_processor.DTO;

public record SongMetadataRequestDTO(
        Integer id,
        String name,
        String artist,
        String album,
        String duration,
        String year
) {
}
