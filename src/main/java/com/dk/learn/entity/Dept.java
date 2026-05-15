package com.dk.learn.entity;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Dept {
    private Long id;
    private String name;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    public Dept(String name) {
        this.name = name;
    }
}
