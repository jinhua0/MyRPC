package com.jinhua.myRPC.client;

import com.jinhua.myRPC.common.RPCRequest;
import com.jinhua.myRPC.common.RPCResponse;
import lombok.AllArgsConstructor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@AllArgsConstructor
public class RPCClientProxy implements InvocationHandler {

    private  RPCClient client;

    // jdk动态代理，每次代理对象调用方法，会经过此方法增强
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // request的构建
        RPCRequest request = RPCRequest.builder().interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .params(args).paramsTypes(method.getParameterTypes()).build();
        // 数据传输
        RPCResponse rpcResponse = client.sendRequest(request);
        return rpcResponse.getData();
    }

    // client代理对象
    <T>T getProxy(Class<T> clazz) {
        Object o = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, this);
        return (T)o;
    }
}
