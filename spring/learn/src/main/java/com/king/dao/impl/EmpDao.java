package com.king.dao.impl;

import com.king.pojo.Emp;
import org.dom4j.DocumentException;

import java.util.List;

public interface EmpDao {
    public List<Emp> listEmp() throws DocumentException;
}
