package com.ecommerce.domain.repository.concurrency;

public class RequestContext {

    // ThreadLocal — each thread has its own isolated slot for this variable
    // Thread A calling set(1L) does NOT affect Thread B's value
    // Critical in thread pools (e.g. web servers): threads are reused across requests,
    // so forgetting remove() leaks the previous request's userId into the next request
    private static final ThreadLocal<Long> currentUserId = new ThreadLocal<>();

    public static void set(Long userId) {
        currentUserId.set(userId);
    }

    public static Long get() {
        return currentUserId.get();
    }

    // MUST call clear() when the request/task is done
    // Thread pools reuse threads — without clear(), the next task on this thread
    // inherits the previous task's userId (stale data + memory leak)
    public static void clear() {
        currentUserId.remove();
    }
}
