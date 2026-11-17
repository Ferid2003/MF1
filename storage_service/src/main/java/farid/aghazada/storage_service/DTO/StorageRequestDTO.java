package farid.aghazada.storage_service.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import farid.aghazada.storage_service.Entity.StorageEntity;
import farid.aghazada.storage_service.Entity.StorageType;

public record StorageRequestDTO(
        @NotNull
        StorageType storageType,
        @NotBlank
        String bucket,
        @NotBlank
        String path
        ) {

    public static StorageEntity toStorageEntity(StorageRequestDTO storageRequestDTO) {
        return StorageEntity.builder()
            .storageType(storageRequestDTO.storageType())
            .bucket(storageRequestDTO.bucket())
            .path(storageRequestDTO.path())
            .build();
    }
}
