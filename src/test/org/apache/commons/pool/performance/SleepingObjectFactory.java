/*
 * $Source: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//pool/src/test/org/apache/commons/pool/performance/SleepingObjectFactory.java,v $
 * $Revision: 1.3 $
 * $Date: 2003/10/09 21:45:57 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2003 The Apache Software Foundation.  All rights
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
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation - http://www.apache.org/"
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
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
 * http://www.apache.org/
 *
 */

package org.apache.commons.pool.performance;

import org.apache.commons.pool.PoolableObjectFactory;

/**
 * Sleepy ObjectFactory (everything takes a while longer)
 * 
 * @author Dirk Verbeeck
 */
public class SleepingObjectFactory implements PoolableObjectFactory {

    private int counter = 0;
    private boolean debug = false;

	public Object makeObject() throws Exception {
        Object obj = new Integer(counter++);
        debug("makeObject", obj);
        sleep(500);
		return obj;
	}

	public void destroyObject(Object obj) throws Exception {
        debug("destroyObject", obj);
        sleep(250);
	}

	public boolean validateObject(Object obj) {
        debug("validateObject", obj);
        sleep(30);
		return true;
	}

	public void activateObject(Object obj) throws Exception {
        debug("activateObject", obj);
        sleep(10);
	}

	public void passivateObject(Object obj) throws Exception {
        debug("passivateObject", obj);
        sleep(10);
	}
    
    private void debug(String method, Object obj) {
        if (debug) {
            String thread = "thread" + Thread.currentThread().getName();
            System.out.println(thread + ": " + method + " " + obj);
        }
    }
    
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        }
        catch (InterruptedException e) {
        }
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean b) {
        debug = b;
    }
}
