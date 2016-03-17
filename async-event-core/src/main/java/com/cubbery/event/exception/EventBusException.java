/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event.exception;

public class EventBusException extends RuntimeException {
    public EventBusException(String message) {
        super(message);
    }

    public EventBusException(Throwable cause) {
        super(cause);
    }
}
