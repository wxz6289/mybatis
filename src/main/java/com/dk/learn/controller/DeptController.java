package com.dk.learn.controller;

import com.dk.learn.common.page.PageResult;
import com.dk.learn.common.result.Result;
import com.dk.learn.dao.DeptName;
import com.dk.learn.entity.Dept;
import com.dk.learn.entity.DeptQuery;
import com.dk.learn.service.DeptService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/depts")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @GetMapping
    public Result<PageResult<Dept>> listDepts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) LocalDateTime createdTimeStart,
            @RequestParam(required = false) LocalDateTime createdTimeEnd) {
        log.info("查询部门数据...");
        log.info("page = {}, size = {}, name = {}, createdTimeStart = {}, createdTimeEnd = {}",
                page, size, name, createdTimeStart, createdTimeEnd);

        DeptQuery deptQuery = new DeptQuery();
        deptQuery.setName(name);
        deptQuery.setCreatedTimeStart(createdTimeStart);
        deptQuery.setCreatedTimeEnd(createdTimeEnd);
        deptQuery.setPageNum(page);
        deptQuery.setPageSize(size);

        PageHelper.startPage(page, size);
        List<Dept> depts = deptService.listByCondition(deptQuery);
        PageInfo<Dept> pageInfo = new PageInfo<>(depts);
        List<Dept> records = pageInfo.getList();

        PageResult<Dept> pageResult = new PageResult<>();
        pageResult.setRecords(records);
        pageResult.setTotal(pageInfo.getTotal());
        pageResult.setPage(pageInfo.getPageNum());
        pageResult.setSize(pageInfo.getPageSize());
        pageResult.setPages(pageInfo.getPages());
        return Result.ok(pageResult);
    }

    @PostMapping
    public Result<Void> addDepts(@RequestBody DeptName deptName) {
        log.info("保存部门 {}...", deptName);
        deptService.add(deptName.getName());
        return Result.ok(null);
    }

    @PutMapping(value = "/{id}")
    public Result<Void> updateDepts(@PathVariable("id") Long id, @RequestBody DeptName deptName) {
        log.info("更新部门 {}...", deptName);
        Dept dept = new Dept();
        dept.setId(id);
        dept.setName(deptName.getName());
        deptService.update(dept);
        return Result.ok(null);
    }

    @DeleteMapping(value = "/{id}")
    public Result<Void> removeDepts(@PathVariable("id") Long id) {
        log.info("删除部门 {}...", id);
        deptService.remove(id);
        return Result.ok(null);
    }
}