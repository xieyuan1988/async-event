/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event.sample.jdbc;

import com.cubbery.event.EventBus;
import com.cubbery.event.EventStorage;
import com.cubbery.event.channel.PersistentChannel;
import com.cubbery.event.retry.RetryService;
import com.cubbery.event.sample.event.BothSub;
import com.cubbery.event.sample.event.EventA;
import com.cubbery.event.sample.event.ListenerSub;
import com.cubbery.event.storage.JdbcEventStorage;
import com.cubbery.event.utils.Threads;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.sql.DataSource;
import java.util.concurrent.CountDownLatch;

public class MysqlJdbcSpringDemo {
    private static volatile boolean end = false;

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring-ds.xml");
        DataSource dataSource = (DataSource) applicationContext.getBean("dataSource");
        JdbcEventStorage storage = new JdbcEventStorage(dataSource, EventStorage.DataSourceType.MYSQL);

        //使用持久化的通道，保证数据不丢
        final EventBus eventBus = new EventBus(new PersistentChannel(1024, storage));
        RetryService retryService = new RetryService(eventBus);
        eventBus.setRetryService(retryService);

        eventBus.register(new BothSub());//注册消费者
        eventBus.register(new ListenerSub());//注册消费者
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
                        eventBus.publish(new EventA());//发送事件消息
                    }
                }
            }).start();
        }
        countDownLatch.countDown();
        Threads.sleep(1000 * 60);
        end = true;
        eventBus.stop();
    }
}
