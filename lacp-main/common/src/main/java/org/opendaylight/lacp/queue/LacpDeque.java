package org.opendaylight.lacp.queue;

import java.util.concurrent.ConcurrentLinkedDeque;


public class LacpDeque<E> {

    private ConcurrentLinkedDeque<E> list = new ConcurrentLinkedDeque<E>();

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

    public boolean hasItems() {
            return !list.isEmpty();
    }

    public int size() {
            return list.size();
    }

    public boolean remove(){
            E item;
            while ((item = list.poll()) != null) {
                    System.out.println("Removed: " + item);
            }
            return true;
     }

    public boolean addFirst(E item){
        return list.offerFirst(item);
    }
    public E read(){
	return list.peek();
    }	
}
