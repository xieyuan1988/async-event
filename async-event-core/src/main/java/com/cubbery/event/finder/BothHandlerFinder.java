/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event.finder;

import com.cubbery.event.EventStorage;
import com.cubbery.event.HandlerFinder;
import com.cubbery.event.handler.EventHandler;

import java.util.Map;
import java.util.Set;

public class BothHandlerFinder extends AbstractHandlerFinder {
    private final HandlerFinder annotatedFinder;
    private final HandlerFinder listenersFinder;

    public BothHandlerFinder() {
        this.annotatedFinder = new AnnotatedHandlerFinder();
        this.listenersFinder = new ListenerHandlerFinder();
    }

    @Override
    public Map<Class<?>, Set<EventHandler>> findAllHandlers(Object listener) {
        Map<Class<?>, Set<EventHandler>> multimap = this.annotatedFinder.findAllHandlers(listener);
        putAll(multimap,this.listenersFinder.findAllHandlers(listener));
        return multimap;
    }

    @Override
    public AbstractHandlerFinder setStorage(EventStorage storage) {
        super.setStorage(storage);
        annotatedFinder.setStorage(getStorage());
        listenersFinder.setStorage(getStorage());
        return this;
    }
}
