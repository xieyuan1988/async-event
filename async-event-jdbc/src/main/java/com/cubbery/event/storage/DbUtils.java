/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

final class DbUtils {
    private final static Logger logger = LoggerFactory.getLogger("DB_UTILS");
    private final Connection conn;

    DbUtils(Connection conn) {
        this.conn = conn;
    }

    /**
     * 执行insert update delete SQl
     *
     * @param sql    SQL语句
     * @param params 参数列表
     * @return       影响的行数
     */
    public int executeSQL(String sql, Object... params) {
        logger.info(String.format("executeSQL:" + sql.replace("?", "%s"), params));
        PreparedStatement ps = null;
        int rows = 0;
        try {
            ps = conn.prepareStatement(sql);
            if (params != null && params.length > 0) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
            }
            rows = ps.executeUpdate();
        } catch (SQLException e) {
            logger.info(String.format("executeSQL:" + sql.replace("?", "%s"), params) + "Error Code : " + e.getErrorCode(), e);
        } finally {
            close(null,ps,conn);
        }
        return rows;
    }

    /**
     * 执行insert update delete SQl
     *
     * @param sql               SQL语句
     * @param generatedKeys     当执行insert操作的时候，返回字段生产的key
     * @param params            参数列表
     * @param <T>               类型
     * @return
     */
    public <T> T executeSQL(String sql, IRowMap<T> generatedKeys, Object... params) {
        logger.info(String.format("executeSQL:" + sql.replace("?", "%s"), params));
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            if (params != null && params.length > 0) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
            }
            ps.executeUpdate();
            // 检索由于执行此 Statement 对象而创建的所有自动生成的键
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return generatedKeys.mapRow(rs);
            }
        } catch (SQLException e) {
            logger.info(String.format("executeSQL:" + sql.replace("?", "%s"), params) + "Error Code : " + e.getErrorCode(), e);
        } finally {
            close(null,ps,conn);
        }
        return null;
    }

    /**
     * 根据Select查询产生Object对象
     *
     * @param sql
     * @param map
     * @param params
     * @return
     */
    public <T> T queryForObject(String sql, IRowMap<T> map, Object... params) {
        logger.info(String.format("executeSQL:" + sql.replace("?", "%s"), params));
        T obj = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            if (params != null && params.length > 0) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
            }
            rs = ps.executeQuery();
            if (rs.next()) {
                obj = map.mapRow(rs);
            }
        } catch (SQLException e) {
            logger.info(String.format("executeSQL:" + sql.replace("?", "%s"), params) + "Error Code : " + e.getErrorCode(), e);
        } finally {
            close(rs,ps,conn);
        }
        return obj;
    }

    /**
     * 根据SQL查询 返回int类型结果
     *
     * @param sql
     * @param params
     * @return
     */
    public int queryForInt(String sql, Object... params) {
        logger.info(String.format("executeSQL:" + sql.replace("?", "%s"), params));
        int obj = 0;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);

            if (params != null && params.length > 0) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
            }
            rs = ps.executeQuery();
            if (rs.next()) {
                obj = rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.info(String.format("executeSQL:" + sql.replace("?", "%s"), params) + "Error Code : " + e.getErrorCode(), e);
        } finally {
            close(rs,ps,conn);
        }
        return obj;
    }

    /**
     * 根据Select查询产生List集合
     *
     * @param sql
     * @param map
     * @param params
     * @return
     */
    public <T> List<T> queryForList(String sql, IRowMap<T> map, Object... params) {
        logger.info(String.format("executeSQL:" + sql.replace("?", "%s"), params));
        List<T> list = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);

            if (params != null && params.length > 0) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                if (list == null) {
                    list = new ArrayList<T>();
                }
                T obj = map.mapRow(rs);
                list.add(obj);
            }
        } catch (SQLException e) {
            logger.info(String.format("executeSQL:" + sql.replace("?", "%s"), params) + "Error Code : " + e.getErrorCode(), e);
        } finally {
            close(rs,ps,conn);
        }
        return list;
    }

    /**
     * 关闭操作
     *
     * @param rs        结果集
     * @param ps        处理
     * @param conn      连接
     */
    private void close(ResultSet rs,PreparedStatement ps, Connection conn) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                logger.info("Close Rs Error! Code : " + e.getErrorCode(), e);
            }
        }
        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException e) {
                logger.info("Close Ps Error! Code : " + e.getErrorCode(), e);
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.info("Close Conn Error! Code : " + e.getErrorCode(), e);
            }
        }
    }
}

interface IRowMap<T> {
    T mapRow(ResultSet rs) throws SQLException;
}
