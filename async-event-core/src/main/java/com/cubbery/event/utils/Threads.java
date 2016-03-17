/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event.utils;

import java.util.concurrent.TimeUnit;

public class Threads {
    /**
     * sleep等待,单位为毫秒,忽略InterruptedException.
     * @param millis
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.interrupted();
            return;
        }
    }

    /**
     * sleep等待,忽略InterruptedException.
     * @param duration
     * @param unit
     */
    public static void sleep(long duration, TimeUnit unit) {
        try {
            Thread.sleep(unit.toMillis(duration));
        } catch (InterruptedException e) {
            Thread.interrupted();
            return;
        }
    }
}
