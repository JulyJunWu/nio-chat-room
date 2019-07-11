package com.ws.chatroom;

import com.alibaba.fastjson.JSON;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NIO
 * 聊天室服务端
 */
public class NioServer {

    private Selector selector = null;

    private Charset charset = Charset.forName("UTF-8");

    //存放用户昵称
    private Map<Channel, String> userMap = new ConcurrentHashMap<>();

    public void start() {

        try {
            //打开一个socket , 相当于一个ServerSocket
            ServerSocketChannel channel = ServerSocketChannel.open();
            //设置为非堵塞
            channel.configureBlocking(false);
            channel.socket().bind(new InetSocketAddress(8888));

            selector = Selector.open();
            //将channel注册到selector中,以便于selector监听观察到新变化
            channel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("<<<<<服务器启动成功>>>>>");

            while (true) {
                //在此处堵塞监听(也可以设置指定的时间) channel的变化,是否有新的请求, 不同于socket只能一对一对 , 此处一个selector可以监听多个channel
                int select = selector.select();

                if (select == 0) continue;

                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()) {
                        handlerAccept(key);
                    } else if (key.isReadable()) {
                        handleRead(key, selector);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理新加入的channel请求
     *
     * @param selectionKey
     */
    private void handlerAccept(SelectionKey selectionKey) {
        try {
            ServerSocketChannel channel = (ServerSocketChannel) selectionKey.channel();
            SocketChannel socketChannel = channel.accept();
            socketChannel.configureBlocking(false);

            //注册状态改变
            socketChannel.register(selector, SelectionKey.OP_READ);
            socketChannel.write(charset.encode("系统:你已加入聊天室!"));

        } catch (Exception e) {
            selectionKey.cancel();
        }
    }

    /**
     * 处理channel 数据请求
     *
     * @param selectionKey
     */
    private void handleRead(SelectionKey selectionKey, Selector selector) {
        try {
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            SocketChannel channel = (SocketChannel) selectionKey.channel();

            StringBuilder sb = new StringBuilder();

            while (channel.read(byteBuffer) > 0) {
                byteBuffer.flip();
                sb.append(charset.decode(byteBuffer));
            }

            ChannelEventWrapper eventWrapper = JSON.parseObject(sb.toString(), ChannelEventWrapper.class);

            sb.delete(0, sb.length());
            String message;

            switch (eventWrapper.getChannelEvent()) {
                case REGISTER:
                    userMap.put(channel, eventWrapper.getMessage());
                    //将消息发送给房间内所有人
                    message = sb.append("系统:用户[").append(eventWrapper.getMessage()).append("]加入聊天室!\r").toString();
                    broadcast(selector, channel, message);
                    System.out.println(message);
                    broadcastLiveNum();
                    break;
                case WRITE:
                case READ:
                    //将消息发送给房间内所有人
                    message = sb.append(userMap.get(channel)).append(":").append(eventWrapper.getMessage()).toString();
                    broadcast(selector, channel, message);
                    System.out.println(message);
            }

        } catch (Exception e) {
            selectionKey.cancel();
            String name = userMap.get(selectionKey.channel());
            userMap.remove(selectionKey.channel());
            broadcast(selector, (SocketChannel) selectionKey.channel(), "系统:用户[" + name + "]已下线!当前在线人数:" + userMap.size());
        }
    }

    /**
     * 广播信息
     */
    private void broadcast(Selector selector, SocketChannel self, String message) {

        try {
            Set<SelectionKey> selectionKeys = selector.keys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();

            while (iterator.hasNext()) {
                SelectionKey next = iterator.next();

                Channel channel = next.channel();
                //给所有人广播,排除自身
                if (channel instanceof SocketChannel && channel != self) {
                    SocketChannel socketChannel = (SocketChannel) channel;
                    socketChannel.write(charset.encode(message));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 广播在线人数
     */
    private void broadcastLiveNum() {
        if (userMap.size() > 0) {
            broadcast(selector, null, "系统:当前聊天室人数:" + userMap.size());
        }
    }

    public static void main(String[] args) {
        new NioServer().start();
    }

}
