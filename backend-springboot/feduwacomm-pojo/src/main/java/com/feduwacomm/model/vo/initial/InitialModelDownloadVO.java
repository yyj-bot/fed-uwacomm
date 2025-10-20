package com.feduwacomm.model.vo.initial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 初始模型下载载荷
 * 支持文件资源或内存内容两种形式。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitialModelDownloadVO {

    /**
     * 是否为直接文件资源（基于存储路径）
     */
    private boolean fileResource;

    /**
     * 下载文件名
     */
    private String filename;

    /**
     * 内容类型（MIME）
     */
    private String contentType;

    /**
     * 内容长度（字节）
     */
    private long contentLength;

    /**
     * 文件系统路径（当 fileResource=true 时有效）
     */
    private String filePath;

    /**
     * 内存中的模型内容（二进制），当 fileResource=false 时有效
     */
    private byte[] inlineContent;
}
