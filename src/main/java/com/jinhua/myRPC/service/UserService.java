package com.jinhua.myRPC.service;

import com.jinhua.myRPC.common.User;

public interface UserService {
    User getUserByUserId(Integer id);

    Integer insertUserId(User user);

    String hello();
}
