package com.king.learn;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class XmlBeanCollections {
    public static void main(String[] args) {
       ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("beans-collections.xml");
       Employees employees = context.getBean("employees", Employees.class);
       employees.getAges();
       employees.getEmployeeID();
       employees.getEmployeeNameID();
       employees.getEmployeeNameAge();
    }
}