package com.aitechskill.book.storage.service;

import com.aitechskill.book.storage.config.ObjectStorageProperties;
import com.aitechskill.book.storage.domain.StorageObjectRequest;
import com.aitechskill.book.storage.domain.StoredObject;
import com.aitechskill.book.storage.domain.StoredObjectContent;
import com.aitechskill.book.storage.exception.ObjectStorageException;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * 基于 AWS SDK 的 S3 兼容对象存储实现。
 */
@Service
@ConditionalOnProperty(name = "app.storage.enabled", havingValue = "true")
public class S3ObjectStorageService implements ObjectStorageService {

    private final S3Client s3Client;
    private final ObjectStorageProperties properties;
    private final StorageObjectKeyFactory keyFactory;

    public S3ObjectStorageService(
            S3Client s3Client,
            ObjectStorageProperties properties,
            StorageObjectKeyFactory keyFactory) {
        this.s3Client = s3Client;
        this.properties = properties;
        this.keyFactory = keyFactory;
    }

    /** 写入对象并返回稳定对象键。 */
    @Override
    public StoredObject put(StorageObjectRequest request) {
        String objectKey = keyFactory.create(request.business(), request.ownerId(), request.extension());
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .contentType(request.contentType())
                    .contentLength((long) request.content().length)
                    .cacheControl("private, max-age=604800")
                    .metadata(Map.of("business", request.business(), "owner-id", request.ownerId()))
                    .build();
            s3Client.putObject(putRequest, RequestBody.fromBytes(request.content()));
            return new StoredObject(objectKey, request.contentType(), request.content().length);
        } catch (S3Exception | SdkClientException exception) {
            throw new ObjectStorageException("对象存储写入失败", exception);
        }
    }

    /** 读取对象内容。 */
    @Override
    public StoredObjectContent get(String objectKey) {
        try {
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .build());
            String contentType = response.response().contentType();
            byte[] content = response.asByteArray();
            return new StoredObjectContent(content, contentType, content.length);
        } catch (S3Exception | SdkClientException exception) {
            throw new ObjectStorageException("对象存储读取失败", exception);
        }
    }

    /** 删除指定对象。 */
    @Override
    public void delete(String objectKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .build());
        } catch (S3Exception | SdkClientException exception) {
            throw new ObjectStorageException("对象存储删除失败", exception);
        }
    }
}
