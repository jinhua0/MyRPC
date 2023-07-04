package com.jinhua.myRPC.register;

import com.jinhua.myRPC.common.RPCRequest;

import java.net.InetSocketAddress;

/**
 * 实现两大基本功能
 * 1、注册：保存服务和地址
 * 2、服务发现：根据服务名查找地址
 */
public interface ServiceRegister {
    // 服务注册：服务名字+服务地址
    void register(String serviceName, InetSocketAddress serverAddress);

    // 服务发现：根据服务名字查询服务地址
    InetSocketAddress serviceDiscovery(String serviceName, RPCRequest request);
}
