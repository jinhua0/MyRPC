package com.jinhua.myRPC.client;

import com.jinhua.myRPC.common.RPCRequest;
import com.jinhua.myRPC.common.RPCResponse;
import com.jinhua.myRPC.register.ServiceRegister;
import com.jinhua.myRPC.register.ZkServiceRegister;

import java.io.IOError;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class SimpleRPCClient implements RPCClient{
    private String host;

    private int port;

    // zk服务发现
    private ServiceRegister serviceRegister;

    public SimpleRPCClient() {
        // 初始化zk
        this.serviceRegister = new ZkServiceRegister();
    }

    // Socket建立连接，发起请求Request,得到响应Response
    @Override
    public RPCResponse sendRequest(RPCRequest request) {
        // 从注册中心获取host, port
        InetSocketAddress address = serviceRegister.serviceDiscovery(request.getInterfaceName(), request);
        host = address.getHostName();
        port = address.getPort();
        try {
            Socket socket = new Socket(host, port);

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            System.out.println(request);
            objectOutputStream.writeObject(request);
            objectOutputStream.flush();

            RPCResponse rpcResponse = (RPCResponse) objectInputStream.readObject();

            return rpcResponse;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println();
            return null;
        }
    }
}
