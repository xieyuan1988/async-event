/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event;

import com.cubbery.event.channel.MemoryChannel;
import com.cubbery.event.conf.Configurable;
import com.cubbery.event.conf.ConfigureKeys;
import com.cubbery.event.conf.Context;
import com.cubbery.event.exception.EventBusException;
import com.cubbery.event.finder.BothHandlerFinder;
import com.cubbery.event.handler.EventHandler;
import com.cubbery.event.retry.RetryService;
import com.cubbery.event.utils.RefactorUtils;
import com.cubbery.event.utils.ThreadFactories;
import com.cubbery.event.utils.Threads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class EventBus implements Configurable {
    private final static Logger LOG = LoggerFactory.getLogger("Event-Bus");
    //时间单位为s
    private TimeUnit timeUnit = TimeUnit.SECONDS;
    //是否已经启动
    private AtomicBoolean started;
    //通道
    private Channel channel;
    //查找方式
    private HandlerFinder finder;
    //存储
    private EventStorage storage;
    //注入方式，如果不存在那么不启动重试服务
    private RetryService retryService;
    //分发器
    private Dispatcher dispatcher;
    //分发器线程池大小
    private int dispatcherPoolSize;
    //分发者执行器
    private ExecutorService dispatchExecutor;
    //消费者线程池大小
    private int consumerPoolSize;
    //消费者执行器
    private ExecutorService consumeExecutor;
    //配置上下文
    private Context context;

    private final Map<Class<?>,Set<EventHandler>> handlersByType = new ConcurrentHashMap<Class<?>, Set<EventHandler>>();

    private final Map<String,Class<? >> eventTypeByClass = new HashMap<String, Class<?>>();

    public EventBus() {
        this(new MemoryChannel(1024));
    }

    public EventBus(Channel channel) {
        this(channel,new BothHandlerFinder());
    }

    public EventBus(Properties properties) {
        this(null,null,new Context(properties));
    }

    public EventBus(Channel channel,Properties properties) {
        this(channel,null,new Context(properties));
    }

    public EventBus(Channel channel,HandlerFinder finder) {
        this(channel,finder,null);
    }

    public EventBus(Channel channel,HandlerFinder finder,Context context) {
        this(channel,finder,null,0,0,context);
    }

    public EventBus(Channel channel,HandlerFinder finder,EventStorage storage,int consumerCount,int dispatcherCount,Context context) {
        this.context = context;
        this.channel = channel;
        initChannelByConf();
        if(finder != null) {
            this.finder = finder;
        } else {
            this.finder = new BothHandlerFinder();
        }

        this.started = new AtomicBoolean(false);
        if(channel != null && channel.getStorage() != null && storage == null) {
            this.storage = channel.getStorage();
        }
        if(storage != null) {
            this.storage = storage;
            if(channel != null ) {
                channel.setStorage(storage);
            }
        }
        this.finder.setStorage(this.storage);
        this.consumerPoolSize = consumerCount > 0 ? consumerCount : Runtime.getRuntime().availableProcessors() + 1 ;
        this.dispatcherPoolSize = dispatcherCount > 0 ? dispatcherCount : Runtime.getRuntime().availableProcessors() / 3 + 1;
    }

    public synchronized void start() {
        if(started.get()) {
            if(LOG.isErrorEnabled()) {
                LOG.error("Event Bus is Running!");
            }
        }
        //检查参数项
        checkNecessary();
        //配置下发到相关组件
        deliverConfig();
        if(consumeExecutor == null) {
            consumeExecutor = Executors.newFixedThreadPool(consumerPoolSize,new ThreadFactories("Consumer"));
        }
        if(dispatchExecutor == null) {
            dispatchExecutor = Executors.newFixedThreadPool(dispatcherPoolSize,new ThreadFactories("Dispatcher"));
        }
        if(retryService != null) {
            retryService.start();
        }
        if(dispatcher == null) {
            dispatcher = new Dispatcher(this).start();
        }
        buildEventType();
        started.set(true);
        if(LOG.isDebugEnabled()) {
            LOG.debug("Event Bus started!");
        }
    }

    public synchronized void stop() {
        started.set(false);//确保新消息无法发送
        if(retryService != null) {
            retryService.stop();
        }
        while (!channel.isEmpty()) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Try to Stop ,wait the channel to be empty...");
            }
            Threads.sleep(10);
        }
        if(dispatcher != null) {
            dispatcher.stop();
        }
        if(!dispatchExecutor.isShutdown()) {
            dispatchExecutor.shutdown();
        }
        if(!consumeExecutor.isShutdown()) {
            consumeExecutor.shutdown();
        }
        try {
            dispatchExecutor.awaitTermination(Long.MAX_VALUE,TimeUnit.DAYS);
            consumeExecutor.awaitTermination(Long.MAX_VALUE,TimeUnit.DAYS);
        } catch (InterruptedException e) {
            Thread.interrupted();
            LOG.error("Wait Bus Terminal Be Interrupted!",e);
        }
        LOG.info("Event Bus Terminated !");
    }

    public boolean publish(Object e) {
        if(started.get()) {//event-bus本身不对消息去重，到达即唯一
            if(e == null) return false;
            Set<EventHandler> handlers = getHandlersForEventType(e.getClass());
            if(handlers.isEmpty()) {//聪哥建议：对于未注册handler的消息，一律异常警告调用方！
                throw new EventBusException("Must be register at last one Available event handler before!");
            }
            for(EventHandler handler : handlers) {
                channel.offer(e,handler);
            }
            return true;
        }
        throw new EventBusException("Must Be started Before!");
    }

    public synchronized void register(Object eventHandler) {
        if(started.get()) {
            throw new EventBusException("Must Be Before started!");
        }
        handlersByType.putAll(finder.findAllHandlers(eventHandler));
    }

    public Set<EventHandler> getHandlersForEventType(Class<?> type) {
        Set<EventHandler> sets = handlersByType.get(type);
        if(sets == null) {
            handlersByType.put(type,new HashSet<EventHandler>());
        }
        return handlersByType.get(type);
    }

    public EventHandler getHandlerClassByType(String eventType,String handlerExpression) {
        Class<?> eventClass = eventTypeByClass.get(eventType);
        Set<EventHandler> handlers = getHandlersForEventType(eventClass);
        for(EventHandler handler : handlers) {
            if(handler.expression().equalsIgnoreCase(handlerExpression)) {
                return handler;
            }
        }
        return null;
    }

    public Class<?> getEventClassByType(String eventType) {
        return eventTypeByClass.get(eventType);
    }

    public Channel getChannel() {
        return channel;
    }

    public ExecutorService getConsumeExecutor() {
        return consumeExecutor;
    }

    public EventStorage getStorage() {
        return storage;
    }

    public void setRetryService(RetryService retryService) {
        this.retryService = retryService;
    }

    public ExecutorService getDispatchExecutor() {
        return dispatchExecutor;
    }

    public int getDispatcherPoolSize() {
        return dispatcherPoolSize;
    }

    public int getConsumerPoolSize() {
        return consumerPoolSize;
    }

    private void checkNecessary() {
        if(this.channel == null || !this.channel.checkInit()) {
            throw new EventBusException("Channel Init Error！ Check Necessary Parameters!");
        }
    }

    private void buildEventType() {
        for(Class<?> clazz : handlersByType.keySet()) {
            eventTypeByClass.put(clazz.getCanonicalName(),clazz);
        }
    }

    private void initChannelByConf() {
        if(this.channel != null) {
            return;
        }
        String channelType = context.getString(ConfigureKeys.CHANNEL_TYPE);
        if(channelType == null || channelType.equals("")) {
            return;
        }
        try {
            Class clazz = Class.forName(channelType);
            int size = context.getInt(ConfigureKeys.CHANNEL_SIZE);
            //定义了大小的channel
            Object obj = RefactorUtils.newInstance(clazz,size);
            //无参构造
            if(obj == null) {
                obj = RefactorUtils.newInstance(clazz);
            }
            //其他类型的构造
            if(obj != null && Channel.class.isAssignableFrom(clazz)) {
                this.channel = (Channel) obj;
            }
        } catch (ClassNotFoundException e) {
            LOG.error(" Can't Find the Class of channel.type! Error Type = " + channelType);
        } catch (Exception e) {
            LOG.error(" Can't Instance the Class of channel.type! Error Type = " + channelType);
        }
    }

    private void deliverConfig() {
        if(context == null) {
            return;
        }
        this.configure(context);
        if(channel != null && Configurable.class.isAssignableFrom(this.channel.getClass())) {
            ((Configurable)channel).configure(context);
        }
        if(retryService != null) {
            this.retryService.configure(context);
        }
    }

    @Override
    public void configure(Context context) {
        this.consumerPoolSize = context.getInt(ConfigureKeys.BUS_CONSUME_COUNT,this.consumerPoolSize);;
        this.dispatcherPoolSize = context.getInt(ConfigureKeys.BUS_DISPATCH_COUNT,this.dispatcherPoolSize);
        this.timeUnit = context.getTimeUnit(ConfigureKeys.BUS_TIME_UNIT, this.timeUnit);
    }
}
