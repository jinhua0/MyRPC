# MyRPC

手写一个RPC，通过搭建一个简易的RPC来学习RPC框架的一个基本原理

主要的几个技术点如下
## 反射，动态代理
### 客户端通过动态代理封装request对象：
```java
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
```
 
### 服务端通过反射调用对应服务的方法返回response:
```java
/**
     * 根据请求调用对应的方法返回response
     * @return
     */
    RPCResponse getResponse(RPCRequest request) {
        // 得到服务名
        String interfaceName = request.getInterfaceName();
        // 得到服务端相应服务实现类
        Object service = serviceProvider.getService(interfaceName);
        // 反射调用方法
        Method method = null;
        try {
            method = service.getClass().getMethod(request.getMethodName(), request.getParamsTypes());
            Object invoke = method.invoke(service, request.getParams());
            return RPCResponse.success(invoke);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            System.out.println("方法执行错误");
            return RPCResponse.fail();
        }
    }
```

## Netty的使用
客户端使用Netty的代码结构：
- RPCClient: 不同网络连接，网络传输方式的客户端分别实现这个接口
- NettyRPCClient：客户端使用Netty的实现类
- RPCClientProxy：动态代理封装request对象，并且利用NettyRPCClient对象，负责与服务端通信
 
### Netty 服务端的实现
```java
/**
 * Nettty版的RPC 服务端
 */
@AllArgsConstructor
public class NettyRPCServer implements RPCServer{
    private ServiceProvider serviceProvider;
    @Override
    public void start(int port) {
        // netty 服务线程组boss负责建立连接， work负责具体的请求
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workGroup = new NioEventLoopGroup();
        System.out.println("Netty服务端启动了...端口号为" + port );
        try {
            /// 启动netty服务器
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            // 初始化
            serverBootstrap.group(bossGroup,workGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new NettyServerInitializer(serviceProvider));
            // 阻塞等待端口绑定成功
            ChannelFuture channelFuture = serverBootstrap.bind(port).syncUninterruptibly();

            // 阻塞等待通道关闭
            channelFuture.channel().closeFuture().syncUninterruptibly();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }
}
```
### Netty客户端的实现：
```java
public class NettyRPCClient implements RPCClient{
    // 客户端与服务端通道初始化
    private static final Bootstrap bootstrap;

    // 用于处理客户端通道的所有事件
    private static final EventLoopGroup eventLoopGroup;

    private String host;
    private int port;
    private ServiceRegister serviceRegister;
    public NettyRPCClient() {
        this.serviceRegister = new ZkServiceRegister();
    }
    // netty客户端初始化，通道初始化
    static {
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                .handler(new NettyClientInitializer());
    }
    @Override
    public RPCResponse sendRequest(RPCRequest request) {
        // 通过接口名字找到服务地址
        InetSocketAddress address = serviceRegister.serviceDiscovery(request.getInterfaceName(), request);
        host = address.getHostName();
        port = address.getPort();
        try {
            // 阻塞等待连接建立
            ChannelFuture channelFuture  = bootstrap.connect(host, port).syncUninterruptibly();
            Channel channel = channelFuture.channel();
            // 发送数据
            channel.writeAndFlush(request);
            channel.closeFuture().syncUninterruptibly();
            // 阻塞的获得结果，通过给channel设计别名，获取特定名字下的channel中的内容（这个在hanlder中设置）
            // AttributeKey是，线程隔离的，不会由线程安全问题。
            AttributeKey<RPCResponse> key = AttributeKey.valueOf("RPCResponse");
            RPCResponse response = channel.attr(key).get();

            System.out.println(response);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
```

总结一下，阻塞和非阻塞描述的是网络IO数据拷贝的第一个阶段（数据准备阶段），同步和异步描述的是第二个阶段（数据从内核拷贝到用户中）。
- 阻塞：线程一直等待数据准备好，期间什么都不干，但是会让出CPU。
- 非阻塞：发起网络IO请求的时候会立即返回去干别的事情，但是会不断地进行询问数据是否准备好（轮询）。
- 同步：数据从内核拷贝到用户时，发起该请求的线程会自己来拷贝数据。
- 异步：数据从内核拷贝到用户时，发起该请求的线程不会自己来拷贝数据，而是由其他线程来完成。 

