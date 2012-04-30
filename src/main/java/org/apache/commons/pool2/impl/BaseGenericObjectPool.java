/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.pool2.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

/**
 * Base class that provides common functionality for {@link GenericObjectPool}
 * and {@link GenericKeyedObjectPool}. The primary reason this class exists is
 * reduce code duplication between the two pool implementations.
 */
public abstract class BaseGenericObjectPool implements NotificationEmitter {

    // Constants
    /**
     * Name of the JMX notification broadcast when the pool implementation
     * swallows an {@link Exception}.
     */
    public static final String NOTIFICATION_SWALLOWED_EXCEPTION =
            "SWALLOWED_EXCEPTION";
    private static final int SWALLOWED_EXCEPTION_QUEUE_SIZE = 10;

    // Configuration attributes
    private volatile int maxTotal =
            GenericKeyedObjectPoolConfig.DEFAULT_MAX_TOTAL;
    private volatile boolean blockWhenExhausted =
            GenericObjectPoolConfig.DEFAULT_BLOCK_WHEN_EXHAUSTED;
    private volatile long maxWaitMillis =
            GenericKeyedObjectPoolConfig.DEFAULT_MAX_WAIT_MILLIS;
    private volatile boolean testOnBorrow =
            GenericObjectPoolConfig.DEFAULT_TEST_ON_BORROW;
    private volatile boolean testOnReturn =
            GenericObjectPoolConfig.DEFAULT_TEST_ON_RETURN;


    // Internal (primarily state) attributes
    volatile boolean closed = false;

    /**
     * Class loader for evictor thread to use since in a J2EE or similar
     * environment the context class loader for the evictor thread may have
     * visibility of the correct factory. See POOL-161.
     */
    private final ClassLoader factoryClassLoader;


    // Monitoring (primarily JMX) attributes
    private final NotificationBroadcasterSupport jmxNotificationSupport;
    private final String creationStackTrace;
    private final Deque<String> swallowedExceptions = new LinkedList<String>();
    private final AtomicInteger swallowedExcpetionCount = new AtomicInteger(0);


    public BaseGenericObjectPool(BaseObjectPoolConfig config) {
        if (config.getJmxEnabled()) {
            this.jmxNotificationSupport = new NotificationBroadcasterSupport();
        } else {
            this.jmxNotificationSupport = null;
        }

        // Populate the swallowed exceptions queue
        for (int i = 0; i < SWALLOWED_EXCEPTION_QUEUE_SIZE; i++) {
            swallowedExceptions.add(null);
        }

        // Populate the creation stack trace
        this.creationStackTrace = getStackTrace(new Exception());

        // save the current CCL to be used later by the evictor Thread
        factoryClassLoader = Thread.currentThread().getContextClassLoader();
    }


    /**
     * Returns the maximum number of objects that can be allocated by the pool
     * (checked out to clients, or idle awaiting checkout) at a given time. When
     * non-positive, there is no limit to the number of objects that can be
     * managed by the pool at one time.
     *
     * @return the cap on the total number of object instances managed by the
     *         pool.
     * @see #setMaxTotal
     */
    public int getMaxTotal() {
        return maxTotal;
    }

    /**
     * Sets the cap on the number of objects that can be allocated by the pool
     * (checked out to clients, or idle awaiting checkout) at a given time. Use
     * a negative value for no limit.
     *
     * @param maxTotal  The cap on the total number of object instances managed
     *                  by the pool. Negative values mean that there is no limit
     *                  to the number of objects allocated by the pool.
     * @see #getMaxTotal
     */
    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

    /**
     * Returns whether to block when the <code>borrowObject()</code> method is
     * invoked when the pool is exhausted (the maximum number of "active"
     * objects has been reached).
     *
     * @return <code>true</code> if <code>borrowObject()</code> should block
     *         when the pool is exhausted
     * @see #setBlockWhenExhausted
     */
    public boolean getBlockWhenExhausted() {
        return blockWhenExhausted;
    }

    /**
     * Sets whether to block when the <code>borrowObject()</code> method is
     * invoked when the pool is exhausted (the maximum number of "active"
     * objects has been reached).
     *
     * @param blockWhenExhausted    <code>true</code> if
     *                              <code>borrowObject()</code> should block
     *                              when the pool is exhausted
     * @see #getBlockWhenExhausted
     */
    public void setBlockWhenExhausted(boolean blockWhenExhausted) {
        this.blockWhenExhausted = blockWhenExhausted;
    }

