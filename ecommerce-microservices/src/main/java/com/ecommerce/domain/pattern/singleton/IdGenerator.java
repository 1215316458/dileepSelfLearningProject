package com.ecommerce.domain.pattern.singleton;

import java.util.concurrent.atomic.AtomicLong;

public class IdGenerator {
    private static final IdGenerator idGenerator = new IdGenerator();
    private static final AtomicLong atomicLong = new AtomicLong(0);
    private IdGenerator(){}
    public static IdGenerator getInstance(){
        return idGenerator;
    }
    public long getId(){
        return atomicLong.incrementAndGet();
    }  

}
