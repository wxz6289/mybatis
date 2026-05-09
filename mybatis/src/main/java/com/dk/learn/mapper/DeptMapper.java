package com.dk.learn.mapper;

import com.dk.learn.dao.DeptName;
import com.dk.learn.entity.Dept;
import com.dk.learn.entity.DeptQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeptMapper {
    public Dept getDeptById(Integer id);

    public List<Dept> list();

    void remove(Long id);

    void add(String name);

    void update(Dept dept);
    
    /**
     * 根据条件查询部门列表
     * @param query 查询条件
     * @return 部门列表
     */
    List<Dept> listByCondition(@Param("query") DeptQuery query);
}
