package com.ecommerce.domain.concurrency;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DeadlockDemo {

    private final Lock inventoryLock = new ReentrantLock();
    private final Lock paymentLock   = new ReentrantLock();

    // DEADLOCK: Thread1 holds inventoryLock, waits for paymentLock
    //           Thread2 holds paymentLock,   waits for inventoryLock
    //           Both wait forever — circular dependency
    public void deadlockScenario() throws InterruptedException {
        Thread t1 = new Thread(() -> {
            inventoryLock.lock();
            try {
                simulateDelay(50); // give t2 time to grab paymentLock
                paymentLock.lock(); // BLOCKS — t2 holds this
                try {
                    System.out.println("  Thread1: processed (will never print in deadlock)");
                } finally { paymentLock.unlock(); }
            } finally { inventoryLock.unlock(); }
        }, "Thread1-Deadlock");

        Thread t2 = new Thread(() -> {
            paymentLock.lock();
            try {
                simulateDelay(50);
                inventoryLock.lock(); // BLOCKS — t1 holds this
                try {
                    System.out.println("  Thread2: processed (will never print in deadlock)");
                } finally { inventoryLock.unlock(); }
            } finally { paymentLock.unlock(); }
        }, "Thread2-Deadlock");

        t1.start();
        t2.start();

        // wait 300ms — if still alive, deadlock confirmed
        t1.join(300);
        t2.join(300);

        boolean deadlocked = t1.isAlive() && t2.isAlive();
        System.out.println("  Deadlock detected: " + deadlocked);

        // interrupt both threads to unblock them
        t1.interrupt();
        t2.interrupt();
        t1.join(200);
        t2.join(200);
    }

    // FIX: consistent lock ordering — ALWAYS acquire inventoryLock before paymentLock
    // Both threads acquire locks in the same order → no circular wait → no deadlock
    public void fixedScenario() throws InterruptedException {
        Thread t1 = new Thread(() -> {
            inventoryLock.lock();
            try {
                simulateDelay(50);
                paymentLock.lock();
                try {
                    System.out.println("  Thread1-Fixed: processed ✓");
                } finally { paymentLock.unlock(); }
            } finally { inventoryLock.unlock(); }
        }, "Thread1-Fixed");

        Thread t2 = new Thread(() -> {
            // same order as t1: inventoryLock first, then paymentLock
            inventoryLock.lock();
            try {
                simulateDelay(50);
                paymentLock.lock();
                try {
                    System.out.println("  Thread2-Fixed: processed ✓");
                } finally { paymentLock.unlock(); }
            } finally { inventoryLock.unlock(); }
        }, "Thread2-Fixed");

        t1.start();
        t2.start();
        t1.join(1000);
        t2.join(1000);

        System.out.println("  Both threads completed: " + (!t1.isAlive() && !t2.isAlive()));
    }

    // tryLock with timeout — alternative fix: give up if lock not acquired within timeout
    public void tryLockScenario() throws InterruptedException {
        Thread t1 = new Thread(() -> {
            try {
                if (inventoryLock.tryLock(100, TimeUnit.MILLISECONDS)) {
                    try {
                        simulateDelay(50);
                        if (paymentLock.tryLock(100, TimeUnit.MILLISECONDS)) {
                            try {
                                System.out.println("  Thread1-TryLock: processed ✓");
                            } finally { paymentLock.unlock(); }
                        } else {
                            System.out.println("  Thread1-TryLock: gave up waiting for paymentLock");
                        }
                    } finally { inventoryLock.unlock(); }
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "Thread1-TryLock");

        t1.start();
        t1.join(500);
    }

    private void simulateDelay(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
