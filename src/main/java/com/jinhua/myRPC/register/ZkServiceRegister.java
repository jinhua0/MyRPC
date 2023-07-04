package com.jinhua.myRPC.register;

import com.jinhua.myRPC.LoadBalance.ConsistentHashLoadBalance;
import com.jinhua.myRPC.LoadBalance.LoadBalance;
import com.jinhua.myRPC.LoadBalance.RandomLoadBalance;
import com.jinhua.myRPC.common.RPCRequest;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 使用zookeeper作为服务注册和发现中心
 */
public class ZkServiceRegister implements ServiceRegister{
    // curator 提供的zookeeper客户端
    private CuratorFramework client;

    // zk的根路径节点
    private static final String ROOT_PATH = "MyRPC";

    // 初始化随机的负载均衡器
    private LoadBalance loadBalance = new ConsistentHashLoadBalance();

    // zk客户端初始化，并与zk服务端建立连接
    public ZkServiceRegister() {
        // 指数退避策略
        RetryPolicy policy = new ExponentialBackoffRetry(1000, 3);

        this.client = CuratorFrameworkFactory.builder().connectString("192.168.157.100:32189")
                .sessionTimeoutMs(40000).retryPolicy(policy).namespace(ROOT_PATH).build();
        this.client.start();
        System.out.println("zookeeper 连接成功");
    }


    @Override
    public void register(String serviceName, InetSocketAddress serverAddress) {
        try {
            if (client.checkExists().forPath("/" + serviceName) == null) {
                client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath("/" + serviceName);
            }
            String path = "/" + serviceName +"/"+ getServiceAddress(serverAddress);
            // 临时节点，服务器下线就删除节点
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path);

        } catch (Exception e) {
            System.out.println("此服务已经存在");
        }
    }

    // 服务发现
    @Override
    public InetSocketAddress serviceDiscovery(String serviceName, RPCRequest request) {
        try {
            List<String> strings = client.getChildren().forPath("/" + serviceName);
            String balance = loadBalance.doSelect(strings, request);
            return parseAddress(balance);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 地址 -> 192.168.157.100:32189
    private String getServiceAddress(InetSocketAddress serverAddress) {
        return serverAddress.getHostName() +
                ":" +
                serverAddress.getPort();
    }

    // 192.168.157.100:32189 -> 地址
    private InetSocketAddress parseAddress(String address) {
        String[] result = address.split(":");
        return new InetSocketAddress(result[0], Integer.parseInt(result[1]));
    }
}
