package com.supereal.bigfile.service.Impl;

import com.supereal.bigfile.common.Constant;
import com.supereal.bigfile.dataobject.UploadFile;
import com.supereal.bigfile.form.FileForm;
import com.supereal.bigfile.repository.UploadFileRepository;
import com.supereal.bigfile.service.UploadFileService;
import com.supereal.bigfile.utils.FileMd5Util;
import com.supereal.bigfile.utils.KeyUtil;
import com.supereal.bigfile.utils.NameUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Create by tianci
 * 2019/1/11 11:24
 */

@Service
public class UploadFileServiceImpl implements UploadFileService {

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

    @Autowired
    UploadFileRepository uploadFileRepository;

    @Override
    public Map<String, Object> findByFileMd5(FileForm form) {
        String fileId = UUID.randomUUID().toString().replaceAll("-", "");
        String md5 = form.getMd5();
        String fileName = form.getName();
        String size = form.getSize();
        String suffix = NameUtil.getExtensionName(fileName);
        String saveDirectory = Constant.PATH + File.separator + fileId;
        String filePath = saveDirectory + File.separator + fileId + "." + suffix;


        UploadFile uploadFile = uploadFileRepository.findByFileMd5(md5);

        Map<String, Object> map = null;
        if (uploadFile == null) {

            uploadFile = new UploadFile();
            uploadFile.setFileMd5(md5);
            uploadFile.setFileName(fileName);
            uploadFile.setFileSuffix(suffix);
            uploadFile.setFileId(fileId);
            uploadFile.setFilePath(filePath);
            uploadFile.setFileSize(size);
            uploadFile.setFileStatus(1);
            uploadFileRepository.save(uploadFile);

            //没有上传过文件
            map = new HashMap<>(4);
            map.put("flag", 0);
            map.put("fileId", fileId);
            map.put("date", simpleDateFormat.format(new Date()));
            map.put("msg", "未上传过的文件");
        } else {
            //上传过文件，判断文件现在还存在不存在
            File file = new File(uploadFile.getFilePath());

            if (file.exists()) {
                if (uploadFile.getFileStatus() == 1) {
                    //文件只上传了一部分
                    map = new HashMap<>(4);
                    map.put("flag", 1);
                    map.put("fileId", uploadFile.getFileId());
                    map.put("date", simpleDateFormat.format(new Date()));
                    map.put("msg", "文件上传了一部分");
                } else if (uploadFile.getFileStatus() == 2) {
                    //文件早已上传完整
                    map = new HashMap<>(2);
                    map.put("flag", 2);
                    map.put("msg", "文件已全部上传完成");
                }
            } else {//文件被删除
                map = new HashMap<>(3);
                map.put("flag", 0);
                map.put("fileId", uploadFile.getFileId());
                map.put("date", simpleDateFormat.format(new Date()));
            }
        }
        return map;
    }


    @Override
    public Map<String, Object> realUpload(FileForm form, MultipartFile multipartFile) throws Exception {
        Integer index = Integer.valueOf(form.getIndex());
        String action = form.getAction();
        String fileId = form.getUuid();
        String partMd5 = form.getPartMd5();
        Integer total = Integer.valueOf(form.getTotal());
        String fileName = form.getName();
        String suffix = NameUtil.getExtensionName(fileName);

        String saveDirectory = Constant.PATH + File.separator + fileId;
        //验证路径是否存在，不存在则创建目录
        File path = new File(saveDirectory);
        if (!path.exists()) {
            path.mkdirs();
        }
        //文件分片位置
        File file = new File(saveDirectory, fileId + "_" + index);
        //根据action不同执行不同操作. check:校验分片是否上传过; upload:直接上传分片
        Map<String, Object> map = null;
        if (file.exists()) {
            file.delete();
        }
        multipartFile.transferTo(new File(saveDirectory, fileId + "_" + index));
        map = new HashMap<>(2);
        map.put("flag", "1");
        map.put("fileId", fileId);
        if (!index.equals(total)) {
            return map;
        } else {
            File[] fileArray = path.listFiles();
            if (fileArray != null) {
                if (fileArray.length == total) {
                    map = new HashMap<>(2);
                    map.put("fileId", fileId);
                    map.put("flag", "2");
                    return map;
                }
            }
        }
        return map;
    }

    @Override
    public Map<String, Object> checkPartFile(FileForm form) {
        Integer index = Integer.valueOf(form.getIndex());
        String fileId = form.getUuid();
        String partMd5 = form.getPartMd5();
        String saveDirectory = Constant.PATH + File.separator + fileId;
        //验证路径是否存在，不存在则创建目录
        File path = new File(saveDirectory);
        if (!path.exists()) {
            path.mkdirs();
        }
        //文件分片位置
        File file = new File(saveDirectory, fileId + "_" + index);

        Map<String, Object> map = null;
        String md5Str = FileMd5Util.getFileMD5(file);
        if (md5Str != null && md5Str.length() == 31) {
            System.out.println("check length =" + partMd5.length() + " md5Str length" + md5Str.length() + "   " + partMd5 + " " + md5Str);
            md5Str = "0" + md5Str;
        }
        //分片已经上传且文件上传完整
        if (md5Str != null && md5Str.equals(partMd5)) {
            map = new HashMap<>(2);
            map.put("flag", "1");
            map.put("fileId", fileId);
        } else {
            //分片未上传
            map = new HashMap<>(2);
            map.put("flag", "0");
            map.put("fileId", fileId);
        }
        return map;
    }

    @Override
    public Map<String, Object> assembleFiles(String fileId, int total) throws IOException {
        Optional<UploadFile> byId = uploadFileRepository.findById(fileId);
        if (byId.get() == null) {
            return null;
        }
        String suffix = byId.get().getFileSuffix();
        Map<String, Object> map = null;
        String saveDirectory = Constant.PATH + File.separator + fileId;
        File path = new File(saveDirectory);
        File[] fileArray = path.listFiles();
        if (fileArray != null) {
            if (fileArray.length == total) {
                //分块全部上传完毕,合并
                File newFile = new File(saveDirectory, fileId + "." + suffix);
                //文件追加写入
                FileOutputStream outputStream = new FileOutputStream(newFile, true);
                byte[] byt = new byte[10 * 1024 * 1024];
                int len;
                //分片文件
                FileInputStream temp = null;
                for (int i = 0; i < total; i++) {
                    int j = i + 1;
                    temp = new FileInputStream(new File(saveDirectory, fileId + "_" + j));
                    while ((len = temp.read(byt)) != -1) {
                        outputStream.write(byt, 0, len);
                    }
                }
                //关闭流
                temp.close();
                outputStream.close();
                //修改FileRes记录为上传成功
                UploadFile uploadFile = byId.get();
                uploadFile.setFileId(fileId);
                uploadFile.setFileStatus(2);
                uploadFileRepository.save(uploadFile);

                map = new HashMap<>(2);
                map.put("fileId", fileId);
                map.put("flag", "2");
            }
        }
        return null;
    }
}
