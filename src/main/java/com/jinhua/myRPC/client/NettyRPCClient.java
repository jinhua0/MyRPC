package com.jinhua.myRPC.client;

import com.jinhua.myRPC.common.RPCRequest;
import com.jinhua.myRPC.common.RPCResponse;
import com.jinhua.myRPC.register.ServiceRegister;
import com.jinhua.myRPC.register.ZkServiceRegister;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;

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
