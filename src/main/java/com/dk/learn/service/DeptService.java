package com.dk.learn.service;

import com.dk.learn.entity.Dept;
import com.dk.learn.entity.DeptQuery;
import com.dk.learn.mapper.DeptMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeptService {

    private final DeptMapper deptMapper;

    public Dept getDeptById(Integer id) {
        return deptMapper.getDeptById(id);
    }

    public List<Dept> list() {
        return deptMapper.list();
    }

    public void remove(Long id) {
        deptMapper.remove(id);
    }

    public void add(String name) {
        deptMapper.add(name);
    }

    public void update(Dept dept) {
        deptMapper.update(dept);
    }

    public List<Dept> listByCondition(DeptQuery query) {
        return deptMapper.listByCondition(query);
    }
}