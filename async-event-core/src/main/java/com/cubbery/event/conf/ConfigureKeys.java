/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event.conf;

public interface ConfigureKeys {

    /**总线消费者数量**/
    String BUS_CONSUME_COUNT = "bus.consume.count";

    /**总线分发者数量**/
    String BUS_DISPATCH_COUNT = "bus.dispatch.count";

    /**总线时间单位**/
    String BUS_TIME_UNIT = "bus.time.unit";

    /**渠道默认超时时间**/
    String CHANNEL_OFFER_EXPIRE = "channel.offer.expire";

    /**通道类型**/
    String CHANNEL_TYPE = "channel.type";

    /**通道大小**/
    String CHANNEL_SIZE = "channel.size";

    /**重试服务master优先权,单位为秒（s）**/
    String RETRY_MASTER_PRIORITY = "retry.master.priority";

    /**重试并发处理数**/
    String RETRY_PARALLEL_COUNT = "retry.parallel.count";

    /**新master上线等待时间**/
    String RETRY_MASTER_WAIT = "retry.master.wait";

    /**租约周期**/
    String RETRY_LEASE_PERIOD = "retry.lease.period";

}
