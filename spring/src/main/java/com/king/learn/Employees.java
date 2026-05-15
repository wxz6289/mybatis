package com.king.learn;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Properties;

public class Employees {
    List ages;
    Set employeeID;
    Map employeeNameID;
    Properties employeeNameAge;

    public List getAges() {
        System.out.println("Ages: " + ages);
        return ages;
    }

    public void setAges(List ages) {
        this.ages = ages;
    }

    public Set getEmployeeID() {
        System.out.println("Employee IDs: " + employeeID);
        return employeeID;
    }

    public void setEmployeeID(Set employeeID) {
        this.employeeID = employeeID;
    }

    public Map getEmployeeNameID() {
        System.out.println("Employee Name IDs: " + employeeNameID);

        return employeeNameID;
    }

    public void setEmployeeNameID(Map employeeNameID) {
        this.employeeNameID = employeeNameID;
    }

    public Properties getEmployeeNameAge() {
        System.out.println("Employee Name Ages: " + employeeNameAge);
        return employeeNameAge;
    }

    public void setEmployeeNameAge(Properties employeeNameAge) {
        this.employeeNameAge = employeeNameAge;
    }
}