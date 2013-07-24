package org.apache.commons.pool2.impl;

import org.apache.commons.pool2.KeyedPoolableObjectFactory;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PoolableObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;

/**
 * Implementation specific utilities.
 */
public class PoolImplUtils {

    /**
     * Wraps a {@link PoolableObjectFactory} to convert it into a
     * {@link PooledObjectFactory}.
     */
    public static <T> PooledObjectFactory<T> poolableToPooledObjectFactory(
            PoolableObjectFactory<T> innerFactory) {
        return new PoolableObjectFactoryWrapper<T>(innerFactory);
    }


    /**
     * Wraps a {@link KeyedPoolableObjectFactory} to convert it into a
     * {@link KeyedPooledObjectFactory}.
     */
    public static <K,V> KeyedPooledObjectFactory<K,V> poolableToKeyedPooledObjectFactory(
            KeyedPoolableObjectFactory<K,V> innerFactory) {
        return new KeyedPoolableObjectFactoryWrapper<K,V>(innerFactory);
    }


    private static class PoolableObjectFactoryWrapper<T>
            implements PooledObjectFactory<T> {

        private final PoolableObjectFactory<T> innerFactory;

        public PoolableObjectFactoryWrapper(PoolableObjectFactory<T> innerFactory) {
            this.innerFactory = innerFactory;
        }

        @Override
        public PooledObject<T> makeObject() throws Exception {
            return new PooledObjectImpl<T>(innerFactory.makeObject());
        }

        @Override
        public void destroyObject(PooledObject<T> p) throws Exception {
            innerFactory.destroyObject(p.getObject());
        }

        @Override
        public boolean validateObject(PooledObject<T> p) {
            return innerFactory.validateObject(p.getObject());
        }

        @Override
        public void activateObject(PooledObject<T> p) throws Exception {
            innerFactory.activateObject(p.getObject());
        }

        @Override
        public void passivateObject(PooledObject<T> p) throws Exception {
            innerFactory.passivateObject(p.getObject());
        }
    }


    private static class KeyedPoolableObjectFactoryWrapper<K,V>
            implements KeyedPooledObjectFactory<K,V> {

        private final KeyedPoolableObjectFactory<K,V> innerFactory;

        public KeyedPoolableObjectFactoryWrapper(
                KeyedPoolableObjectFactory<K,V> innerFactory) {
            this.innerFactory = innerFactory;
        }

        @Override
        public PooledObject<V> makeObject(K key) throws Exception {
            V obj = innerFactory.makeObject(key);
            return new PooledObjectImpl<V>(obj);
        }

        @Override
        public void destroyObject(K key, PooledObject<V> p) throws Exception {
            innerFactory.destroyObject(key, p.getObject());
        }

        @Override
        public boolean validateObject(K key, PooledObject<V> p) {
            return innerFactory.validateObject(key, p.getObject());
        }

        @Override
        public void activateObject(K key, PooledObject<V> p) throws Exception {
            innerFactory.activateObject(key, p.getObject());
        }

        @Override
        public void passivateObject(K key, PooledObject<V> p)
                throws Exception {
            innerFactory.passivateObject(key, p.getObject());
        }
    }
}
