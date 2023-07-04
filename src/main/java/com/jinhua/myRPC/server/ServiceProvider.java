package com.jinhua.myRPC.server;

import com.jinhua.myRPC.register.ServiceRegister;
import com.jinhua.myRPC.register.ZkServiceRegister;
import sun.util.locale.provider.LocaleServiceProviderPool;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * 存放服务接口名与服务对应的实现类
 */
public class ServiceProvider {
    // 一个接口可能有多个实现类
    private Map<String, Object> interfaceProvider;

    // 服务注册
    private ServiceRegister serviceRegister;

    private String host;
    private int port;

    public ServiceProvider(String host, int port) {
        this.host = host;
        this.port = port;
        this.interfaceProvider = new HashMap<>();
        this.serviceRegister = new ZkServiceRegister();
    }

    public void provideServiceInterface(Object service) {
        Class<?>[] interfaces = service.getClass().getInterfaces();

        for (Class clazz : interfaces) {
            // 添加到映射表
            interfaceProvider.put(clazz.getName(), service);
            // 在注册中心注册服务
            serviceRegister.register(clazz.getName(), new InetSocketAddress(host, port));
        }
    }

    public Object getService(String interfaceName) {
        return interfaceProvider.get(interfaceName);
    }
}
