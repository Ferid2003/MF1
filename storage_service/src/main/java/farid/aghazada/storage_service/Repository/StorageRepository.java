package farid.aghazada.storage_service.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import farid.aghazada.storage_service.Entity.StorageEntity;
import farid.aghazada.storage_service.Entity.StorageType;

@Repository
public interface StorageRepository extends JpaRepository<StorageEntity, Integer> {

    boolean existsByStorageType(StorageType storageType);
}
