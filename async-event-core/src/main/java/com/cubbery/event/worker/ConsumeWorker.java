/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event.worker;

import com.cubbery.event.channel.ChannelData;
import com.cubbery.event.handler.EventHandler;

import java.lang.reflect.InvocationTargetException;

/**
 * 从queue中取出的event可以使用该worker来消费，否则不能
 */
public class ConsumeWorker extends AbstractWorker implements Runnable {
    private EventHandler handler;
    private ChannelData event;//从queue中取出的event

    public ConsumeWorker(EventHandler handler, ChannelData event) {
        this.handler = handler;
        this.event = event;
    }

    @Override
    public void run() {
        try {
            handler.handleEvent(event);
        } catch (InvocationTargetException e) {
            //mark as dead event !
            //该异常是由于反射调用实际的处理逻辑异常，说明handler的封装有问题，即使重试也会报相同的错误。所以标志为死讯
            markAsDead(this.handler,event.getId());
        }
    }
}
