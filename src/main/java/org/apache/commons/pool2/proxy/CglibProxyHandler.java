package org.apache.commons.pool2.proxy;

import java.lang.reflect.Method;

import org.apache.commons.pool2.UsageTracking;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;


public class CglibProxyHandler<T> extends BaseProxyHandler<T>
        implements MethodInterceptor {

    private final UsageTracking<T> usageTracking;

    CglibProxyHandler(T pooledObject, UsageTracking<T> usageTracking) {
        super(pooledObject);
        this.usageTracking = usageTracking;
    }

    @Override
    public Object intercept(Object object, Method method, Object[] args,
            MethodProxy methodProxy) throws Throwable {
        validateProxiedObject();
        T pooledObject = getPooledObject();
        if (usageTracking != null) {
            usageTracking.use(pooledObject);
        }

        return method.invoke(pooledObject, args);
    }
}
