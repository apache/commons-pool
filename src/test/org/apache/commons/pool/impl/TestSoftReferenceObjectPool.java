/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//pool/src/test/org/apache/commons/pool/impl/TestSoftReferenceObjectPool.java,v 1.1 2002/03/19 17:27:54 rwaldhoff Exp $
 * $Revision: 1.1 $
 * $Date: 2002/03/19 17:27:54 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.commons.pool.impl;

import junit.framework.*;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;

/**
 * @author Rodney Waldhoff
 * @version $Revision: 1.1 $ $Date: 2002/03/19 17:27:54 $
 */
public class TestSoftReferenceObjectPool extends TestCase {
    public TestSoftReferenceObjectPool(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestSoftReferenceObjectPool.class);
    }

    public static void main(String args[]) {
        String[] testCaseName = { TestSoftReferenceObjectPool.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    private SoftReferenceObjectPool pool = null;

    public void setUp() {
        pool = new SoftReferenceObjectPool(
            new PoolableObjectFactory()  {
                int counter = 0;
                public Object makeObject() { return String.valueOf(counter++); }
                public void destroyObject(Object obj) { }
                public boolean validateObject(Object obj) { return true; }
                public void activateObject(Object obj) { }
                public void passivateObject(Object obj) { }
            }
            );
    }

    public void testBorrow() throws Exception {
        Object obj0 = pool.borrowObject();
        assertEquals("0",obj0);
        Object obj1 = pool.borrowObject();
        assertEquals("1",obj1);
        Object obj2 = pool.borrowObject();
        assertEquals("2",obj2);
    }

    public void testBorrowReturn() throws Exception {
        Object obj0 = pool.borrowObject();
        assertEquals("borrowObject from an empty pool should create a new instance.","0",obj0);
        Object obj1 = pool.borrowObject();
        assertEquals("A second borrowObject from an empty pool should create a second instance.","1",obj1);
        Object obj2 = pool.borrowObject();
        assertEquals("A third borrowObject from an empty pool should create a third instance.","2",obj2);

        pool.returnObject(obj2);
        obj2 = pool.borrowObject();
        assertEquals("Having returned the third instance to the empty pool, borrowObject should return it.","2",obj2);

        pool.returnObject(obj1);
        obj1 = pool.borrowObject();
        assertEquals("Having returned the second instance to the empty pool, borrowObject should return it.","1",obj1);

        pool.returnObject(obj0);
        pool.returnObject(obj2);
        obj2 = pool.borrowObject();
        assertEquals("Having returned the first, then third instance to the empty pool, borrowObject should return the third instance.","2",obj2);
        obj0 = pool.borrowObject();
        assertEquals("Having returned the first, then third instance to the empty pool, the second call to borrowObject should return the first instance.","0",obj0);
    }

    public void testNumActiveNumIdle() throws Exception {
        assertEquals(0,pool.numActive());
        assertEquals(0,pool.numIdle());
        Object obj0 = pool.borrowObject();
        assertEquals(1,pool.numActive());
        assertEquals(0,pool.numIdle());
        Object obj1 = pool.borrowObject();
        assertEquals(2,pool.numActive());
        assertEquals(0,pool.numIdle());
        pool.returnObject(obj1);
        assertEquals(1,pool.numActive());
        assertEquals(1,pool.numIdle());
        pool.returnObject(obj0);
        assertEquals(0,pool.numActive());
        assertEquals(2,pool.numIdle());
    }

    public void testClear() throws Exception {
        assertEquals(0,pool.numActive());
        assertEquals(0,pool.numIdle());
        Object obj0 = pool.borrowObject();
        Object obj1 = pool.borrowObject();
        assertEquals(2,pool.numActive());
        assertEquals(0,pool.numIdle());
        pool.returnObject(obj1);
        pool.returnObject(obj0);
        assertEquals(0,pool.numActive());
        assertEquals(2,pool.numIdle());
        pool.clear();
        assertEquals(0,pool.numActive());
        assertEquals(0,pool.numIdle());
        Object obj2 = pool.borrowObject();
        assertEquals("2",obj2);
    }

}