这些概念之间并不矛盾，可以根据实际情况选择使用。例如，在Netty中，它采用NIO实现同步非阻塞IO，并且它是一个异步事件驱动框架。

## 序列化反序列化
### Java原生的序列化方式
```java
public class ObjectSerializer implements Serializer{
    // 利用java IO 对象 -> 字节数组
    @Override
    public byte[] serialize(Object obj) {
        byte[] bytes = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            bytes = bos.toByteArray();
            oos.close();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bytes;
    }

    // 字节数组 -> 对象
    @Override
    public Object deserialize(byte[] bytes, int messageType) {
        Object obj = null;
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        try {
            ObjectInputStream ois = new ObjectInputStream(bis);
            obj = ois.readObject();
            ois.close();
            bis.close();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return obj;
    }

    // 0 代表java原生的序列化器
    @Override
    public int getType() {
        return 0;
    }
}
```
### Json序列化器
```java
/**
 * 序列化的时候将对象转化为了字符串，丢失了类信息，
 * 反序列化的时候，需要根据类信息把Json -> 对应对象
 */
public class JsonSerializer implements Serializer{
    @Override
    public byte[] serialize(Object obj) {
        byte[] bytes = JSONObject.toJSONBytes(obj);
        return bytes;
    }

    @Override
    public Object deserialize(byte[] bytes, int messageType) {
        Object obj = null;
        // 传输的消息分为request与response
        switch (messageType) {
            case 0:
                RPCRequest request = JSON.parseObject(bytes, RPCRequest.class);

                // 判空
                if (request.getParams() == null) {
                    return request;
                }

                Object[] objects = new Object[request.getParams().length];
                // 把json字符串转化为对应的对象
                for (int i = 0; i < objects.length; i++) {
                    Class<?> paramsType = request.getParamsTypes()[i];
                    if (!paramsType.isAssignableFrom(request.getParams()[i].getClass())) {
                        objects[i] = JSONObject.toJavaObject((JSONObject) request.getParams()[i],request.getParamsTypes()[i]);
                    } else {
                        objects[i] = request.getParams()[i];
                    }
                }
                request.setParams(objects);
                obj = request;
                break;

            case 1:
                RPCResponse response = JSON.parseObject(bytes, RPCResponse.class);
                Class<?> dataType = response.getDataType();
                if(! dataType.isAssignableFrom(response.getData().getClass())){
                    response.setData(JSONObject.toJavaObject((JSONObject) response.getData(),dataType));
                }
                obj = response;
                break;
            default:
                System.out.println("暂时不支持此种消息");
                throw new RuntimeException();
        }
        return obj;
    }

    // 1:代表json序列化方式
    @Override
    public int getType() {
        return 1;
    }
}
```
### Hessian序列化
```java
/**
 *  Hessian 是基于二进制的序列化协议
 *  利用自带的 API 实现序列化 与 反序列化
 */
public class HessianSerializer implements Serializer{
    @Override
    public byte[] serialize(Object obj) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            HessianOutput hessianOutput = new HessianOutput(byteArrayOutputStream);
            hessianOutput.writeObject(obj);

            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Object deserialize(byte[] bytes, int messageType) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes)) {
            HessianInput hessianInput = new HessianInput(byteArrayInputStream);
            Object o = hessianInput.readObject();

            return o;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int getType() {
        return 2;
    }
}
```

