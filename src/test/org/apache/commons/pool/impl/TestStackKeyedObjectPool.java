/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//pool/src/test/org/apache/commons/pool/impl/TestStackKeyedObjectPool.java,v 1.1 2001/04/14 16:42:16 rwaldhoff Exp $
 * $Revision: 1.1 $
 * $Date: 2001/04/14 16:42:16 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2001 The Apache Software Foundation.  All rights
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
import org.apache.commons.pool.*;

/**
 * @author Rodney Waldhoff
 * @version $Id: TestStackKeyedObjectPool.java,v 1.1 2001/04/14 16:42:16 rwaldhoff Exp $
 */
public class TestStackKeyedObjectPool extends TestCase {
    public TestStackKeyedObjectPool(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestStackKeyedObjectPool.class);
    }

    public static void main(String args[]) {
        String[] testCaseName = { TestStackKeyedObjectPool.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    private StackKeyedObjectPool pool = null;

    public void setUp() {
        pool = new StackKeyedObjectPool(
            new KeyedPoolableObjectFactory()  {
                int counter = 0;
                public Object makeObject(Object key) { return String.valueOf(key) + String.valueOf(counter++); }
                public void destroyObject(Object key, Object obj) { }
                public boolean validateObject(Object key, Object obj) { return true; }
                public void activateObject(Object key, Object obj) { }
                public void passivateObject(Object key, Object obj) { }
            }
            );
    }

    public void testBorrow() {
        Object obj0 = pool.borrowObject("");
        assertEquals("0",obj0);
        Object obj1 = pool.borrowObject("");
        assertEquals("1",obj1);
        Object obj2 = pool.borrowObject("");
        assertEquals("2",obj2);
    }

    public void testBorrowReturn() {
        Object obj0 = pool.borrowObject("");
        assertEquals("0",obj0);
        Object obj1 = pool.borrowObject("");
        assertEquals("1",obj1);
        Object obj2 = pool.borrowObject("");
        assertEquals("2",obj2);
        pool.returnObject("",obj2);
        obj2 = pool.borrowObject("");
        assertEquals("2",obj2);
        pool.returnObject("",obj1);
        obj1 = pool.borrowObject("");
        assertEquals("1",obj1);
        pool.returnObject("",obj0);
        pool.returnObject("",obj2);
        obj2 = pool.borrowObject("");
        assertEquals("2",obj2);
        obj0 = pool.borrowObject("");
        assertEquals("0",obj0);
    }

    public void testNumActiveNumIdle() {
        assertEquals(0,pool.numActive(""));
        assertEquals(0,pool.numIdle(""));
        Object obj0 = pool.borrowObject("");
        assertEquals(1,pool.numActive(""));
        assertEquals(0,pool.numIdle(""));
        Object obj1 = pool.borrowObject("");
        assertEquals(2,pool.numActive(""));
        assertEquals(0,pool.numIdle(""));
        pool.returnObject("",obj1);
        assertEquals(1,pool.numActive(""));
        assertEquals(1,pool.numIdle(""));
        pool.returnObject("",obj0);
        assertEquals(0,pool.numActive(""));
        assertEquals(2,pool.numIdle(""));
    }

    public void testNumActiveNumIdle2() {
        assertEquals(0,pool.numActive());
        assertEquals(0,pool.numIdle());
        assertEquals(0,pool.numActive("A"));
        assertEquals(0,pool.numIdle("A"));
        assertEquals(0,pool.numActive("B"));
        assertEquals(0,pool.numIdle("B"));

        Object objA0 = pool.borrowObject("A");
        Object objB0 = pool.borrowObject("B");

        assertEquals(2,pool.numActive());
        assertEquals(0,pool.numIdle());
        assertEquals(1,pool.numActive("A"));
        assertEquals(0,pool.numIdle("A"));
        assertEquals(1,pool.numActive("B"));
        assertEquals(0,pool.numIdle("B"));

        Object objA1 = pool.borrowObject("A");
        Object objB1 = pool.borrowObject("B");

        assertEquals(4,pool.numActive());
        assertEquals(0,pool.numIdle());
        assertEquals(2,pool.numActive("A"));
        assertEquals(0,pool.numIdle("A"));
        assertEquals(2,pool.numActive("B"));
        assertEquals(0,pool.numIdle("B"));

        pool.returnObject("A",objA0);
        pool.returnObject("B",objB0);

        assertEquals(2,pool.numActive());
        assertEquals(2,pool.numIdle());
        assertEquals(1,pool.numActive("A"));
        assertEquals(1,pool.numIdle("A"));
        assertEquals(1,pool.numActive("B"));
        assertEquals(1,pool.numIdle("B"));

        pool.returnObject("A",objA1);
        pool.returnObject("B",objB1);

        assertEquals(0,pool.numActive());
        assertEquals(4,pool.numIdle());
        assertEquals(0,pool.numActive("A"));
        assertEquals(2,pool.numIdle("A"));
        assertEquals(0,pool.numActive("B"));
        assertEquals(2,pool.numIdle("B"));
    }

    public void testClear() {
        assertEquals(0,pool.numActive(""));
        assertEquals(0,pool.numIdle(""));
        Object obj0 = pool.borrowObject("");
        Object obj1 = pool.borrowObject("");
        assertEquals(2,pool.numActive(""));
        assertEquals(0,pool.numIdle(""));
        pool.returnObject("",obj1);
        pool.returnObject("",obj0);
        assertEquals(0,pool.numActive(""));
        assertEquals(2,pool.numIdle(""));
        pool.clear("");
        assertEquals(0,pool.numActive(""));
        assertEquals(0,pool.numIdle(""));
        Object obj2 = pool.borrowObject("");
        assertEquals("2",obj2);
    }

    public void testIdleCap() {
        Object[] active = new Object[100];
        for(int i=0;i<100;i++) {
            active[i] = pool.borrowObject("");
        }
        assertEquals(100,pool.numActive(""));
        assertEquals(0,pool.numIdle(""));
        for(int i=0;i<100;i++) {
            pool.returnObject("",active[i]);
            assertEquals(99 - i,pool.numActive(""));
            assertEquals((i < 8 ? i+1 : 8),pool.numIdle(""));
        }
    }
}
