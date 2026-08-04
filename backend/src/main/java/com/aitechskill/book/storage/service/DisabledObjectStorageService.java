package com.aitechskill.book.storage.service;

import com.aitechskill.book.storage.domain.StorageObjectRequest;
import com.aitechskill.book.storage.domain.StoredObject;
import com.aitechskill.book.storage.domain.StoredObjectContent;
import com.aitechskill.book.storage.exception.ObjectStorageException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 本地未配置对象存储时的安全占位实现。
 */
@Service
@ConditionalOnProperty(name = "app.storage.enabled", havingValue = "false", matchIfMissing = true)
public class DisabledObjectStorageService implements ObjectStorageService {

    /** 拒绝未配置环境的写入。 */
    @Override
    public StoredObject put(StorageObjectRequest request) {
        throw unavailable();
    }

    /** 拒绝未配置环境的读取。 */
    @Override
    public StoredObjectContent get(String objectKey) {
        throw unavailable();
    }

    /** 拒绝未配置环境的删除。 */
    @Override
    public void delete(String objectKey) {
        throw unavailable();
    }

    /** 创建统一的未配置异常。 */
    private ObjectStorageException unavailable() {
        return new ObjectStorageException("对象存储尚未配置");
    }
}
