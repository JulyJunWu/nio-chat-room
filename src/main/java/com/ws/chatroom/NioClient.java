package com.ws.chatroom;

import com.alibaba.fastjson.JSON;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

/**
 * NIO
 * 聊天室客户端
 */
public class NioClient {

    private String name;

    private Selector selector;

    private SocketChannel socketChannel;

    private Charset charset = Charset.forName("UTF-8");


    public NioClient(String name) {
        this.name = name;
    }


    public void start() throws Exception {
        init();
    }

    private void init() throws Exception {

        socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 8888));
        socketChannel.configureBlocking(false);

        selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_READ);

        //监听可读操作
        new Thread(new ReadThread()).start();

        Scanner scanner = new Scanner(System.in);

        ChannelEventWrapper wrapper = new ChannelEventWrapper(ChannelEvent.REGISTER, name);
        String json = JSON.toJSONString(wrapper);
        socketChannel.write(charset.encode(json));

        while (true) {

            if (scanner.hasNext()) {
                String message = scanner.next();
                wrapper.setChannelEvent(ChannelEvent.WRITE);
                wrapper.setMessage(message);
                json = JSON.toJSONString(wrapper);
                socketChannel.write(charset.encode(json));
            }
        }

    }

    private class ReadThread implements Runnable {

        @Override
        public void run() {
            try {
                while (true) {
                    int select = selector.select();
                    if (select == 0) continue;

                    Set<SelectionKey> selectionKeys = selector.selectedKeys();

                    Iterator<SelectionKey> iterator = selectionKeys.iterator();

                    while (iterator.hasNext()) {
                        SelectionKey next = iterator.next();
                        iterator.remove();

                        if (next.isReadable()) {
                            SocketChannel channel = (SocketChannel) next.channel();
                            StringBuilder sb = new StringBuilder();
                            ByteBuffer allocate = ByteBuffer.allocate(1024);

                            while (channel.read(allocate) > 0) {
                                allocate.flip();
                                sb.append(charset.decode(allocate));
                            }

                            System.out.println(sb.toString());
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) throws Exception {
        NioClient client = new NioClient("遮天");
        client.start();
    }

}
