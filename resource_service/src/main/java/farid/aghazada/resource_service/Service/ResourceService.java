package farid.aghazada.resource_service.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import farid.aghazada.resource_service.Client.SongServiceClient;
import farid.aghazada.resource_service.Client.StorageClient;
import farid.aghazada.resource_service.DTO.StorageDTO;
import farid.aghazada.resource_service.Entity.ResourceEntity;
import farid.aghazada.resource_service.Entity.ResourceState;
import farid.aghazada.resource_service.Exception.CSVFormatException;
import farid.aghazada.resource_service.Exception.CSVLengthRestrictionException;
import farid.aghazada.resource_service.Exception.ContentMismatchException;
import farid.aghazada.resource_service.Exception.InvalidIdException;
import farid.aghazada.resource_service.Exception.ResourceNotFoundException;
import farid.aghazada.resource_service.Repository.ResourceRepository;
import farid.aghazada.resource_service.RabbitMQ.RabbitMQProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final SongServiceClient songServiceClient;
    private final StorageClient storageClient;
    private final S3Service s3Service;
    private final RabbitMQProducer rabbitMQProducer;

    @Transactional
    public Map<String, Integer> uploadMp3(byte[] audioData, String contentType) {
        if (!"audio/mpeg".equalsIgnoreCase(contentType)) {
            throw new ContentMismatchException("Invalid file format: " + contentType + ". Only MP3 files are allowed");
        }

        StorageDTO staging = storageClient.getStorage(ResourceState.STAGING);
        String key = buildKey(staging.path(), UUID.randomUUID() + ".mp3");
        s3Service.uploadFile(staging.bucket(), key, audioData, contentType);

        try {
            Integer id = resourceRepository.save(ResourceEntity.builder()
                    .link(key)
                    .state(ResourceState.STAGING)
                    .bucket(staging.bucket())
                    .path(staging.path())
                    .build()).getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    rabbitMQProducer.sendMessage(id.toString());
                }
            });
            return Map.of("id", id);
        } catch (RuntimeException e) {
            s3Service.deleteFile(staging.bucket(), key);
            throw e;
        }
    }

    @Transactional
    public void moveToPermanent(Integer id) {
        ResourceEntity resource = resourceRepository.findById(id).orElse(null);
        if (resource == null) {
            log.warn("Processed notification for unknown resource {}, ignoring", id);
            return;
        }
        if (resource.getState() == ResourceState.PERMANENT) {
            return; // duplicate notification
        }
        StorageDTO permanent = storageClient.getStorage(ResourceState.PERMANENT);
        String fileName = resource.getLink().substring(resource.getLink().lastIndexOf('/') + 1);
        String newKey = buildKey(permanent.path(), fileName);
        String oldBucket = resource.getBucket();
        String oldKey = resource.getLink();
        s3Service.copyFile(oldBucket, oldKey, permanent.bucket(), newKey);
        // drop the source only once the DB points at the copy; on rollback drop the copy instead
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    s3Service.deleteFile(oldBucket, oldKey);
                } else {
                    s3Service.deleteFile(permanent.bucket(), newKey);
                }
            }
        });
        resource.setState(ResourceState.PERMANENT);
        resource.setBucket(permanent.bucket());
        resource.setPath(permanent.path());
        resource.setLink(newKey);
        resourceRepository.save(resource);
    }

    private String buildKey(String path, String fileName) {
        String prefix = path == null ? "" : path.replaceAll("^/+|/+$", "");
        return prefix.isEmpty() ? fileName : prefix + "/" + fileName;
    }

    public byte[] getMp3(Integer id) {
        if (id <= 0) {
            throw new InvalidIdException("Invalid value '" + id + "' for ID. Must be a positive integer");
        }
        ResourceEntity resourceEntity = resourceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Resource with ID=" + id + " not found"));
        return s3Service.getFile(resourceEntity.getBucket(), resourceEntity.getLink());
    }

    @Transactional
    public Map<String, List<Integer>> deleteResources(String csvIds) {
        if (csvIds.length() > 200) {
            throw new CSVLengthRestrictionException(
                    "CSV string is too long: received " + csvIds.length() + " characters, maximum allowed is 200");
        }
        List<Integer> parsedIds = parseCsvIds(csvIds);
        List<ResourceEntity> existing = resourceRepository.findAllById(parsedIds);
        List<Integer> existingIds = existing.stream().map(ResourceEntity::getId).toList();
        existing.forEach(r -> s3Service.deleteFile(r.getBucket(), r.getLink()));
        resourceRepository.deleteAllByIdInBatch(existingIds);
        if (!existingIds.isEmpty()) {
            String existingCsv = existingIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            songServiceClient.deleteSongMetadata(existingCsv);
        }
        return Map.of("ids", existingIds);
    }

    private List<Integer> parseCsvIds(String csvIds) {
        List<Integer> result = new ArrayList<>();
        for (String token : csvIds.split(",")) {
            try {
                int value = Integer.parseInt(token.trim());
                if (value <= 0) {
                    throw new NumberFormatException();
                }
                result.add(value);
            } catch (NumberFormatException e) {
                throw new CSVFormatException("Invalid ID format: '" + token + "'. Only positive integers are allowed");
            }
        }
        return result;
    }
}