    /**
     * Returns the maximum amount of time (in milliseconds) the
     * <code>borrowObject()</code> method should block before throwing an
     * exception when the pool is exhausted and
     * {@link #getBlockWhenExhausted} is true. When less than 0, the
     * <code>borrowObject()</code> method may block indefinitely.
     *
     * @return the maximum number of milliseconds <code>borrowObject()</code>
     * will block.
     * @see #setMaxWaitMillis
     * @see #setBlockWhenExhausted
     */
    public long getMaxWaitMillis() {
        return maxWaitMillis;
    }

    /**
     * Sets the maximum amount of time (in milliseconds) the
     * <code>borrowObject()</code> method should block before throwing an
     * exception when the pool is exhausted and
     * {@link #getBlockWhenExhausted} is true. When less than 0, the
     * <code>borrowObject()</code> method may block indefinitely.
     *
     * @param maxWaitMillis the maximum number of milliseconds
     *                      <code>borrowObject()</code> will block or negative
     *                      for indefinitely.
     * @see #getMaxWaitMillis
     * @see #setBlockWhenExhausted
     */
    public void setMaxWaitMillis(long maxWaitMillis) {
        this.maxWaitMillis = maxWaitMillis;
    }

    /**
     * Returns whether objects borrowed from the pool will be validated before
     * being returned from the <code>borrowObject()</code> method. Validation is
     * performed by the factory associated with the pool. If the object fails to
     * validate, it will be dropped from the pool and destroyed, and a new
     * attempt will be made to borrow an object from the pool.  
     *
     * @return <code>true</code> if objects are validated before being returned
     *         from the <code>borrowObject()</code> method
     * @see #setTestOnBorrow
     */
    public boolean getTestOnBorrow() {
        return testOnBorrow;
    }

