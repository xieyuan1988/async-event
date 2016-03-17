/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event.exception;

public class EventStorageException extends EventBusException {
    public EventStorageException(String message) {
        super(message);
    }

    public EventStorageException(Throwable cause) {
        super(cause);
    }
}
