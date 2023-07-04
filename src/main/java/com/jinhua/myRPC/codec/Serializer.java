package com.jinhua.myRPC.codec;

public interface Serializer {
    // 把对象序列化成字节数组
    byte[] serialize(Object obj);

    // 反序列化的时候，java自带的序列化方式自带消息类型
    // 其他方式反序列化的时候需要把message转化为相应的对象
    Object deserialize(byte[] bytes, int messageType);

    // 0:java自带序列化方式，1：json序列化方式
    int getType();

    // 还有其它的序列化方式实现这个接口
    static Serializer getSerializerByCode(int code) {
        switch (code) {
            case  0:
                return new ObjectSerializer();
            case 1:
                return new JsonSerializer();
            case 2:
                return new HessianSerializer();
            default:
                return null;
        }
    }
}
