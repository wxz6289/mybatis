package com.dk.learn.mapper;

import com.dk.learn.entity.FileInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 文件信息Mapper
 */
@Mapper
public interface FileInfoMapper {
    
    /**
     * 保存文件信息
     * @param fileInfo 文件信息
     * @return 影响行数
     */
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    @Insert("INSERT INTO file_info (original_name, stored_name, file_size, content_type, extension, " +
            "file_path, file_url, uploaded_by, upload_time, description) " +
            "VALUES (#{originalName}, #{storedName}, #{fileSize}, #{contentType}, #{extension}, " +
            "#{filePath}, #{fileUrl}, #{uploadedBy}, #{uploadTime}, #{description})")
    int save(FileInfo fileInfo);
    
    /**
     * 根据ID查询文件信息
     * @param id 文件ID
     * @return 文件信息
     */
    @Select("SELECT * FROM file_info WHERE id = #{id} AND deleted = 0")
    FileInfo findById(Long id);
    
    /**
     * 根据存储文件名查询
     * @param storedName 存储文件名
     * @return 文件信息
     */
    @Select("SELECT * FROM file_info WHERE stored_name = #{storedName} AND deleted = 0")
    FileInfo findByStoredName(String storedName);
    
    /**
     * 查询所有文件列表（分页）
     * @param offset 偏移量
     * @param size 每页大小
     * @return 文件列表
     */
    @Select("SELECT * FROM file_info WHERE deleted = 0 ORDER BY upload_time DESC LIMIT #{offset}, #{size}")
    List<FileInfo> list(@Param("offset") int offset, @Param("size") int size);
    
    /**
     * 统计文件总数
     * @return 总数
     */
    @Select("SELECT COUNT(*) FROM file_info WHERE deleted = 0")
    long count();
    
    /**
     * 根据条件查询文件
     */
    @Select("<script>" +
            "SELECT * FROM file_info WHERE deleted = 0" +
            "<if test='originalName != null and originalName != \"\"'>" +
            "  AND original_name LIKE CONCAT('%', #{originalName}, '%')" +
            "</if>" +
            "<if test='contentType != null and contentType != \"\"'>" +
            "  AND content_type = #{contentType}" +
            "</if>" +
            " ORDER BY upload_time DESC" +
            "<if test='offset != null and size != null'>" +
            "  LIMIT #{offset}, #{size}" +
            "</if>" +
            "</script>")
    List<FileInfo> listByCondition(@Param("originalName") String originalName,
                                    @Param("contentType") String contentType,
                                    @Param("offset") Integer offset,
                                    @Param("size") Integer size);

    /**
     * 根据条件统计文件总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM file_info WHERE deleted = 0" +
            "<if test='originalName != null and originalName != \"\"'>" +
            "  AND original_name LIKE CONCAT('%', #{originalName}, '%')" +
            "</if>" +
            "<if test='contentType != null and contentType != \"\"'>" +
            "  AND content_type = #{contentType}" +
            "</if>" +
            "</script>")
    long countByCondition(@Param("originalName") String originalName,
                          @Param("contentType") String contentType);
    
    /**
     * 逻辑删除文件
     * @param id 文件ID
     * @return 影响行数
     */
    @Update("UPDATE file_info SET deleted = 1 WHERE id = #{id}")
    int deleteById(Long id);
    
    /**
     * 批量逻辑删除
     * @param ids 文件ID列表
     * @return 影响行数
     */
    @Update("<script>" +
            "UPDATE file_info SET deleted = 1 WHERE id IN " +
            "<foreach item='id' collection='ids' separator=',' open='(' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchDelete(@Param("ids") List<Long> ids);
}
