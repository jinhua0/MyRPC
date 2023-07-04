package com.jinhua.myRPC.codec;

import com.jinhua.myRPC.common.RPCRequest;
import com.jinhua.myRPC.common.RPCResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.AllArgsConstructor;

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
