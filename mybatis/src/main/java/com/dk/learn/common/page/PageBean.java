package com.dk.learn.common.page;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页参数实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageBean {
    
    /**
     * 当前页码
     */
    private Integer pageNum = 1;
    
    /**
     * 每页显示条数
     */
    private Integer pageSize = 10;
    
    /**
     * 总记录数
     */
    private Long total = 0L;
    
    /**
     * 总页数
     */
    private Integer pages = 0;
    
    /**
     * 计算偏移量（用于SQL的LIMIT OFFSET）
     * @return 偏移量
     */
    public Integer getOffset() {
        return (pageNum - 1) * pageSize;
    }
    
    /**
     * 根据总记录数计算总页数
     */
    public void calculatePages() {
        if (pageSize != null && pageSize > 0) {
            this.pages = total == 0 ? 0 : (int) ((total + pageSize - 1) / pageSize);
        } else {
            this.pages = 0;
        }
    }
    
    /**
     * 创建默认的分页对象
     * @return 默认分页对象
     */
    public static PageBean of() {
        return new PageBean();
    }
    
    /**
     * 创建指定页码和每页大小的分页对象
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页对象
     */
    public static PageBean of(Integer pageNum, Integer pageSize) {
        PageBean pageBean = new PageBean();
        pageBean.setPageNum(pageNum != null ? pageNum : 1);
        pageBean.setPageSize(pageSize != null ? pageSize : 10);
        return pageBean;
    }
}
