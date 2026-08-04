package com.aitechskill.book.storage.service;

import com.aitechskill.book.storage.domain.StorageObjectRequest;
import com.aitechskill.book.storage.domain.StoredObject;
import com.aitechskill.book.storage.domain.StoredObjectContent;

/**
 * 通用对象存储服务，业务模块不得直接依赖 S3 客户端。
 */
public interface ObjectStorageService {

    /**
     * 按统一对象键规范写入文件。
     *
     * @param request 文件写入请求
     * @return 已写入对象元数据
     */
    StoredObject put(StorageObjectRequest request);

    /**
     * 读取指定对象。
     *
     * @param objectKey 对象键
     * @return 文件内容
     */
    StoredObjectContent get(String objectKey);

    /**
     * 删除指定对象，目标不存在时视为成功。
     *
     * @param objectKey 对象键
     */
    void delete(String objectKey);
}
