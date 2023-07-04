package com.jinhua.myRPC.service;

import com.jinhua.myRPC.common.User;

public class UserServiceImpl implements UserService {
    private int count = -1;

    @Override
    public User getUserByUserId(Integer id) {
        // 模拟数据库读取数据
        User user = User.builder().id(id).userName("jinhua").sex(true).build();
        System.out.println("客户端查询了"+id+"用户");
        return user;
    }

    @Override
    public Integer insertUserId(User user) {
        System.out.println("插入数据成功："+user);
        count++;
        return count % 100;
    }

    @Override
    public String hello() {
        return "Hello World!";
    }
}
