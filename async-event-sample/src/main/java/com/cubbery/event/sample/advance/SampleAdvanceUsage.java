/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event.sample.advance;

import com.cubbery.event.EventBus;
import com.cubbery.event.EventStorage;
import com.cubbery.event.channel.MonitoredChannel;
import com.cubbery.event.channel.PersistentChannel;
import com.cubbery.event.event.RetryEvent;
import com.cubbery.event.event.SimpleEvent;
import com.cubbery.event.monitor.ChannelStatistics;
import com.cubbery.event.retry.Lease;
import com.cubbery.event.sample.event.EventA;
import com.cubbery.event.sample.event.ListenerSub;
import com.cubbery.event.utils.Threads;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SampleAdvanceUsage {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring-ds.xml");

        //使用持久化的通道，保证数据不丢
        EventStorage eventStorage = applicationContext.getBean("storage",EventStorage.class);

        //监控采集样本,这个实现是监控数据放在客户端内存，也可以写入远端存储
        final ChannelStatistics cs = new ChannelStatistics("channel_monitor");

        //创建事件总线
        final EventBus eventBus = new EventBus(new MonitoredPersistenceChannel(1024,eventStorage,cs));
        eventBus.register(new ListenerSub());
        eventBus.start();

        final AtomicBoolean isEnd = new AtomicBoolean(false);
        //开启队列监控,定时打印队列信息
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!isEnd.get()) {
                    System.out.println(cs);
                    Threads.sleep(1000);
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!isEnd.get()) {
                    eventBus.publish(new EventA("lee",10));
                }
            }
        }).start();

        Threads.sleep(10 * 1000);
        isEnd.set(true);
        eventBus.stop();
        System.out.println("===End====" + cs);

        //===End====CHANNEL:channel_monitor{channel.event.put.success=2015, channel.current.size=1024, channel.capacity=1024, channel.event.take.attempt=2665, channel.event.take.success=2015, channel.event.put.attempt=2015}
    }
}
