package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointService {
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
    private final PointValidator pointValidator;
    private final LockManager lockManager;

    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable, PointValidator pointValidator, LockManager lockManager) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
        this.pointValidator = pointValidator;
        this.lockManager = lockManager;
    }

    // 특정 유저의 포인트를 조회
    public UserPoint getUserPoint(long userId) {
        // validation :: 아이디 확인
        pointValidator.checkUserId(userId);

        return userPointTable.selectById(userId);
    }

    // 특정 유저의 포인트 충전/이용 내역을 조회
    public List<PointHistory> getUserPointHistory(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    // 특정 유저의 포인트를 충전하는 기능
    public UserPoint chargeUserPoint(long userId, long amount) {
        lockManager.lock(userId);
        try {
            // validation :: 충전하려는 포인트가 0보다 커야한다.
            pointValidator.checkReloadPoint(amount);

            // 기존 포인트 조회
            UserPoint userPoint = userPointTable.selectById(userId);
            if (userPoint == null) {
                userPoint = UserPoint.empty(userId);
            }

            // 포인트 업데이트
            UserPoint totalUserPoint = userPointTable.insertOrUpdate(userId, userPoint.point() + amount);

            // 충전 내역 기록
            pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

            return totalUserPoint;
        } finally {
            lockManager.unlock(userId);
        }
    }

    // 특정 유저의 포인트를 사용하는 기능
    public UserPoint useUserPoint(long userId, long amount) {
        lockManager.lock(userId);
        try {
            // 기존 포인트 조회
            UserPoint userPoint = userPointTable.selectById(userId);
            if (userPoint == null) {
                throw new IllegalStateException("정보가 존재하지 않습니다.");
            }

            // 포인트 차감
            long addPoint = userPoint.point() - amount;

            // 포인트가 부족하면 예외 처리
            if (addPoint < 0) {
                throw new IllegalStateException("포인트가 부족합니다. 현재 잔액: " + userPoint.point());
            }

            // 포인트 업데이트
            userPointTable.insertOrUpdate(userId, addPoint);

            // 사용 내역 기록
            pointHistoryTable.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());

            // 갱신된 포인트 반환
            return userPointTable.selectById(userId);
        } finally {
            lockManager.unlock(userId);
        }
    }

    // 히스토리 목록 조회
    public List<PointHistory> getPointHistory(long userId) {
        // validation :: 아이디 확인
        pointValidator.checkUserId(userId);

        return pointHistoryTable.selectAllByUserId(userId);
    }
}
