package com.jinhua.myRPC.client;

import com.jinhua.myRPC.codec.*;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import lombok.AllArgsConstructor;

/**
 * 初始化，主要负责序列化的编码解码，解决netty的粘包问题
 */
@AllArgsConstructor
public class NettyClientInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();
        // 使用自定义的编解码器
        pipeline.addLast(new MyDecode());
        // 编码需要传入序列化器
        pipeline.addLast(new MyEncode(new HessianSerializer()));
        pipeline.addLast(new NettyClientHandler());
    }
}
