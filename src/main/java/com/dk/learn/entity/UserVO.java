package com.dk.learn.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserVO extends User {
    private int total;

    @JsonIgnore
    public User getUser() {
        return new User(this.getId(), this.getName(), this.getAge(), this.getDeptId(), this.getCreatedTime(), this.getUpdatedTime());
    }
}