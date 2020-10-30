package com.supereal.bigfile.service;

import com.supereal.bigfile.form.FileForm;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Create by tianci
 * 2019/1/11 11:23
 */
public interface UploadFileService {

    /**
     * 通过md5值查找文件对象
     * @param md5
     * @return
     */
    Map<String, Object> findByFileMd5(FileForm md5);

    /**
     * 上传文件
     * @param form 文件表单信息
     * @param multipartFile 文件
     * @return
     */
    Map<String, Object> realUpload(FileForm form, MultipartFile multipartFile) throws IOException, Exception;

    /**
     * 检查切片文件是否需要上传
     * @param form
     * @return
     */
    Map<String,Object> checkPartFile(FileForm form);


    Map<String, Object> assembleFiles(String fileId,int total)throws IOException;
}
