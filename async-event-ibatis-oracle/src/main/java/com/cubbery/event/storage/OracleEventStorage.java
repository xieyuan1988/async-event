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

public interface OracleEventStorage extends EventStorage {

    @SelectKey(keyProperty = "id", before = true,resultType = java.lang.Long.class,statement = "SELECT SEQ_ASYNC_EVENT_ID.nextval FROM dual")
    @Insert("insert into async_event(id,status,data,created_Time,modified_Time,type,expression) values (#{id}," + EventState.CONSUME + ",#{data},systimestamp,systimestamp,#{type},#{expression})")
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
    int markAsRetry(@Param("id")long id);

    @Update("update async_event set status = "+ EventState.RETRY + " where systimestamp - modified_Time > 600000 and status = " + EventState.CONSUME)
    @Override
    int batchMarkAsRetry();

    @Select("select id,status,expression,data,mark,created_Time as createdTime,modified_Time as modifiedTime from async_event where state = " + EventState.RETRY + " and rownum <= 100")
    @Override
    List<RetryEvent> selectRetryEvents();

    @Select("select id,period,master,version,created_Time as createdTime,modified_Time as modifiedTime,systimestamp as now from async_lease")
    @Override
    Lease selectLease();

    @Update("update async_lease set master = #{masterInfo},version = #{oldVersion} + 1 where version = #{oldVersion}")
    @Override
    int updateLease(@Param("masterInfo") String masterInfo, @Param("oldVersion")  long oldVersion);

    @Insert("insert into async_lease(id,period,master,version,created_Time,modified_Time) values (1,#{period},'127.0.0.1',1,systimestamp,systimestamp) ")
    @Override
    void initLease(@Param("period") long period);

    @SelectKey(keyProperty = "id", before = true,resultType = java.lang.Long.class,statement = "SELECT SEQ_ASYNC_LEASE_OFFLINE.nextval FROM dual")
    @Insert("insert into async_lease_offline(id,master,created_Time) values (#{id},#{master},systimestamp) ")
    @Override
    int confirmOffline(Lease lease);

    @Select("select count(1) from async_lease_offline where systimestamp - created_Time > (select period from async_lease)")
    @Override
    int selectOfflineInHalfPeriod();
}
