package com.king.service;

import com.king.dao.impl.EmpDao;
import com.king.pojo.Emp;
import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class EmpServiceImp implements EmpService{
    @Autowired
    private EmpDao empDao;
    @Override
    public List<Emp> listEmp() {
        try {
            List<Emp> empList = empDao.listEmp();
            empList.stream().forEach(emp -> {
                String gender = emp.getGender();
                if ("1".equals(gender)) {
                    emp.setGender("男");
                } else if ("2".equals(gender)) {
                    emp.setGender("女");
                } else {
                    emp.setGender("未知");
                }
                String job = emp.getJob();
                if ("1".equals(job)) {
                    emp.setJob("教师");
                } else if ("2".equals(job)) {
                    emp.setJob("医生");
                } else if ("3".equals(job)) {
                    emp.setJob("程序员");
                } else {
                    emp.setJob("未知");
                }
            });
            return empList;
        } catch (DocumentException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
