package com.dk.learn.common.util;

public final class FileUtils {

    private FileUtils() {
    }

    /**
     * 获取文件扩展名（不含点号），若无扩展名则返回空字符串
     */
    public static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}