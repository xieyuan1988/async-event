/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event.sample.event;

import com.cubbery.event.ISubscribe;
import com.cubbery.event.Subscriber;

import static com.cubbery.event.sample.event.ListenerSub.counter;

public class BothSub implements ISubscribe<EventA> {

    @Override
    public void handler(EventA event) {
        System.out.println(getClass() + "=====handler=====" + counter.incrementAndGet());
    }


    @Subscriber
    public void sub(EventA eventA) {
        System.out.println(getClass() + "=====sub=====" + counter.incrementAndGet());
    }

    @Subscriber
    public void sub0(EventAny eventAny) {
        System.out.println(getClass() + "=====sub0=====" + counter.incrementAndGet());
    }
}
