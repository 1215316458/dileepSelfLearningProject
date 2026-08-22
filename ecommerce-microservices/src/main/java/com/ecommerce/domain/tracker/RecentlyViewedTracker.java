package com.ecommerce.domain.tracker;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.ArrayList;

public class RecentlyViewedTracker implements Iterable<Long>{

    private final LinkedHashSet<Long> viewed = new LinkedHashSet<>();
    private static final int MAX_SIZE = 10;
    
    public void track(Long productId){
        if(viewed.contains(productId)){
            viewed.remove(productId);
        }
        else if(viewed.size() == MAX_SIZE){
            Long firstItem = viewed.iterator().next();
            viewed.remove(firstItem);
        }
        viewed.add(productId);
    }

    public int size() {
        return viewed.size();
    }

    @Override
    public Iterator<Long> iterator() {
        return new ReverseIterator();
    }
    private class ReverseIterator implements Iterator<Long>{
        private final List<Long> list = new ArrayList<>(viewed);
        private int index = list.size() -1;

        @Override
        public boolean hasNext() {
           return index >= 0;
        }

        @Override
        public Long next() {
            if(!hasNext()) throw new NoSuchElementException();
            return list.get(index--);            
        }


    }

}