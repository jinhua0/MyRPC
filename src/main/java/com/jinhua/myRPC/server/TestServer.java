package com.jinhua.myRPC.server;

import com.jinhua.myRPC.service.BlogService;
import com.jinhua.myRPC.service.BlogServiceImpl;
import com.jinhua.myRPC.service.UserService;
import com.jinhua.myRPC.service.UserServiceImpl;

public class TestServer {
    public static void main(String[] args) {
        UserService userService = new UserServiceImpl();
        BlogService blogService = new BlogServiceImpl();

        ServiceProvider serviceProvider = new ServiceProvider("127.0.0.1", 8899);
        serviceProvider.provideServiceInterface(userService);
        serviceProvider.provideServiceInterface(blogService);

        RPCServer rpcServer = new NettyRPCServer(serviceProvider);
        rpcServer.start(8899);
    }
}
