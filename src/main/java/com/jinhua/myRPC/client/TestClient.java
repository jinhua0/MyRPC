package com.jinhua.myRPC.client;

import com.jinhua.myRPC.common.Blog;
import com.jinhua.myRPC.common.User;
import com.jinhua.myRPC.service.BlogService;
import com.jinhua.myRPC.service.UserService;

public class TestClient {
    public static void main(String[] args) {
        // 构建socket传输的客户端
        RPCClient rpcClient = new NettyRPCClient();
        // 使用该客户端的代理客户端
        RPCClientProxy rpcClientProxy = new RPCClientProxy(rpcClient);
        // 代理客户端根据不同的服务获取代理类
        UserService userService = rpcClientProxy.getProxy(UserService.class);
        User userByUserId = userService.getUserByUserId(10);
        System.out.println("从服务端得到的user为：" + userByUserId);

        User user = User.builder().userName("张三").id(100).sex(true).build();
        Integer integer = userService.insertUserId(user);
        System.out.println("向服务端插入数据："+integer);
        try {
            // 暂停执行3秒
            Thread.sleep(10000);
            System.out.println("我停了10秒");
        } catch (InterruptedException e) {
            // 如果线程被中断，则抛出异常
            e.printStackTrace();
        }

        BlogService blogService = rpcClientProxy.getProxy(BlogService.class);

        Blog blogById = blogService.getBlogById(10000);
        System.out.println("从服务端得到的blog为：" + blogById);
        // 测试json调用空参数方法
        System.out.println(userService.hello());
    }
}
