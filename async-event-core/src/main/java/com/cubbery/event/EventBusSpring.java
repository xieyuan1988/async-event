/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>创建人</b>：   <a href="mailto:cubber.zh@gmail.com">百墨</a> <br>
 * <b>创建时间</b>： 2016/3/4 - 10:24  <br>
 * @version 1.0.0   <br>
 */
public class EventBusSpring {
    private EventBus eventBus;
    private List<Object> subscribers;

    public EventBusSpring(EventBus eventBus, List<Object> subscribers) {
        this.eventBus = eventBus;
        this.subscribers = subscribers;
    }

    public EventBusSpring(EventBus eventBus) {
        this.eventBus = eventBus;
        this.subscribers = new ArrayList<Object>();
    }

    public synchronized void start() {
        for(Object obj : subscribers) {
            this.eventBus.register(obj);
        }
        this.eventBus.start();
    }

    public synchronized void stop() {
        this.eventBus.stop();
    }

    public synchronized void setSubscribers(List<Object> subscribers) {
        this.subscribers = subscribers;
    }
}
