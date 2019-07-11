package com.ws.chatroom;

/**
 * 事件包装
 */
public class ChannelEventWrapper {

    private ChannelEvent channelEvent;

    private String message;

    public ChannelEventWrapper(ChannelEvent channelEvent, String message) {
        this.channelEvent = channelEvent;
        this.message = message;
    }

    public ChannelEvent getChannelEvent() {
        return channelEvent;
    }

    public void setChannelEvent(ChannelEvent channelEvent) {
        this.channelEvent = channelEvent;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
