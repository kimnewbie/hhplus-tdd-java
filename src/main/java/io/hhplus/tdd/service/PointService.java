package io.hhplus.tdd.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointService {
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    // 특정 유저의 포인트를 조회
    public UserPoint getUserPoint(long userId) {
        return userPointTable.selectById(userId);
    }

    // 특정 유저의 포인트 충전/이용 내역을 조회
    public List<PointHistory> getUserPointHistory(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    // 특정 유저의 포인트를 충전하는 기능
    public UserPoint chargeUserPoint(long userId, long amount) {
        // 기존 포인트 조회
        UserPoint userPoint = userPointTable.selectById(userId);
        // 포인트 충전
        long addPoint = userPoint.point() + amount;
        // 포인트 업데이트
        userPointTable.insertOrUpdate(userId, addPoint);
        // 충전 내역 기록
        pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

        // 갱신된 포인트 반환
        return userPointTable.selectById(userId);
    }

    // 특정 유저의 포인트를 사용하는 기능
    public UserPoint useUserPoint(long userId, long amount) {
        // 기존 포인트 조회
        UserPoint userPoint = userPointTable.selectById(userId);
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
    }
}
