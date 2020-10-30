package com.supereal.bigfile.form;

import lombok.Data;

/**
 * Create by tianci
 * 2019/1/10 16:33
 */
@Data
public class FileForm {

    private String md5;

    private String uuid;

    private String date;

    private String name;

    private String size;

    private String total;

    private String index;

    private String action;

    private String partMd5;

    @Override
    public String toString() {
        return "FileForm{" +
                "md5='" + md5 + '\'' +
                ", uuid='" + uuid + '\'' +
                ", date='" + date + '\'' +
                ", name='" + name + '\'' +
                ", size='" + size + '\'' +
                ", total='" + total + '\'' +
                ", index='" + index + '\'' +
                ", action='" + action + '\'' +
                ", partMd5='" + partMd5 + '\'' +
                '}';
    }
}
