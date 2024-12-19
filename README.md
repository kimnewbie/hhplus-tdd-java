# 동시성 제어 방식에 대한 분석 및 보고서

## 1. 동시성 테스트 개요

이번 테스트는 여러 사용자가 동시에 포인트 충전 요청을 할 때, 데이터 무결성이 유지되는지 확인하는 동시성 테스트입니다. 테스트의 주요 목적은 동시에 여러 스레드가 데이터에 접근할 때 발생할 수 있는 문제를
방지하고, 데이터 무결성이 보장되는지 검증하는 것입니다.

### 테스트 시나리오

- **Given**: 5명의 사용자가 포인트 충전 시도를 합니다.
- **When**: 각 사용자는 별개의 스레드로 포인트를 충전합니다.
- **Then**: 모든 사용자의 포인트가 충전되어야 하며, 데이터 무결성이 유지되어야 합니다.

## 2. 동시성 제어 방식

동시성 문제를 해결하기 위해 여러 가지 동시성 제어 방식이 사용됩니다. 이 중 `Synchronized`와 `ReentrantLock`은 대표적인 동기화 기법입니다.

### 2.1. `Synchronized` vs `ReentrantLock`

- **Synchronized**
    - 자바에서 제공하는 내장 동기화 키워드입니다.
    - 메서드 또는 코드 블록에 적용되어, 해당 코드가 실행될 때 다른 스레드가 접근하지 못하도록 동기화합니다.
    - `Synchronized`는 해당 코드 블록에 진입할 때만 락을 획득하며, 코드 블록을 벗어나면 자동으로 락이 해제됩니다.
    - 간단하고 직관적이지만, 락의 세밀한 제어가 어렵고, 타임아웃 설정과 같은 추가적인 기능을 제공하지 않습니다.

- **ReentrantLock**
    - `java.util.concurrent.locks` 패키지에서 제공하는 락으로, `Synchronized`보다 더 세밀한 제어를 가능하게 합니다.
    - 락의 획득과 해제, 타임아웃 설정, 공정성 보장 등 다양한 기능을 제공합니다.
    - `ReentrantLock`은 명시적으로 `lock()`과 `unlock()` 메서드를 사용하여 락을 관리합니다. 이로 인해 더 높은 수준의 제어가 가능합니다.
    - 예를 들어, 여러 스레드가 동시에 락을 기다릴 때 `ReentrantLock`은 공정성(fairness)을 보장할 수 있는 기능을 제공합니다.

### 2.2. `ReentrantLock` 선택 이유

이번 프로젝트에서는 `ReentrantLock`을 사용한 이유는 다음과 같습니다:

- **세밀한 락 관리**: `ReentrantLock`은 락을 명시적으로 관리할 수 있어, 보다 유연한 동기화가 가능합니다. `lock()`과 `unlock()` 메서드를 통해 락을 명시적으로 관리하고, 예외 발생
  시에도 락이 제대로 해제되도록 할 수 있습니다.
- **타임아웃 기능**: `ReentrantLock`은 타임아웃을 설정하여 일정 시간 내에 락을 획득하지 못하면 대기하지 않고 처리할 수 있게 합니다. 이는 데드락을 방지하고 더 효율적인 동시성 관리를 가능하게
  합니다.
- **공정성(fairness)**: `ReentrantLock`은 공정성을 설정할 수 있어, 락을 기다리는 스레드가 순차적으로 락을 획득하도록 보장할 수 있습니다. 이는 여러 스레드가 동시에 락을 요청할 때 더
  공정한 처리를 제공합니다.
- **성능 최적화**: `Synchronized`보다 성능이 더 나은 경우가 많으며, 락을 풀기 전에 추가 작업을 수행할 수 있어 성능 최적화가 가능합니다.

## 3. LockManager의 구현

`LockManager` 클래스는 `ReentrantLock`을 사용하여 각 사용자별로 락을 관리합니다. 사용자별로 락을 개별적으로 관리하여, 특정 사용자가 포인트 충전 작업을 하는 동안 다른 사용자가 동시에 해당
작업을 수행하지 않도록 보장합니다.

```java
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
```

### LockManager의 기능

lock() 메서드는 사용자 ID별로 락을 획득합니다. 해당 사용자의 락이 없으면 새롭게 생성하여 락을 걸고, 이미 락이 있으면 이를 재사용합니다.
unlock() 메서드는 락을 해제합니다. 해당 락이 현재 스레드에 의해 획득된 락인지 확인하고, 락을 해제한 후 락이 더 이상 필요하지 않으면 locks에서 제거합니다.

## 4. 동시성 테스트 구현

LockManager와 ReentrantLock을 활용하여 동시성 제어가 제대로 이루어지는지 테스트하기 위해, 5명의 사용자가 동시에 포인트를 충전하는 시나리오를 구현하였습니다.

```java

@Test
@DisplayName("동시성 테스트: 여러 사용자가 동시에 포인트를 충전할 때 데이터 무결성 유지")
void concurrentChargePoint() throws InterruptedException {
    // Given: 5명의 사용자가 포인트 충전 시도
    long[] userIds = {1L, 2L, 3L, 4L, 5L};

    // 각 사용자에 대한 UserPoint를 mock
    mockUserPoints(userIds);

    // 실행할 쓰레드 수
    ExecutorService executor = Executors.newFixedThreadPool(userIds.length);

    // 각 쓰레드에서 포인트 충전 요청
    for (long userId : userIds) {
        executor.submit(() -> pointService.chargeUserPoint(userId, 100));
    }

    // 모든 작업이 끝날 때까지 기다림
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.MINUTES);

    // Then: 모든 유저의 포인트가 충전되어야 함
    verifyUserPointOperations(userIds);
}

private void mockUserPoints(long[] userIds) {
    for (long userId : userIds) {
        UserPoint userPoint = new UserPoint(userId, 0, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(userPoint);
    }
}

private void verifyUserPointOperations(long[] userIds) {
    for (long userId : userIds) {
        verify(userPointTable, times(1)).selectById(userId);
        verify(userPointTable, times(1)).insertOrUpdate(eq(userId), anyLong());
    }
}
```

## 5. 결론

이 테스트는 여러 스레드가 동시에 동일한 데이터를 수정하려는 상황에서 동시성 제어가 제대로 이루어지는지 확인하는 데 중점을 두었습니다. ReentrantLock을 사용하여 각 사용자별로 락을 관리하고, 여러 스레드가
동시에 실행되더라도 데이터 무결성이 유지되도록 했습니다. 이와 같은 동시성 테스트는 멀티스레드 환경에서 발생할 수 있는 문제를 사전에 방지하고, 시스템의 안정성을 높이는 중요한 역할을 합니다.




.