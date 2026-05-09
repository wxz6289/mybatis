package com.dk.learn.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserVO extends User {
    @Getter
    @Setter
    private int total;

    public User getUser() {
        return new User(this);
    }

}
