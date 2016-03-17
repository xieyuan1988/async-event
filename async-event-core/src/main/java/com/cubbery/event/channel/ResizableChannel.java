/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event.channel;

import com.cubbery.event.handler.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

//TODO 锁的粒度太大了，需要优化
class ResizableChannel extends AbstractChannel {
    private static Logger LOGGER = LoggerFactory.getLogger(ResizableChannel.class);

    private LinkedBlockingQueue<ChannelData> queue;
    private Object queueLock = new Object();

    public ResizableChannel(int capacity) {
        super();
        this.queue = new LinkedBlockingQueue<ChannelData>(capacity);
    }

    public void resizeQueue(int capacity) throws InterruptedException {
        int oldCapacity;
        synchronized(queueLock) {
            oldCapacity = queue.size() + queue.remainingCapacity();
        }
        if(oldCapacity == capacity) {
            return;
        } else {
            synchronized(queueLock) {
                LinkedBlockingQueue<ChannelData> newQueue = new LinkedBlockingQueue<ChannelData>(capacity);
                newQueue.addAll(queue);
                queue = newQueue;
            }
        }
    }

    private void resizeQueue() {
        int remainingCapacity = queue.remainingCapacity();
        if(remainingCapacity < 5) {//队列可以数小雨5时，启动resize
            int size = queue.size() + remainingCapacity;//原队列长度
            //新队列长度为原队列的1.5倍
            LinkedBlockingQueue<ChannelData> newQueue = new LinkedBlockingQueue<ChannelData>(size + size/2);
            newQueue.addAll(queue);
            queue = newQueue;
        }
    }

    @Override
    public boolean offer(Object event, EventHandler handler) {
        return this.offer(event,handler,0);
    }

    @Override
    public boolean offer(Object event, EventHandler handler, long id) {
        synchronized(queueLock) {
            resizeQueue();
            try {
                return queue.offer(new ChannelData(event,id,handler),expire,timeUnit);
            } catch (InterruptedException e) {
                Thread.interrupted();
                return false;
            }
        }
    }

    @Override
    public ChannelData poll(long timeout, TimeUnit unit) {
        synchronized (queueLock) {
            try {
                resizeQueue();
                return queue.poll(timeout, unit);
            } catch (InterruptedException e) {
                Thread.interrupted();
                return null;
            }
        }
    }

    @Override
    public boolean checkInit() {
        return queue != null && queue.isEmpty() ;
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
