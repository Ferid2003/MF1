package farid.aghazada.storage_service.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import farid.aghazada.storage_service.DTO.StorageRequestDTO;
import farid.aghazada.storage_service.Entity.StorageEntity;
import farid.aghazada.storage_service.Repository.StorageRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final StorageRepository storageRepository;

    public ResponseEntity<Map<String, Integer>> createStorage(StorageRequestDTO storageRequestDTO) {
        StorageEntity storedStorage = (storageRepository.save(StorageRequestDTO.toStorageEntity(storageRequestDTO)));
        return ResponseEntity.ok(Map.of("id", storedStorage.getId()));
    }

    public ResponseEntity<List<StorageEntity>> getAllStorage() {
        return ResponseEntity.ok(storageRepository.findAll());
    }

    public ResponseEntity<Map<String, List<Integer>>> deleteStorages(String ids) {
        if (ids.length() >= 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "CSV string is too long: received " + ids.length() + " characters, must be less than 200");
        }
        List<Integer> parsedIds = new ArrayList<>();
        for (String token : ids.split(",")) {
            try {
                int value = Integer.parseInt(token.trim());
                if (value <= 0) {
                    throw new NumberFormatException();
                }
                parsedIds.add(value);
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid ID format: '" + token + "'. Only positive integers are allowed");
            }
        }
        // missing ids are ignored; only existing ones are deleted and reported
        List<Integer> existingIds = storageRepository.findAllById(parsedIds).stream()
                .map(StorageEntity::getId)
                .toList();
        storageRepository.deleteAllByIdInBatch(existingIds);
        return ResponseEntity.ok(Map.of("ids", existingIds));
    }

}
