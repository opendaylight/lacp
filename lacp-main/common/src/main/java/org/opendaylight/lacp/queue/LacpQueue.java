/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.lacp.queue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class LacpQueue<E> {

    private BlockingQueue<E> list = new LinkedBlockingQueue<E>();

    public boolean isQueuePresent(){
            if(list != null){
                    return true;
            }else{
                    return false;
            }
    }

    public boolean enqueue(E item) {
            return (list.add(item));
    }

    public E dequeue() {
            return list.poll();
    }

    public E dequeue(long timeoutMillis) throws InterruptedException {
        return list.poll(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    public boolean hasItems() {
            return !list.isEmpty();
    }

    public int size() {
            return list.size();
    }

    public E read(){
        return list.peek();
    }

    public boolean remove(){
            E item;
            while ((item = list.poll()) != null) {
            }
            return true;
     }
}