## 自定义编码解码
我们先初步对自定义格式设计，先读取消息类型(Request, Response), 序列化方式(原生，json, Hessian),再加上消息长度：防止粘包，最后再根据数据长度读取数据data
### 编码
```java
/**
 * 按照自定义的消息格式写入
 * 需要一个序列化器，将对象序列化为字节数组
 */
@AllArgsConstructor
public class MyEncode extends MessageToByteEncoder {
    private Serializer serializer;
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf byteBuf) throws Exception {
        // 写入消息类型
        if (o instanceof RPCRequest) {
            byteBuf.writeShort(MessageType.REQUEST.getCode());
        } else if (o instanceof RPCResponse) {
            byteBuf.writeShort(MessageType.RESPONSE.getCode());
        }
        // 写入序列化方式
        byteBuf.writeShort(serializer.getType());
        byte[] serialize = serializer.serialize(o);
        //写入长度
        byteBuf.writeInt(serialize.length);
        //写入序列化字节数组
        byteBuf.writeBytes(serialize);

    }
}
```
### 解码
```java
/**
 * 按照自定义编码格式进行解码
 */
public class MyDecode extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) throws Exception {
        // 1. 读取消息类型
        short messageType = in.readShort();
        // 现在还只支持request与response请求
        if(messageType != MessageType.REQUEST.getCode() &&
                messageType != MessageType.RESPONSE.getCode()){
            System.out.println("暂不支持此种数据");
            return;
        }
        // 2. 读取序列化的类型
        short serializerType = in.readShort();
        // 根据类型得到相应的序列化器
        Serializer serializer = Serializer.getSerializerByCode(serializerType);
        if(serializer == null)throw new RuntimeException("不存在对应的序列化器");
        // 3. 读取数据序列化后的字节长度
        int length = in.readInt();
        // 4. 读取序列化数组
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        // 用对应的序列化器解码字节数组
        Object deserialize = serializer.deserialize(bytes, messageType);
        out.add(deserialize);
    }
}
```

## Zookeeper的使用
注册中心（如zookeeper）的地址是固定的（为了高可用一般是集群，我们看做黑盒即可）， 服务端上线时，在注册中心注册自己的服务与对应的地址，而客户端调用服务时，去注册中心根据服务名找到对应的服务端地址。

zookeeper我们可以近似看作一个树形目录文件系统，是一个分布式协调应用，其它注册中心有EureKa， Nacos等


这里我们引入Curator客户端
```xml
<!--这个jar包应该依赖log4j,不引入log4j会有控制台会有warn，但不影响正常使用-->
<dependency>
    <groupId>org.apache.curator</groupId>
    <artifactId>curator-recipes</artifactId>
    <version>5.1.0</version>
</dependency>
```
### zookeeper服务注册接口实现类
```java
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
```
因为zookeeper可以看作树形目录文件系统，"MyRPC" -> "服务名" -> "服务地址"，我们可以遍历服务名这个节点下的服务地址节点，来查看这个服务地址是否还在线。


## 负载均衡

### 随机的负载均衡策略
```java
/**
 * 随机的负载均衡策略
 */
public class RandomLoadBalance implements LoadBalance{
    @Override
    public String doSelect(List<String> addressList, RPCRequest request) {
        Random random = new Random();
        int choose = 0;
        if (addressList.size() > 0) {
            choose = random.nextInt(addressList.size());
        }
        System.out.println("负载均衡选择了" + choose + "服务器," + addressList.get(choose));
        return addressList.get(choose);
    }
}
```
基于权值的随机算法：  

在基于权值的随机负载均衡算法中，每个请求都会根据服务节点的权值随机选择一个服务节点。具体来说，算法会计算所有服务节点权值的总和，然后生成一个随机数，该随机数在0到权值总和之间。接着，算法会遍历所有服务节点，累加它们的权值，直到累加和大于等于随机数为止。最后选择的服务节点就是使累加和大于等于随机数的第一个服务节点。 
权重值就相当于服务节点被选中的概率。
### 轮询的负载均衡策略
```java
/**
 * 轮询的负载均衡策略
 */
public class RoundLoadBalance implements LoadBalance{
    private int choose = -1;
    @Override
    public String doSelect(List<String> addressList, RPCRequest request) {
        choose++;
        choose = choose % addressList.size();
        return addressList.get(choose);
    }
}
```
基于权值的轮询算法： 

