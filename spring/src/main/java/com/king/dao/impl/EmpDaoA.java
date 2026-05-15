package com.king.dao.impl;

import com.king.pojo.Emp;
import org.dom4j.DocumentException;
import org.springframework.stereotype.Component;
import utils.XmlParserUtils;
import java.util.List;

@Component
public class EmpDaoA implements EmpDao {
    @Override
    public List<Emp> listEmp() throws DocumentException {
        List<Emp> empList = XmlParserUtils.parse("emp.xml", Emp.class);
        return empList;
    }
}
