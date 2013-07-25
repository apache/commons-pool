package org.apache.commons.pool2.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;

import org.apache.commons.pool2.PooledObject;

public class DefaultPooledObjectInfo implements DefaultPooledObjectInfoMBean {

    private final PooledObject<?> pooledObject;

    public DefaultPooledObjectInfo(PooledObject<?> pooledObject) {
        this.pooledObject = pooledObject;
    }

    @Override
    public long getCreateTime() {
        return pooledObject.getCreateTime();
    }

    @Override
    public String getCreateTimeFormatted() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        return sdf.format(Long.valueOf(pooledObject.getCreateTime()));
    }

    @Override
    public long getLastBorrowTime() {
        return pooledObject.getLastBorrowTime();
    }

    @Override
    public String getLastBorrowTimeFormatted() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        return sdf.format(Long.valueOf(pooledObject.getLastBorrowTime()));
    }

    @Override
    public String getLastBorrowTrace() {
        StringWriter sw = new StringWriter();
        pooledObject.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    @Override
    public long getLastReturnTime() {
        return pooledObject.getLastReturnTime();
    }

    @Override
    public String getLastReturnTimeFormatted() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        return sdf.format(Long.valueOf(pooledObject.getLastReturnTime()));
    }
}
