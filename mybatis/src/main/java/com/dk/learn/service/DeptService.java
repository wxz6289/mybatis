package com.dk.learn.service;

import com.dk.learn.entity.Dept;
import com.dk.learn.entity.DeptQuery;
import com.github.pagehelper.PageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.dk.learn.mapper.DeptMapper;
import java.util.List;

@Service
public class DeptService implements DeptMapper {
    @Autowired
    private DeptMapper deptMapper;
    private static final Logger log = LoggerFactory.getLogger(DeptService.class);
    @Override
    public Dept getDeptById(Integer id) {
        return null;
    }

    @Override
    public List<Dept> list() {
        return deptMapper.list();
    }

    @Override
    public void remove(Long id) {
        deptMapper.remove(id);
    }

    @Override
    public void add(String name) {
        deptMapper.add(name);
    }

    @Override
    public void update(Dept dept) {
        deptMapper.update(dept);
    }
    
    /**
     * 根据条件查询部门列表
     * @param query 查询条件
     * @return 部门列表
     */
    public List<Dept> listByCondition(DeptQuery query) {
        return deptMapper.listByCondition(query);
    }
}
