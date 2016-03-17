/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event.retry;

import com.cubbery.event.EventBus;
import com.cubbery.event.EventStorage;
import com.cubbery.event.conf.Configurable;
import com.cubbery.event.conf.ConfigureKeys;
import com.cubbery.event.conf.Context;
import com.cubbery.event.event.RetryEvent;
import com.cubbery.event.handler.EventHandler;
import com.cubbery.event.utils.ThreadFactories;
import com.cubbery.event.utils.Threads;
import com.cubbery.event.worker.RetryWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <b>类描述</b>： 重试服务，管理重试标记、重试分发线程、重试队列等<br>
 * <b>创建人</b>： <a href="mailto:cubber.zh@gmail.com">百墨</a> <br>
 * <b>创建时间</b>：9:46 2016/2/25 <br>
 * @version 1.0.0 <br>
 */
public class RetryService implements Configurable {
    private final static Logger LOG = LoggerFactory.getLogger("Retry-Service");

    //master优先权,单位为秒（s）
    private long priority;
    //重试线程数
    private int retryCount;
    //新master上线等待时间
    private int masterWaitCount;
    //lease 周期
    private long leasePeriod;

    //Lease 持久化引用
    private EventBus eventBus;
    //当前JVM是否为master
    private AtomicBoolean isMaster;
    //是否启动重试服务
    private AtomicBoolean started;
    //重试服务名称
    private String name;
    //lease 线程
    private LeaseTask leaseTask;
    //重试分发 线程
    private ScheduledExecutorService retryDispatchService;
    //重试线程池
    private ExecutorService retryService;

    public RetryService(EventBus eventBus) {
        this(eventBus,10,3,2,60);//默认重试线程数为3，master优先权为10个时间单位
    }

    public RetryService(EventBus eventBus,long priority,int retryCount,int masterWaitCount,long leasePeriod) {
        this.priority = priority;
        this.retryCount = retryCount;
        this.masterWaitCount = masterWaitCount;
        this.leasePeriod = leasePeriod;

        this.eventBus = eventBus;
        this.isMaster = new AtomicBoolean(false);
        this.started = new AtomicBoolean(false);
        this.name = masterInfo();

    }

    public synchronized void start() {
        if(started.get()) {
            LOG.warn("Retry Service is started!");
            return;
        }
        LOG.info("Try to Start Retry Service ！");
        init();
        startLease();
        startRetry(false);
        started.compareAndSet(false, true);
        LOG.info("Retry Service Started ！");
    }

    public synchronized void stop() {
        LOG.info("Try To Stop Retry Service ！");
        this.setMaster(false);
        leaseTask.stop();
        started.set(false);
        LOG.info("Try To Stop Retry Service ！");
    }

