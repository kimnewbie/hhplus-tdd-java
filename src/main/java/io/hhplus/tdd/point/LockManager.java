package io.hhplus.tdd.point;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class LockManager {
    private ConcurrentHashMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    public void lock(Long userId) {
        locks.computeIfAbsent(userId, id -> new ReentrantLock()).lock();
    }

    public void unlock(Long userId) {
        ReentrantLock lock = locks.get(userId);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            if (!lock.isLocked()) {
                locks.remove(userId);
            }
        }
    }
}
