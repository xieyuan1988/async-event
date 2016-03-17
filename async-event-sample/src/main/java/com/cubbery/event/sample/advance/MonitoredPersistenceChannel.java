/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event.sample.advance;

import com.cubbery.event.EventStorage;
import com.cubbery.event.channel.ChannelData;
import com.cubbery.event.channel.PersistentChannel;
import com.cubbery.event.handler.EventHandler;
import com.cubbery.event.monitor.ChannelStatistics;

import java.util.concurrent.TimeUnit;

/**
 * <b>创建人</b>：   <a href="mailto:cubber.zh@gmail.com">百墨</a> <br>
 * <b>该类在基本管道的基础上，实现了复合的管道类型。</b>
 * @version 1.0.0   <br>
 */
public class MonitoredPersistenceChannel extends PersistentChannel {
    private final ChannelStatistics channelStatistics;

    public MonitoredPersistenceChannel(int capacity, EventStorage storage, ChannelStatistics channelStatistics) {
        super(capacity, storage);
        this.channelStatistics = channelStatistics;
        this.channelStatistics.setChannelCapacity(capacity);
        this.channelStatistics.setChannelSize(capacity);
    }


    @Override
    public boolean offer(Object event, EventHandler handler) {
        return this.offer(event,handler,0);
    }

    @Override
    public boolean offer(Object event, EventHandler handler, long id) {
        boolean isOk = false;
        try {
            this.channelStatistics.incrementEventPutAttemptCount();
            isOk = super.offer(event, handler);
        } finally {
            if(isOk) {
                this.channelStatistics.addToEventPutSuccessCount(1);
            }
            this.channelStatistics.setChannelSize(getQueue().remainingCapacity());
        }
        return isOk;
    }

    @Override
    public ChannelData poll(long timeout, TimeUnit unit) {
        this.channelStatistics.incrementEventTakeAttemptCount();
        ChannelData obj = null;
        try {
            obj = super.poll(timeout, unit);
            return obj;
        } finally {
            if(obj != null) {
                this.channelStatistics.addToEventTakeSuccessCount(1);
            }
            this.channelStatistics.setChannelSize(getQueue().remainingCapacity());
        }
    }
}
