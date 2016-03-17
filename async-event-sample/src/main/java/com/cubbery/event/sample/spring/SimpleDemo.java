/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event.sample.spring;

import com.cubbery.event.EventBus;
import com.cubbery.event.Subscriber;
import com.cubbery.event.channel.PersistentChannel;
import com.cubbery.event.event.SimpleEvent;
import com.cubbery.event.sample.event.AnnotationSub;
import com.cubbery.event.sample.event.BothSub;
import com.cubbery.event.sample.event.EventA;
import com.cubbery.event.utils.Threads;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleDemo {
    private static volatile boolean end = false;

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring-ds.xml");

        final EventBus eventBus = new EventBus(new PersistentChannel(1024, (com.cubbery.event.EventStorage) applicationContext.getBean("storage")));
        eventBus.register(new AnnotationSub());
        eventBus.register(new BothSub());
        eventBus.start();

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        for (int i = 0; i < 10 && !end; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        countDownLatch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    for (int a = 0; a < 100 && !end; a++) {
                        eventBus.publish(new EventA());
                    }
                }
            }).start();
        }
        countDownLatch.countDown();
        Threads.sleep(1000);
        end = true;
        eventBus.stop();
    }
}
