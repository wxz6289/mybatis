package com.king.controller;

import com.king.pojo.Emp;
import com.king.pojo.Result;
import com.king.service.EmpService;
import com.king.service.EmpServiceImp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EmpController {
    @Autowired
    private EmpService empService;

    @RequestMapping("/list-emp")
    public Result list(){
        List<Emp> empList = empService.listEmp();
        return Result.success(empList);
    }
}
