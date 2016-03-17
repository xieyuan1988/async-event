/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event.channel;

import com.cubbery.event.handler.EventHandler;
import com.cubbery.event.monitor.ChannelStatistics;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MonitoredChannel extends AbstractChannel {

    private LinkedBlockingQueue<ChannelData> queue;
    private ChannelStatistics statistics;

    public MonitoredChannel(int capacity,ChannelStatistics statistics) {
        super();
        this.queue = new LinkedBlockingQueue<ChannelData>(capacity);
        this.statistics = statistics;
        this.statistics.setChannelCapacity(capacity);
        this.statistics.setChannelSize(capacity);
    }

    @Override
    public boolean offer(Object event, EventHandler handler) {
        return offer(event,handler,0);
    }

    @Override
    public boolean offer(Object event, EventHandler handler, long id) {
        boolean isOk = false;
        try {
            this.statistics.incrementEventPutAttemptCount();
            isOk = queue.offer(new ChannelData(event,id, handler),expire,timeUnit);
        } catch (InterruptedException e) {
            Thread.interrupted();
        } finally {
            if(isOk) {
                this.statistics.addToEventPutSuccessCount(1);
                this.statistics.setChannelSize(queue.size());
            }
        }
        return isOk;
    }

    @Override
    public ChannelData poll(long timeout, TimeUnit unit) {
        this.statistics.incrementEventTakeAttemptCount();
        try {
            ChannelData obj = queue.poll(timeout, unit);
            this.statistics.addToEventTakeSuccessCount(1);
            this.statistics.setChannelSize(queue.size());
            return obj;
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
        return queue != null && queue.isEmpty() && statistics != null ;
    }

    public void setStatistics(ChannelStatistics statistics) {
        this.statistics = statistics;
    }
}
