package com.jinhua.myRPC.server;

import com.jinhua.myRPC.common.RPCRequest;
import com.jinhua.myRPC.common.RPCResponse;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;

/**
 * 解析得到request请求，执行对应的服务方法，结果返回给客户端
 * 1、从request得到interfaceName
 * 2、根据interfaceName找到Map对应的实现类
 * 3、从request中得到方法名和参数，反射得到服务中的方法
 * 4、结果封装为response，写入socket
 */
@AllArgsConstructor
public class WorkThread implements Runnable {
    private Socket socket;

    private ServiceProvider serviceProvider;
    @Override
    public void run() {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            // 读取客户端请求中的request
            RPCRequest request = (RPCRequest) ois.readObject();
            // 反射得到返回值
            RPCResponse response = getResponse(request);
            //写入到客户端
            oos.writeObject(response);
            oos.flush();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("从IO中读取数据错误");
        }
    }

    private RPCResponse getResponse(RPCRequest request) {
        // 得到服务接口名
        String interfaceName = request.getInterfaceName();
        // 得到对应的实现类
        Object service = serviceProvider.getService(interfaceName);
        // 反射调用方法
        Method method = null;
        try {
            method = service.getClass().getMethod(request.getMethodName(),request.getParamsTypes());
            Object invoke = method.invoke(service, request.getParams());
            return RPCResponse.success(invoke);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            System.out.println("方法执行错误");
            return RPCResponse.fail();
        }
    }
}
