package com.king.controller;

import com.king.pojo.Result;
import com.king.pojo.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@RestController
public class HelloController {
    @RequestMapping("/hello")
    public Result hello(){
        return Result.success("Hello Spring Boot!");
    }

    @RequestMapping("/hello2")
    public Result hello2(@RequestParam(name = "name", required = false) String username, Integer age){
        return Result.success(Map.of("name", username, "age", age));
    }

    @RequestMapping("/hi")
    public Result hi(HttpServletRequest request){
        String name = request.getParameter("name");
        String age = request.getParameter("age");
        System.out.println("name: " + name + ", age: " + age);
        return Result.success(Map.of("name", name, "age", age));
    }


    @RequestMapping("/hi2")
    public String hi2(@RequestParam(name = "name", required = false) String username, Integer age) {
        System.out.println("name: " + username + ", age: " + age);
        return "Hi2 Spring Boot!";
    }

    @RequestMapping("/hi3")
    public String hi3(User user) {
        System.out.println("user: " + user);
        return "Hi3 Spring Boot!";
    }

    @RequestMapping("/array")
    public String hi3(String[] hobbies) {
        System.out.println("hobbies: " + String.join(", ", hobbies));
        return "Hi Array Spring Boot!";
    }

    @RequestMapping("/list")
    public String list( @RequestParam List<String> hobbies) {
        System.out.println("hobbies: " + String.join(", ", hobbies));
        return "Hi List Spring Boot!";
    }

    @RequestMapping("/dt")
    public String dt(@DateTimeFormat(pattern = "yyyy-M-d HH:mm:ss") LocalDateTime date) {
        System.out.println("datetime: " + date.toString());
        return "Hi Date Spring Boot!";
    }

    @RequestMapping("/json")
    public String json(@RequestBody User user) {
        System.out.println("user: " + user);
        return "Hi json Spring Boot!";
    }

    @RequestMapping("/hi/{id}")
    public String PathParam(@PathVariable Integer id) {
        System.out.println("id: " + id);
        return "Hi PathParam Spring Boot!";
    }
}
