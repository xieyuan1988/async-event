/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event;

import com.cubbery.event.handler.EventHandler;

import java.util.Map;
import java.util.Set;

public interface HandlerFinder {
    /**
     * 查询（发现）订阅方法
     *
     * @param listener  监听者（订阅者）
     * @return
     */
    Map<Class<?>, Set<EventHandler>> findAllHandlers(Object listener);

    /**
     * 配置存储
     *
     * @param storage
     * @return
     */
    HandlerFinder setStorage(EventStorage storage);
}
