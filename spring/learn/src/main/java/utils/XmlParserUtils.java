package utils;

import com.king.pojo.Emp;
import com.king.pojo.Employee;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class XmlParserUtils {

    public static List<Employee> parse(InputStream inputStream) throws DocumentException {
        List<Employee> employees = new ArrayList<>();

        SAXReader reader = new SAXReader();
        Document document = reader.read(inputStream);
        Element root = document.getRootElement();

        for (Element empElement : root.elements("emp")) {
            Employee employee = new Employee();

            employee.setId(Integer.parseInt(empElement.elementText("id")));
            employee.setName(empElement.elementText("name"));
            employee.setGender(Integer.parseInt(empElement.elementText("gender")));
            employee.setJob(Integer.parseInt(empElement.elementText("job")));
            employee.setAge(Integer.parseInt(empElement.elementText("age")));

            employees.add(employee);
        }

        return employees;
    }

    public static List<Emp> parse(String file, Class<Emp> empClass) throws DocumentException {
        List<Emp> emps = new ArrayList<>();

        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(XmlParserUtils.class.getClassLoader().getResourceAsStream(file));
            Element root = document.getRootElement();

            for (Element empElement : root.elements("emp")) {
                Emp emp = empClass.getDeclaredConstructor().newInstance();

                String genderText = empElement.elementText("gender");
                String jobText = empElement.elementText("job");

                if (genderText != null) {
                    emp.setGender(genderText);
                }
                if (jobText != null) {
                    emp.setJob(jobText);
                }

                emps.add(emp);
            }
        } catch (Exception e) {
            throw new DocumentException("Error parsing XML", e);
        }

        return emps;
    }
}
