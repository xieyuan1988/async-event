/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event.storage;

import com.cubbery.event.EventStorage;
import com.cubbery.event.event.EventState;
import com.cubbery.event.event.RetryEvent;
import com.cubbery.event.event.SimpleEvent;
import com.cubbery.event.retry.Lease;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface MysqlEventStorage extends EventStorage {

    @Insert("insert into async_event(`status`,`data`,`mark`,`type`,`expression`) values ( " + EventState.CONSUME + ",#{data},#{mark},#{type},#{expression}) ")
    @Options(useGeneratedKeys = true,keyProperty = "id")
    @Override
    void insertEvent(SimpleEvent event);

    @Update("update async_event set status = "+ EventState.DEAD + " where id = #{id}")
    @Override
    int markAsDead(@Param("id")long id);

    @Update("update async_event set status = "+ EventState.SUCCESS + " where id = #{id}")
    @Override
    int markAsSuccess(@Param("id")long id);

    @Update("update async_event set status = "+ EventState.RETRY + " where id = #{id}")
    @Override
    int markAsRetry(@Param("id") long id);

    @Update("update async_event set status = "+ EventState.RETRY + " where now() - modifiedTime > 600000 and status = " + EventState.CONSUME)
    @Override
    int batchMarkAsRetry();

    @Select("select `id`,`status`,`expression`,`data`,`mark`,`type`,`createdTime`,`modifiedTime` from async_event where status = " + EventState.RETRY + " limit 0,100")
    @Override
    List<RetryEvent> selectRetryEvents();

    @Select("select `id`,`period`,`master`,`version`,`createdTime`,`modifiedTime`,now() as now from async_lease")
    @Override
    Lease selectLease();

    @Update("update async_lease set master = #{masterInfo},version = #{oldVersion} + 1,modifiedTime = now()  where version = #{oldVersion}")
    @Override
    int updateLease(@Param("masterInfo") String masterInfo, @Param("oldVersion")  long oldVersion);

    @Insert("insert into async_lease(`id`,`period`,`master`,`version`) values (1,#{period},'127.0.0.1',1) ON DUPLICATE KEY UPDATE period = #{period}")
    @Override
    void initLease(@Param("period") long period);

    @Insert("insert into async_lease_offline(`master`) values (#{master}) ")
    @Override
    int confirmOffline(Lease lease);

    @Select("select count(1) from async_lease_offline where now() - createdTime > (select period from async_lease)")
    @Override
    int selectOfflineInHalfPeriod();
}
