/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event;

import com.cubbery.event.channel.ChannelData;
import com.cubbery.event.handler.EventHandler;
import com.cubbery.event.utils.Validation;
import com.cubbery.event.worker.ConsumeWorker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 事件派发者
 */
final class Dispatcher {
    private final EventBus eventBus;
    private volatile boolean running;

    protected Dispatcher(EventBus eventBus) {
        this.eventBus = eventBus;
        this.running = false;
    }

    public synchronized Dispatcher start() {
        this.running = true;
        //使用线程池而不使用单个线程是确保在dispatch的时候尽可能的短平快。
        //同时需要确保dispatch的数量< consume的熟练，尽可能的把时间资源留给业务逻辑处理。
        batchInvoke(eventBus.getDispatchExecutor(),eventBus.getDispatcherPoolSize());
        return this;
    }

    public synchronized void stop() {
        this.running = false;
    }

    class DispatcherTask implements Runnable {
        @Override
        public void run() {
            Channel channel = eventBus.getChannel();
            while (running) {
                //避免线程无法shutDown,所以给定一个超时时间，而不是一直阻塞
                ChannelData event = channel.poll(10, TimeUnit.MILLISECONDS);
                try {
                    Validation.checkNotNull(event, "Event cannot be null.");
                    Validation.checkNotNull(event.getData(), "Event cannot be null.");
                    Validation.checkNotNull(event.getHandler(), "Handler cannot be null.");
                } catch (Exception e) {
                    continue;
                }
                EventHandler handler = event.getHandler();
                eventBus.getConsumeExecutor().submit(new ConsumeWorker(handler,event));
            }
        }
    }

    private void batchInvoke(ExecutorService executorService,int size) {
        for(int a = 0; a < size ; a++ ) {
            executorService.submit(new DispatcherTask());
        }
    }
}
