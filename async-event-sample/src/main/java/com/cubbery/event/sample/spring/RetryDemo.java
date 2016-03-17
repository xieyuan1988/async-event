/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event.sample.spring;

import com.cubbery.event.EventBus;
import com.cubbery.event.channel.PersistentChannel;
import com.cubbery.event.retry.RetryService;
import com.cubbery.event.sample.event.AnnotationSub;
import com.cubbery.event.sample.event.BothSub;
import com.cubbery.event.sample.event.EventA;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.CountDownLatch;

public class RetryDemo {
    private static volatile boolean end = false;

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring-ds.xml");

        //使用持久化的通道，保证数据不丢
        final EventBus eventBus = new EventBus(new PersistentChannel(1024, (com.cubbery.event.EventStorage) applicationContext.getBean("storage")));
        RetryService retryService = new RetryService(
                eventBus,//数据总线
                10,//master优先权,单位为秒（s）
                3,//重试线程数
                2,//新master上线等待时间
                60 //lease 周期
        );
        eventBus.setRetryService(retryService);

        eventBus.register(new AnnotationSub());//注册消费者
        eventBus.register(new BothSub());//注册消费者
        eventBus.start();//如果持有重试服务，那么启动他

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
                        eventBus.publish(new EventA());//发送事件消息
                    }
                }
            }).start();
        }
        countDownLatch.countDown();
//        Threads.sleep(1000 * 60);
//        end = true;
//        eventBus.stop();
    }
}