    private synchronized void init() {
        //init Lease
        try {
            eventBus.getStorage().initLease(leasePeriod);
        } catch (Exception e) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Init Lease Error! Ignore the DuplicateKeyException ！",e);
            }
        }
        //init ...
    }

    public synchronized void setMaster(boolean isNewMaster) {
        if(this.isMaster() && isNewMaster) {
            return;//保留现在的状态
        }
        final boolean confirmOfflineIfNecessary = this.isMaster() && !isNewMaster;
        final boolean waitOnlineIfNecessary = !this.isMaster() && isNewMaster;

        this.isMaster.set(isNewMaster);//先改，然后启动
        if(this.isMaster()) {
            startRetry(waitOnlineIfNecessary);
        } else {
            stopRetry(confirmOfflineIfNecessary);
        }
    }

    private void startRetry(boolean waitOnlineIfNecessary) {
        LOG.info("Try to Start Inner Retry Service ！");
        //先读取是否可以上线
        int waitCount = 0;
        while(waitOnlineIfNecessary && (++waitCount) < masterWaitCount) {
            if(eventBus.getStorage().selectOfflineInHalfPeriod() != 1) {
                LOG.info("Wait for Pre-Master Release the Lease...");
                Threads.sleep(1000);
            }
        }
        //在老的master发送了下线通知，或者超时未收到的情况下（存在风险）,新的master上线
        if(started.get() && isMaster()) {
            LOG.info("Start to Retry ！");
            //懒初始化，配置加载
            if(retryService == null) {
                this.retryService = Executors.newFixedThreadPool(this.retryCount,new ThreadFactories("Retry-Consumer"));
            }
            if(retryDispatchService == null) {
                retryDispatchService = Executors.newScheduledThreadPool(1,new ThreadFactories("Retry-Dispatcher"));
            }
            //默认10s处理一次，如果10s之内没有处理完,那么到下一个执行窗口再拉取数据
            retryDispatchService.scheduleWithFixedDelay(new RetryTask(), 0, 10, TimeUnit.SECONDS);
        }
        LOG.info("Inner Retry Service Started ！");
    }

    private void stopRetry(boolean confirmOfflineIfNecessary) {
        LOG.info("Try To Stop Inner Retry Service ！");
        if(retryDispatchService != null && !retryDispatchService.isShutdown()) {
            retryDispatchService.shutdownNow();//不等待，立刻关闭。在执行的runnable中处理interrupted异常！
        }
        if(retryService != null && !retryService.isShutdown()) {
            retryService.shutdownNow();//不等待，立刻关闭。在执行的runnable中处理interrupted异常！
        }
        try {
            retryDispatchService.awaitTermination(Long.MAX_VALUE,TimeUnit.DAYS);
            retryService.awaitTermination(Long.MAX_VALUE,TimeUnit.DAYS);
        } catch (InterruptedException e) {
            Thread.interrupted();
            LOG.error("Wait Retry Service Terminal Be Interrupted!",e);
        }
        //发送下线确认
        if(confirmOfflineIfNecessary) {
            this.eventBus.getStorage().confirmOffline(new Lease().setMaster(name));
        }
        LOG.info("Stop Inner Retry Service Success ！");
    }

    private void startLease() {
        LOG.info("Try To Start Lease Service ！");
        if(started.get()) return;
        if(leaseTask == null) {
            leaseTask = new LeaseTask(this, false);
        }
        Thread leaseThread  = new ThreadFactories("Lease").newThread(leaseTask);
        leaseThread.start();
        LOG.info("Start Lease Service Success ！");
    }

    public boolean isMaster() {
        return this.isMaster.get();
    }

    public long getPriority() {
        return priority;
    }

    public String getName() {
        return name;
    }

    public void setPriority(long priority) {
        this.priority = priority;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public EventStorage getLeaseDao() {
        return eventBus.getStorage();
    }

    class RetryTask implements Runnable {
        @Override
        public void run() {
            List<RetryEvent> retries = eventBus.getStorage().selectRetryEvents();
            int size = retries.size();
            LOG.info("Read {} items to retry!",size);
            if(size < 1) return;

            for (int a = 0; (a < size && started.get()); a++ ) {
                try {
                    RetryEvent entity = retries.get(a);
                    consumeEvent(entity);
                } catch (Throwable throwable) {
                    LOG.error("Consumer Error!",throwable);
                    break;
                }
            }
        }

        private void consumeEvent(RetryEvent entity) {
            EventHandler handler = eventBus.getHandlerClassByType(entity.getType(),entity.getExpression());
            if(handler == null) return;
            retryService.submit(new RetryWorker(eventBus,handler, entity));
        }
    }

    private String masterInfo() {
        StringBuffer sb = new StringBuffer();
        try {
            sb.append(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            sb.append("127.0.0.1");
        }
        sb.append("_").append(Thread.currentThread().getName());
        sb.append("_").append(new Random().nextInt(100));
        return sb.toString();
    }

    @Override
    public void configure(Context context) {
        this.priority = context.getLong(ConfigureKeys.RETRY_MASTER_PRIORITY,this.priority);
        this.retryCount = context.getInt(ConfigureKeys.RETRY_PARALLEL_COUNT,this.retryCount);
        this.masterWaitCount = context.getInt(ConfigureKeys.RETRY_MASTER_WAIT,this.masterWaitCount);
        this.leasePeriod = context.getLong(ConfigureKeys.RETRY_LEASE_PERIOD,this.leasePeriod);
    }
}
