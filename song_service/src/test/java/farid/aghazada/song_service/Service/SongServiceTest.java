package farid.aghazada.song_service.Service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import farid.aghazada.song_service.DTO.Request.SongCreationDTO;
import farid.aghazada.song_service.Entity.SongMetadataEntity;
import farid.aghazada.song_service.Exception.SongMetadataAlreadyExistsException;
import farid.aghazada.song_service.Exception.SongMetadataNotFoundException;
import farid.aghazada.song_service.Repository.SongRepository;

@ExtendWith(MockitoExtension.class)
class SongServiceTest {

    @Mock
    SongRepository songRepository;

    @InjectMocks
    SongService songService;

    private final SongCreationDTO dto = new SongCreationDTO(1, "Song", "Artist", "Album", "03:45", "2020");

    @Test
    void createSongMetadata_savesAndReturnsId() {
        when(songRepository.findById(1)).thenReturn(Optional.empty());
        when(songRepository.save(any(SongMetadataEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        assertThat(songService.createSongMetadata(dto)).containsEntry("id", 1);
    }

    @Test
    void createSongMetadata_duplicateId_throws() {
        when(songRepository.findById(1)).thenReturn(Optional.of(new SongMetadataEntity()));

        assertThatThrownBy(() -> songService.createSongMetadata(dto))
                .isInstanceOf(SongMetadataAlreadyExistsException.class);
        verify(songRepository, never()).save(any());
    }

    @Test
    void getSongMetadata_unknownId_throws() {
        when(songRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> songService.getSongMetadata(99))
                .isInstanceOf(SongMetadataNotFoundException.class);
    }
}
