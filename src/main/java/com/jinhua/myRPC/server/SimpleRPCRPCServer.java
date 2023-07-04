package com.jinhua.myRPC.server;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Java 原始的BIO监听，来一个任务，就new一个线程去处理
 * 处理任务的工作在WorkThread中
 */
public class SimpleRPCRPCServer implements RPCServer{
    // map存着服务接口名 -> service对象
    private ServiceProvider serviceProvider;

    public SimpleRPCRPCServer(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    public void start(int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("服务端启动了");
            // BIO的方式监听Socket
            while (true) {
                // 阻塞读取客户端请求
                Socket socket = serverSocket.accept();
                // 开启一个新线程去处理
                new Thread(new WorkThread(socket, serviceProvider)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("服务器启动失败");
        }
    }
}
