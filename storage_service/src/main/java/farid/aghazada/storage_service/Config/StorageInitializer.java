package farid.aghazada.storage_service.Config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import farid.aghazada.storage_service.Entity.StorageEntity;
import farid.aghazada.storage_service.Entity.StorageType;
import farid.aghazada.storage_service.Repository.StorageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageInitializer implements ApplicationRunner {

    private final StorageRepository storageRepository;
    private final S3Client s3Client;

    @Override
    public void run(ApplicationArguments args) {
        seed(StorageType.STAGING, "staging-bucket", "/files");
        seed(StorageType.PERMANENT, "permanent-bucket", "/files");
        for (StorageEntity storage : storageRepository.findAll()) {
            createBucketIfMissing(storage.getBucket());
        }
    }

    private void seed(StorageType type, String bucket, String path) {
        if (!storageRepository.existsByStorageType(type)) {
            storageRepository.save(StorageEntity.builder().storageType(type).bucket(bucket).path(path).build());
            log.info("Seeded {} storage: {}{}", type, bucket, path);
        }
    }

    private void createBucketIfMissing(String bucket) {
        try {
            s3Client.createBucket(b -> b.bucket(bucket));
            log.info("Ensured bucket {}", bucket);
        } catch (BucketAlreadyOwnedByYouException e) {
            log.info("Bucket already present");
        }
    }
}
