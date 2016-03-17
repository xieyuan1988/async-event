/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event.storage;

import com.cubbery.event.EventStorage;
import com.cubbery.event.event.EventState;
import com.cubbery.event.event.RetryEvent;
import com.cubbery.event.event.SimpleEvent;
import com.cubbery.event.exception.EventStorageException;
import com.cubbery.event.retry.Lease;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class JdbcEventStorage implements EventStorage {
    private final DataSource dataSource;
    private final DataSourceType dataSourceType;
    private final String selectRetry ;
    private final String selectLease;

    public JdbcEventStorage(DataSource dataSource,DataSourceType dataSourceType) {
        this.dataSource = dataSource;
        this.dataSourceType = dataSourceType;
        if(DataSourceType.MYSQL.equals(this.dataSourceType)) {
            selectRetry = "select `id`,`status`,`expression`,`data`,`mark`,`type`,`createdTime`,`modifiedTime` from async_event where status = " + EventState.RETRY + " limit 0,100";
            selectLease = "select `id`,`period`,`master`,`version`,`createdTime`,`modifiedTime`,now() as now from async_lease";
        } else {
            selectRetry = "select id,status,expression,data,mark,type,created_Time as createdTime,modified_Time as modifiedTime from async_event where status = " + EventState.RETRY + " and rownum <= 100";
            selectLease = "select id,period,master,version,created_Time as createdTime,modified_Time as modifiedTime,systimestamp as now from async_lease";
        }
    }

    @Override
    public void insertEvent(SimpleEvent event) {
        try {
            if(DataSourceType.MYSQL.equals(this.dataSourceType)) {
                long id = new DbUtils(dataSource.getConnection()).executeSQL("insert into async_event(status,data,mark,type,expression) values ( " + EventState.CONSUME + ",?,?,?,?) ", new IRowMap<Long>() {
                    @Override
                    public Long mapRow(ResultSet rs) throws SQLException {
                        return rs.getLong(1);
                    }
                }, event.getData(),event.getMark(),event.getType(), event.getExpression());
                event.setId(id);
                return;
            }
            if(DataSourceType.ORACLE.equals(this.dataSourceType)) {
                long id = new DbUtils(dataSource.getConnection()).queryForObject("SELECT SEQ_ASYNC_EVENT_ID.nextval as id FROM dual",new IRowMap<Long>(){
                    @Override
                    public Long mapRow(ResultSet rs) throws SQLException {
                        return rs.getLong("id");
                    }
                });
                if(id > 0) {
                    new DbUtils(dataSource.getConnection()).executeSQL(
                            "insert into async_event(status,data,mark,type,expression) values (?," + EventState.CONSUME + ",?,?,?,?) "
                            ,id, event.getData(),event.getMark(),event.getType(), event.getExpression()
                    );
                }

            }
        } catch (SQLException e) {
            throw new EventStorageException(e);
        }
    }

    @Override
    public int markAsDead(long id) {
        try {
            return new DbUtils(dataSource.getConnection()).executeSQL("update async_event set status = " + EventState.DEAD +" where id = ?",id);
        } catch (SQLException e) {
            throw new EventStorageException(e);
        }
    }

    @Override
    public int markAsSuccess(long id) {
        try {
            return new DbUtils(dataSource.getConnection()).executeSQL("update async_event set status = " + EventState.SUCCESS +" where id = ?",id);
        } catch (SQLException e) {
            throw new EventStorageException(e);
        }
    }

    @Override
    public int markAsRetry(long id) {
        try {
            return new DbUtils(dataSource.getConnection()).executeSQL("update async_event set status = " + EventState.RETRY +" where id = ?",id);
        } catch (SQLException e) {
            throw new EventStorageException(e);
        }
    }

    @Override
    public int batchMarkAsRetry() {
        try {
            return new DbUtils(dataSource.getConnection()).executeSQL("update async_event set status = " + EventState.DEAD);
        } catch (SQLException e) {
            throw new EventStorageException(e);
        }
    }

    @Override
    public List<RetryEvent> selectRetryEvents() {
        try {
            return new DbUtils(dataSource.getConnection()).queryForList(selectRetry,new IRowMap<RetryEvent>() {
                @Override
                public RetryEvent mapRow(ResultSet rs) throws SQLException {
                    RetryEvent event = new RetryEvent();
                    event.setCreatedTime(rs.getTimestamp("createdTime"));
                    event.setModifiedTime(rs.getTimestamp("modifiedTime"));

                    event.setMark(rs.getString("mark"));
                    event.setData(rs.getString("data"));
                    event.setStatus(rs.getInt("status"));
                    event.setId(rs.getLong("id"));
                    event.setExpression(rs.getString("expression"));
                    event.setType(rs.getString("type"));
                    return event;
                }
            });
        } catch (SQLException e) {
            throw new EventStorageException(e);
        }
    }

    @Override
    public Lease selectLease() {
        try {
            return new DbUtils(dataSource.getConnection()).queryForObject(selectLease,new IRowMap<Lease>() {
                @Override
                public Lease mapRow(ResultSet rs) throws SQLException {
                    Lease lease = new Lease();
                    lease.setCreatedTime(rs.getTimestamp("createdTime"));
                    lease.setModifiedTime(rs.getTimestamp("modifiedTime"));
                    lease.setId(rs.getLong("id"));
                    lease.setPeriod(rs.getLong("period"));
                    lease.setMaster(rs.getString("master"));
                    lease.setVersion(rs.getLong("version"));
                    lease.setNow(rs.getTimestamp("now"));
                    return lease;
                }
            });
        } catch (SQLException e) {
            throw new EventStorageException(e);
        }
    }

    @Override
    public int updateLease(String masterInfo, long oldVersion) {
        try {
            if(DataSourceType.MYSQL.equals(this.dataSourceType)) {
                return new DbUtils(dataSource.getConnection()).executeSQL("update async_lease set master = ?,version = ?,modifiedTime = now()  where version = ?",masterInfo,oldVersion + 1,oldVersion);
            }
            return new DbUtils(dataSource.getConnection()).executeSQL("update async_lease set master = ?,version = ?,modifiedTime = systimestamp  where version = ?",masterInfo,oldVersion + 1,oldVersion);
        } catch (SQLException e) {
            throw new EventStorageException(e);
        }
    }

    @Override
    public void initLease(long period) {
        try {
            if(DataSourceType.MYSQL.equals(this.dataSourceType)) {
                new DbUtils(dataSource.getConnection()).executeSQL("insert into async_lease(id,period,master,version) values (1,?,'127.0.0.1',1) ON DUPLICATE KEY UPDATE period = ?",period,period);
            } else {
                new DbUtils(dataSource.getConnection()).executeSQL("insert into async_lease(id,period,master,version) values (1,?,'127.0.0.1',1) ON DUPLICATE KEY UPDATE period = ?",period,period);
            }
        } catch (SQLException e) {
            throw new EventStorageException(e);
        }
    }

    @Override
    public int confirmOffline(Lease masterInfo) {
        try {
            if(DataSourceType.MYSQL.equals(this.dataSourceType)) {
                return new DbUtils(dataSource.getConnection()).executeSQL("insert into async_lease_offline(master) values (?)", masterInfo);
            }
            if(DataSourceType.ORACLE.equals(this.dataSourceType)) {
                long id = new DbUtils(dataSource.getConnection()).queryForObject("SELECT SEQ_ASYNC_LEASE_OFFLINE.nextval as id FROM dual",new IRowMap<Long>(){
                    @Override
                    public Long mapRow(ResultSet rs) throws SQLException {
                        return rs.getLong("id");
                    }
                });
                if(id > 0) {
                    return new DbUtils(dataSource.getConnection()).executeSQL("insert into async_lease_offline(id,master,createdTime) values (?,?,systimestamp)",id,masterInfo.getMaster());
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new EventStorageException(e);
        }
    }

    @Override
    public int selectOfflineInHalfPeriod() {
        try {
            if(DataSourceType.MYSQL.equals(this.dataSourceType)) {
                return new DbUtils(dataSource.getConnection()).queryForInt("select count(1) from async_lease_offline where now() - createdTime > (select period from async_lease)");
            }
            return new DbUtils(dataSource.getConnection()).queryForInt("select count(1) from async_lease_offline where systimestamp - created_Time > (select period from async_lease)");
        } catch (SQLException e) {
            throw new EventStorageException(e);
        }
    }
}
