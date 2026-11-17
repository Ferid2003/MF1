package farid.aghazada.storage_service.Controller;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import farid.aghazada.storage_service.DTO.StorageRequestDTO;
import farid.aghazada.storage_service.Entity.StorageEntity;
import farid.aghazada.storage_service.Service.StorageService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/storages")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    @PostMapping
    public ResponseEntity<Map<String, Integer>> createStorage(@RequestBody @Valid StorageRequestDTO storageRequestDTO) {
        return storageService.createStorage(storageRequestDTO);
    }

    @GetMapping
    public ResponseEntity<List<StorageEntity>> getAllStorages() {
        return storageService.getAllStorage();
    }

    @DeleteMapping
    public ResponseEntity<Map<String, List<Integer>>> deleteStorages(@RequestParam String id) {
        return storageService.deleteStorages(id);
    }
}
