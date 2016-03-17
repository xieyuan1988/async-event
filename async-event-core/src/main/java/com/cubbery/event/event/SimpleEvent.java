/**
 * Copyright (c) 2015, www.cubbery.com. All rights reserved.
 */
package com.cubbery.event.event;

import com.cubbery.event.channel.ChannelData;
import com.cubbery.event.handler.EventHandler;
import com.cubbery.event.utils.JsonUtils;

import java.util.Date;

/**
 * event 基础实现，mapping到数据库逻辑结构。
 */
public class SimpleEvent {
    private long id;
    private String data;
    private int status;
    private String mark;
    private Date createdTime;
    private Date modifiedTime;
    private String type;//event type
    private String expression;//handler

    public void setId(long id) {
        this.id = id;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public Date getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(Date modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    /**
     * 将从队列中取出的数据格式化成统一的数据结构，便于保存和后续统一使用
     *
     * @param e 任意obj或者包装结构
     * @return
     */
    public static SimpleEvent create(Object e,EventHandler handler) {
        if(e != null ) {
            SimpleEvent simpleEvent = new SimpleEvent();
            if(e instanceof ChannelData) {
                simpleEvent.setId(((ChannelData)e).getId());
            }
            simpleEvent.setData(JsonUtils.serialize(e));
            simpleEvent.setType(e.getClass().getCanonicalName());
            simpleEvent.setExpression(handler.expression());
            return simpleEvent;
        }
        return null;
    }

    /**
     * 由统一格式，反解成event的格式
     *
     * @param event     统一格式
     * @param clazz     目标对象类型
     * @return          反解失败返回null
     */
    public static Object reCreate(SimpleEvent event,Class<?> clazz) {
        if(clazz == null) return null;
        if(!SimpleEvent.class.equals(clazz)) {
            //还原事件类型
            if(event.getData() == null) {
                try {
                    return clazz.newInstance();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                return null;
            }
            return JsonUtils.deSerialize(event.getData(), clazz);
        }
        return event;
    }

    public long getId() {
        return this.id;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
