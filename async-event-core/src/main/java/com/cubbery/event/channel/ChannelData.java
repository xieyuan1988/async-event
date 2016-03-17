/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event.channel;

import com.cubbery.event.handler.EventHandler;

public class ChannelData {
    private Object data;//event
    private long id;//persistent id
    private EventHandler handler;//handler


    public ChannelData(Object data) {
        this.data = data;
    }

    public ChannelData(Object data, EventHandler handler) {
        this.data = data;
        this.handler = handler;
    }

    public ChannelData(Object data, long id, EventHandler handler) {
        this.data = data;
        this.id = id;
        this.handler = handler;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public EventHandler getHandler() {
        return handler;
    }

    public void setHandler(EventHandler handler) {
        this.handler = handler;
    }
}