算法会计算所有服务节点权值的最大公约数，并将每个服务节点的权值除以最大公约数得到一个新的权值。然后，算法会根据新的权值来调整指针的移动速度。例如，假设有两个服务节点A和B，它们的权值分别为1和3。那么，在基于权值的轮询负载均衡算法中，指针会先移动到服务节点A，然后连续移动到服务节点B三次，然后再次移动到服务节点A。这样就可以根据服务节点的处理能力来分配请求。
### 一致性哈希的负载均衡策略
```java
/**
 * 一致性哈希算法，就是先将服务节点映射到[0,2^32-1]的哈希环上，再将新请求的key值映射到哈希环上，该请求就会选择比这个key值大的最近的一个服务节点
 */
public class ConsistentHashLoadBalance implements LoadBalance{
    // 使用 ConcurrentHashMap来存储不同的服务对应ConsistentHashSelector实例
    private final ConcurrentHashMap<String, ConsistentHashSelector> selectors = new ConcurrentHashMap<>();

    @Override
    public String doSelect(List<String> addressList, RPCRequest request) {
        // 获取 serviceAddresses 的哈希码
        int identityHashCode = System.identityHashCode(addressList);
        String interfaceName = request.getInterfaceName();

        // 根据服务的名字找对应的 ConsistentHashSelector 实例
        ConsistentHashSelector selector = selectors.get(interfaceName);
        // 检查是否需要更新 ConsistentHashSelector 实例
        if (selector == null || selector.identityHashCode != identityHashCode) {
            // 如果 selector 不存在或者哈希码不匹配，创建一个新的 ConsistentHashSelector 实例并存入 selectors 中
            selectors.put(interfaceName, new ConsistentHashSelector(addressList, 160, identityHashCode));
            selector = selectors.get(interfaceName);
        }
        // 调用 selector 的 select 方法进行选择，并返回结果
        return selector.select(interfaceName + Arrays.stream(request.getParams()));
    }

    static class ConsistentHashSelector {
        // 使用TreeMap来存储虚拟节点和真实节点之间的映射关系
        private final TreeMap<Long, String> virtualInvokers;

        // 用于标识ConsistentHashSelector实例的哈希码
        private final int identityHashCode;

        ConsistentHashSelector(List<String> invokers, int replicaNumber, int identityHashCode) {
            this.virtualInvokers = new TreeMap<>();
            this.identityHashCode = identityHashCode;

            // 遍历 invokers 列表，为每个 invoker 创建 replicaNumber/4 个虚拟节点，并将虚拟节点和真实节点的映射关系存入 virtualInvokers中
            for (String invoker : invokers) {
                for (int i = 0; i < replicaNumber / 4; i++) {
                    byte[] digest = md5(invoker + i);
                    for (int h = 0; h < 4; h++) {
                        long m = hash(digest, h);
                        // 哈希值 为key , 服务地址为 value 放在TreeMap数据结构中
                        virtualInvokers.put(m, invoker);
                    }
                }
            }
        }

        // 计算字符串的 MD5 哈希值
        static byte[] md5(String key) {
            MessageDigest md;
            try {
                // 获取 MD5消息摘要 算法实例
                md = MessageDigest.getInstance("MD5");
                byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
                // 使用MD5算法对字节数组进行哈希计算
                md.update(bytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            return md.digest();
        }

        // 计算哈希值
        private long hash(byte[] digest, int number) {
            // 这个就是将字符串的MD5哈希值的字节数组，按4个字节转化为一个32b的整数最后合并起来形成一个long的哈希值
            return (((long) (digest[3 + number * 4] & 0xFF) << 24)
                    | ((long) (digest[2 + number * 4] & 0xFF) << 16)
                    | ((long) (digest[1 + number * 4] & 0xFF) << 8)
                    | (digest[number * 4] & 0xFF))
                    & 0xFFFFFFFFL;
        }

        // 根据 rpcServiceKey 选择一个服务地址
        public String select(String rpcServiceKey) {
            byte[] digest = md5(rpcServiceKey);
            return selectForKey(hash(digest, 0));
        }

        // 根据哈希值选择一个服务地址
        public String selectForKey(long hashCode) {
            Map.Entry<Long, String> entry = virtualInvokers.tailMap(hashCode, true).firstEntry();

            if (entry == null) {
                entry = virtualInvokers.firstEntry();
            }

            return entry.getValue();
        }

    }
}
```

一致性哈希算法：  

一致性哈希算法通过将服务节点和键映射到一个哈希环上来实现负载均衡和容错。首先，它使用哈希函数将每个服务节点映射到哈希环上的一个位置。然后，当需要查找一个键对应的服务节点时，它使用相同的哈希函数计算该键的哈希值，并在哈希环上查找离该哈希值最近的服务节点。 

这种方法可以保证当服务节点发生变化时，只有很少的键需要重新映射到新的服务节点上。例如，当添加或删除一个服务节点时，只有该服务节点附近的一小部分键需要重新映射，而其他键仍然映射到原来的服务节点上。这样就可以实现负载均衡和容错。