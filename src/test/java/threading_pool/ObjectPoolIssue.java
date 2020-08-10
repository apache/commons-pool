package threading_pool;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.BaseObjectPoolConfig;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

/*
 On my box with 4 cores this test fails at between 5s and 900s with an average of 240s (data from 10 runs of test).

 Example stack trace:
java.util.concurrent.ExecutionException: java.lang.NullPointerException
    at java.util.concurrent.FutureTask.report(FutureTask.java:122)
    at java.util.concurrent.FutureTask.get(FutureTask.java:192)
    at threading_pool.ObjectPoolIssue.run(ObjectPoolIssue.java:62)
    at threading_pool.ObjectPoolIssue.main(ObjectPoolIssue.java:22)
    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
    at java.lang.reflect.Method.invoke(Method.java:498)
    at com.intellij.rt.execution.application.AppMain.main(AppMain.java:147)
Caused by: java.lang.NullPointerException
    at org.apache.commons.pool2.impl.GenericKeyedObjectPool.create(GenericKeyedObjectPool.java:1028)
    at org.apache.commons.pool2.impl.GenericKeyedObjectPool.borrowObject(GenericKeyedObjectPool.java:380)
    at org.apache.commons.pool2.impl.GenericKeyedObjectPool.borrowObject(GenericKeyedObjectPool.java:279)
    at threading_pool.ObjectPoolIssue$Task.call(ObjectPoolIssue.java:105)
    at java.util.concurrent.FutureTask.run(FutureTask.java:266)
    at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
    at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
    at java.lang.Thread.run(Thread.java:745)


 */
public final class ObjectPoolIssue {
    private final Object m_lockObject = new Object();

    public static void main(String[] args) {
        try {
            new ObjectPoolIssue().run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void run() throws Exception {
        GenericKeyedObjectPoolConfig poolConfig = new GenericKeyedObjectPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxTotalPerKey(5);
        poolConfig.setMinIdlePerKey(-1);
        poolConfig.setMaxIdlePerKey(-1);
        poolConfig.setLifo(true);
        poolConfig.setFairness(true);
        poolConfig.setMaxWaitMillis(30 * 1000);
        poolConfig.setMinEvictableIdleTimeMillis(-1);
        poolConfig.setSoftMinEvictableIdleTimeMillis(-1);
        poolConfig.setNumTestsPerEvictionRun(1);
        poolConfig.setTestOnCreate(false);
        poolConfig.setTestOnBorrow(false);
        poolConfig.setTestOnReturn(false);
        poolConfig.setTestWhileIdle(false);
        poolConfig.setTimeBetweenEvictionRunsMillis(5 * 1000);
        poolConfig.setEvictionPolicyClassName(BaseObjectPoolConfig.DEFAULT_EVICTION_POLICY_CLASS_NAME);
        poolConfig.setBlockWhenExhausted(false);
        poolConfig.setJmxEnabled(false);
        poolConfig.setJmxNameBase(null);
        poolConfig.setJmxNamePrefix(null);

        GenericKeyedObjectPool<Integer, Object> pool = new GenericKeyedObjectPool<>(new ObjectFactory(), poolConfig);

        //number of threads to reproduce is finicky.  this count seems to be best for my 4 core box.
        //too many doesn't reproduce it ever, too few doesn't either.
        ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
        long startTime = System.currentTimeMillis();
        try {
            while (true) {
                List<Task> tasks = createTasks(pool);
                List<Future<Object>> futures = service.invokeAll(tasks);
                for (Future<Object> future : futures) {
                    future.get();
                }
            }
        } finally {
            System.out.println("Time: " + (System.currentTimeMillis() - startTime)/1000.0);
            service.shutdown();
        }
    }

    private List<Task> createTasks(GenericKeyedObjectPool<Integer, Object> pool) {
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            tasks.add(new Task(pool, i));
        }
        return tasks;
    }

    private class ObjectFactory extends BaseKeyedPooledObjectFactory<Integer, Object> {
        @Override
        public Object create(Integer s) throws Exception {
            return new TestObject();
        }

        @Override
        public PooledObject<Object> wrap(Object o) {
            return new DefaultPooledObject<>(o);
        }
    }

    private class TestObject {}

    private class Task implements Callable<Object> {
        private final GenericKeyedObjectPool<Integer, Object> m_pool;
        private final int m_key;

        Task(GenericKeyedObjectPool<Integer, Object> pool, int count) {
            m_pool = pool;
            m_key = count % 20;
        }

        @Override
        public Object call() throws Exception {
            try {
                Object value;
                synchronized (m_lockObject) {
                    value = m_pool.borrowObject(m_key);
                }
                //don't make this too long or it won't reproduce, and don't make it zero or it won't reproduce
                //constant low value also doesn't reproduce
                busyWait(System.currentTimeMillis() % 4);
                synchronized (m_lockObject) {
                    m_pool.returnObject(m_key, value);
                }
                return "success";
            } catch (NoSuchElementException e) {
                //ignore, we've exhausted the pool
                //not sure whether what we do here matters for reproducing
                busyWait(System.currentTimeMillis() % 20);
                return "exhausted";
            }
        }

        private void busyWait(long timeMillis) {
            //busy waiting intentionally as a simple thread.sleep fails to reproduce
            long endTime = System.currentTimeMillis() + timeMillis;
            while (System.currentTimeMillis() < endTime);
        }
    }
}
