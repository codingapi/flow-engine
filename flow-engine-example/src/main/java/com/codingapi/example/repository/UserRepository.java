package com.codingapi.example.repository;

import com.codingapi.example.entity.User;
import com.codingapi.springboot.fast.jpa.repository.FastRepository;

import java.util.List;

public interface UserRepository extends FastRepository<User,Long> {

    User getUserById(long id);

    List<User> findUserByIdIn(List<Long> ids);

    User getUserByAccount(String account);

}
