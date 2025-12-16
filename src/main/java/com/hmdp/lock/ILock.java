package com.hmdp.lock;

public interface ILock {
    boolean tryLock(long timeoutSec);

    void unlock();
}
