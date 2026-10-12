package farid.aghazada.resource_service.Service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import farid.aghazada.resource_service.Exception.StorageException;
import farid.aghazada.resource_service.Exception.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public String uploadFile(byte[] audioData, String contentType) {

        String key = UUID.randomUUID() + ".mp3";

        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(contentType)
            .contentLength((long) audioData.length)
            .build();

        try {
            s3Client.putObject(request, RequestBody.fromBytes(audioData));
        } catch (SdkException e) {
            throw new StorageException("Failed to store file", e);
        }

        return key;
    }

    public byte[] getFile(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build();

        try {
            return s3Client.getObjectAsBytes(request).asByteArray();
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFoundException("File not found in storage");
        } catch (SdkException e) {
            throw new StorageException("Failed to read file", e);
        }
    }

    public void deleteFile(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build();

        try {
            s3Client.deleteObject(request);
        } catch (SdkException e) {
            throw new StorageException("Failed to delete file", e);
        }
    }

}
