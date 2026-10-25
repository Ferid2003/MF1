package farid.aghazada.song_service.Contract;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;

import farid.aghazada.song_service.Controller.SongController;
import farid.aghazada.song_service.Exception.GlobalExceptionHandler;
import farid.aghazada.song_service.Exception.SongMetadataAlreadyExistsException;
import farid.aghazada.song_service.Service.SongService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

/** Provider side: controller + real exception handler, service stubbed by id. */
public abstract class ContractBase {

    @BeforeEach
    void setup() {
        SongService service = mock(SongService.class);
        when(service.createSongMetadata(any())).thenAnswer(inv -> {
            int id = inv.<farid.aghazada.song_service.DTO.Request.SongCreationDTO>getArgument(0).id();
            if (id == 2) {
                throw new SongMetadataAlreadyExistsException(id);
            }
            return Map.of("id", id);
        });
        RestAssuredMockMvc.standaloneSetup(
                org.springframework.test.web.servlet.setup.MockMvcBuilders
                        .standaloneSetup(new SongController(service))
                        .setControllerAdvice(new GlobalExceptionHandler()));
    }
}
