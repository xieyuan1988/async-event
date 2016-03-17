/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event.channel;

import com.cubbery.event.EventStorage;
import com.cubbery.event.event.SimpleEvent;
import com.cubbery.event.handler.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistentChannel extends MemoryChannel {
    private final Logger LOGGER = LoggerFactory.getLogger("Persistent-Channel");

    private EventStorage storage;

    public PersistentChannel(int capacity, final EventStorage storage) {
        super(capacity);
        this.storage = storage;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                //将最近10min没有消费的数据mark as retry!
                //使用其他后台方式定时mark as retry!
                storage.batchMarkAsRetry();
            }
        });
    }

    @Override
    public boolean offer(Object event,EventHandler handler) {
        try {
            SimpleEvent simpleEvent = SimpleEvent.create(event,handler);
            storage.insertEvent(simpleEvent);
            return super.offer(event,handler,simpleEvent.getId());
        } catch (Exception e) {
            //1、数据库记录成功，但是队列入失败。重试服务处理此类问题。
            //2、数据库和队列对入失败,没有影响。
            LOGGER.warn("Offer operator wrong! event = {} " + event,e);
        }
        return false;
    }

    @Override
    public boolean checkInit() {
        return super.checkInit() && storage != null ;
    }

    @Override
    public EventStorage getStorage() {
        return this.storage;
    }


    @Override
    public void setStorage(EventStorage storage) {
        this.storage = storage;
    }
}
