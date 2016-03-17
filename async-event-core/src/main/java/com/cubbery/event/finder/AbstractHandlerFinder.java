/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event.finder;

import com.cubbery.event.EventStorage;
import com.cubbery.event.HandlerFinder;
import com.cubbery.event.handler.EventHandler;
import com.cubbery.event.handler.PersistenceEventHandler;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

abstract class AbstractHandlerFinder implements HandlerFinder {
    private EventStorage storage;

    protected EventHandler makeHandler(Object listener, Method method) {
        if(getStorage() != null) {
            return new PersistenceEventHandler(listener, method,getStorage());
        }
        return new EventHandler(listener, method);
    }

    public EventStorage getStorage() {
        return storage;
    }

    public AbstractHandlerFinder setStorage(EventStorage storage) {
        this.storage = storage;
        return this;
    }

    protected void put(final Map<Class<?>, Set<EventHandler>> map,final Class<?> clazz,final EventHandler handler) {
        if(!map.containsKey(clazz)) {
            map.put(clazz,new HashSet<EventHandler>());//初始化
        }
        map.get(clazz).add(handler);
    }

    protected void put(final Map<Class<?>, Set<EventHandler>> map,final Class<?> clazz,final Set<EventHandler> handler) {
        if(!map.containsKey(clazz)) {
            map.put(clazz,new HashSet<EventHandler>());//初始化
        }
        map.get(clazz).addAll(handler);
    }

    protected void putAll(final Map<Class<?>, Set<EventHandler>> map,final Map<Class<?>, Set<EventHandler>> map0) {
        Set<Class<?>> keys = map0.keySet();
        for(Class<?> clazz : keys) {
            put(map,clazz,map0.get(clazz));
        }
    }

}
