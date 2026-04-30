package com.dk.learn.mapper;

import com.dk.learn.entity.Dept;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeptMapper {
    public Dept getDeptById(Integer id);
}
