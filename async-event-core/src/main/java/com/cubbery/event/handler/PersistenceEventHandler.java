/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event.handler;

import com.cubbery.event.EventStorage;
import com.cubbery.event.channel.ChannelData;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 持久化类型的事件订阅者代理
 */
public class PersistenceEventHandler extends EventHandler {
    private final EventStorage storage;

    public PersistenceEventHandler(Object target, Method method, EventStorage storage) {
        super(target, method);
        this.storage = storage;
    }

    @Override
    public void handleEvent(Object event) throws InvocationTargetException {
        long id = 0;
        if(event instanceof ChannelData) {
            id = ((ChannelData)event).getId();
        }
        try {
            super.handleEvent(event);
            markAsSuccess(id);
        } catch (InvocationTargetException e) {
            throw e;
        } catch (Throwable throwable) {
            markAsRetry(id);
        }
    }

    private void markAsRetry(long id) {
        if(id > 0) {
            storage.markAsRetry(id);
        }
    }

    private void markAsSuccess(long id) {
        if(id > 0) {
            storage.markAsSuccess(id);
        }
    }

    public EventStorage getStorage() {
        return storage;
    }
}