    /**
     * Sets whether objects borrowed from the pool will be validated before
     * being returned from the <code>borrowObject()</code> method. Validation is
     * performed by the factory associated with the pool. If the object fails to
     * validate, it will be dropped from the pool and destroyed, and a new
     * attempt will be made to borrow an object from the pool.  
     *
     * @param testOnBorrow  <code>true</code> if objects should be validated
     *                      before being returned from the
     *                      <code>borrowObject()</code> method
     * @see #getTestOnBorrow
     */
    public void setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }

    /**
     * Returns whether objects borrowed from the pool will be validated when
     * they are returned to the pool via the <code>returnObject()</code> method.
     * Validation is performed by the factory associated with the pool. If the
     * object fails to it will be destroyed rather then returned the pool.
     *
     * @return <code>true</code> if objects are validated on being returned to
     *         the pool via the <code>returnObject()</code> method
     * @see #setTestOnReturn
     */
    public boolean getTestOnReturn() {
        return testOnReturn;
    }

    /**
     * Sets whether objects borrowed from the pool will be validated when
     * they are returned to the pool via the <code>returnObject()</code> method.
     * Validation is performed by the factory associated with the pool. If the
     * object fails to it will be destroyed rather then returned the pool.
     *
     * @param testOnReturn <code>true</code> if objects are validated on being
     *                     returned to the pool via the
     *                     <code>returnObject()</code> method
     * @see #getTestOnReturn
     */
    public void setTestOnReturn(boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
    }
    
    
    /**
     * Closes the pool, destroys the remaining idle objects and, if registered
     * in JMX, deregisters it.
     */
    public abstract void close();

    /**
     * Has this pool instance been closed.
     * @return <code>true</code> when this pool has been closed.
     */
    public final boolean isClosed() {
        return closed;
    }

    /**
     * <p>Perform <code>numTests</code> idle object eviction tests, evicting
     * examined objects that meet the criteria for eviction. If
     * <code>testWhileIdle</code> is true, examined objects are validated
     * when visited (and removed if invalid); otherwise only objects that
     * have been idle for more than <code>minEvicableIdleTimeMillis</code>
     * are removed.</p>
     *
     * @throws Exception when there is a problem evicting idle objects.
     */
    public abstract void evict() throws Exception;

    /**
     * Throws an <code>IllegalStateException</code> if called when the pool has
     * been closed.
     *
     * @throws IllegalStateException if this pool has been closed.
     * @see #isClosed()
     */
    protected final void assertOpen() throws IllegalStateException {
        if (isClosed()) {
            throw new IllegalStateException("Pool not open");
        }
    }

    protected abstract void ensureMinIdle() throws Exception;


    // Monitoring (primarily JMX) related methods

    /**
     * Provides the name under which the pool has been registered with the
     * platform MBean server or <code>null</code> if the pool has not been
     * registered.
     */
    public abstract ObjectName getJmxName();

    /**
     * Provides the stack trace for the call that created this pool. JMX
     * registration may trigger a memory leak so it is important that pools are
     * deregistered when no longer used by calling the {@link #close()} method.
     * This method is provided to assist with identifying code that creates but
     * does not close it thereby creating a memory leak.
     */
    public String getCreationStackTrace() {
        return creationStackTrace;
    }

    /**
     * Lists the most recent exceptions that have been swallowed by the pool
     * implementation. Exceptions are typically swallowed when a problem occurs
     * while destroying an object.
     */
    public String[] getSwallowedExceptions() {
        List<String> temp =
                new ArrayList<String>(SWALLOWED_EXCEPTION_QUEUE_SIZE);
        synchronized (swallowedExceptions) {
            temp.addAll(swallowedExceptions);
        }
        return temp.toArray(new String[SWALLOWED_EXCEPTION_QUEUE_SIZE]);
    }

    protected final NotificationBroadcasterSupport getJmxNotificationSupport() {
        return jmxNotificationSupport;
    }

    protected String getStackTrace(Exception e) {
        // Need the exception in string form to prevent the retention of
        // references to classes in the stack trace that could trigger a memory
        // leak in a container environment
        Writer w = new StringWriter();
        PrintWriter pw = new PrintWriter(w);
        e.printStackTrace(pw);
        return w.toString();
    }

    protected void swallowException(Exception e) {
        String msg = getStackTrace(e);

        ObjectName oname = getJmxName();
        if (oname != null) {
            Notification n = new Notification(NOTIFICATION_SWALLOWED_EXCEPTION,
                    oname, swallowedExcpetionCount.incrementAndGet(), msg);
            getJmxNotificationSupport().sendNotification(n);
        }

        // Add the exception the queue, removing the oldest
        synchronized (swallowedExceptions) {
            swallowedExceptions.addLast(msg);
            swallowedExceptions.pollFirst();
        }
    }


    // Implement NotificationEmitter interface

    @Override
    public final void addNotificationListener(NotificationListener listener,
            NotificationFilter filter, Object handback)
            throws IllegalArgumentException {

        if (jmxNotificationSupport == null) {
            throw new UnsupportedOperationException("JMX is not enabled");
        }
        jmxNotificationSupport.addNotificationListener(
                listener, filter, handback);
    }

    @Override
    public final void removeNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException {

        if (jmxNotificationSupport == null) {
            throw new UnsupportedOperationException("JMX is not enabled");
        }
        jmxNotificationSupport.removeNotificationListener(listener);
    }

    @Override
    public final MBeanNotificationInfo[] getNotificationInfo() {

        if (jmxNotificationSupport == null) {
            throw new UnsupportedOperationException("JMX is not enabled");
        }
        return jmxNotificationSupport.getNotificationInfo();
    }

    @Override
    public final void removeNotificationListener(NotificationListener listener,
            NotificationFilter filter, Object handback)
            throws ListenerNotFoundException {

        if (jmxNotificationSupport == null) {
            throw new UnsupportedOperationException("JMX is not enabled");
        }
        jmxNotificationSupport.removeNotificationListener(
                listener, filter, handback);
    }


    // Inner classes

    /**
     * The idle object evictor {@link TimerTask}.
     *
     * @see GenericKeyedObjectPool#setTimeBetweenEvictionRunsMillis
     */
    protected class Evictor extends TimerTask {
        /**
         * Run pool maintenance.  Evict objects qualifying for eviction and then
         * ensure that the minimum number of idle instances are available.
         * Since the Timer that invokes Evictors is shared for all Pools but
         * pools may exist in different class loaders, the Evictor ensures that
         * any actions taken are under the class loader of the factory
         * associated with the pool.
         */
        @Override
        public void run() {
            ClassLoader savedClassLoader =
                    Thread.currentThread().getContextClassLoader();
            try {
                //  set the class loader for the factory
                Thread.currentThread().setContextClassLoader(
                        factoryClassLoader);

                //Evict from the pool
                try {
                    evict();
                } catch(Exception e) {
                    // ignored
                } catch(OutOfMemoryError oome) {
                    // Log problem but give evictor thread a chance to continue
                    // in case error is recoverable
                    oome.printStackTrace(System.err);
                }
                //Re-create idle instances.
                try {
                    ensureMinIdle();
                } catch (Exception e) {
                    // ignored
                }
            } finally {
                // restore the previous CCL
                Thread.currentThread().setContextClassLoader(savedClassLoader);
            }
        }
    }
}
