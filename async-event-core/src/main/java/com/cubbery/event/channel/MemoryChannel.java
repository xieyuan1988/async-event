/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event.channel;

import com.cubbery.event.handler.EventHandler;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MemoryChannel extends AbstractChannel {

    private LinkedBlockingQueue<ChannelData> queue;

    public MemoryChannel(int capacity) {
        super();
        this.queue = new LinkedBlockingQueue<ChannelData>(capacity);
    }

    @Override
    public boolean offer(Object event, EventHandler handler) {
        return offer(event,handler,0);
    }

    @Override
    public boolean offer(Object event, EventHandler handler, long id) {
        try {
            return queue.offer(new ChannelData(event,id,handler),expire,timeUnit);
        } catch (InterruptedException e) {
            Thread.interrupted();
            return false;
        }
    }

    @Override
    public ChannelData poll(long timeout, TimeUnit unit) {
        try {
            return queue.poll(timeout, unit);
        } catch (InterruptedException e) {
            Thread.interrupted();
            return null;
        }
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public boolean checkInit() {
        return queue != null && queue.isEmpty();
    }

    protected LinkedBlockingQueue<ChannelData> getQueue() {
        return queue;
    }
}
