package io.hhplus.tdd.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


// 비즈니스 로직이 기대대로 동작하는 지 검증
// PointService는 UserPointTable과 PointHistoryTable에 의존적 > mock처리
class PointServiceTest {

    private UserPointTable userPointTable;
    private PointHistoryTable pointHistoryTable;
    private PointService pointService;

    @BeforeEach
    void setUp() {
        userPointTable = mock(UserPointTable.class);
        pointHistoryTable = mock(PointHistoryTable.class);
        pointService = new PointService(userPointTable, pointHistoryTable);
    }

    @Test
    @DisplayName("특정 유저의 포인트를 조회")
    void getUserPoint() {
        // given
        long userId = 1L;
        UserPoint expected = new UserPoint(userId, 100, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(expected);

        // when
        UserPoint result = pointService.getUserPoint(userId);

        // then
        assertThat(result).isEqualTo(expected);
        verify(userPointTable, times(1)).selectById(userId);
    }

    @Test
    @DisplayName("특정 유저의 포인트 충전/이용 내역을 조회")
    void getPointHistory() {
        long userId = 1L;
        List<PointHistory> expected = List.of(new PointHistory(1L, userId, 100L, TransactionType.CHARGE, System.currentTimeMillis()));
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(expected);

        List<PointHistory> result = pointService.getUserPointHistory(userId);

        assertThat(result).isEqualTo(expected);
        verify(pointHistoryTable, times(1)).selectAllByUserId(userId);
    }

    @Test
    @DisplayName("특정 유저의 포인트를 충전하는 기능")
    void chargeUserPoint() {
        // given
        long userId = 1L;
        long chargeAmount = 50L;
        UserPoint userPoint = new UserPoint(userId, 100L, System.currentTimeMillis());
        UserPoint updatedUserPoint = new UserPoint(userId, 150L, System.currentTimeMillis());

        when(userPointTable.selectById(userId)).thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(userId, 150L)).thenReturn(updatedUserPoint);

        // when
        UserPoint result = pointService.chargeUserPoint(userId, chargeAmount);

        // then
        assertEquals(150L, result.point());
        verify(userPointTable, times(1)).selectById(userId);
        verify(userPointTable, times(1)).insertOrUpdate(userId, 150L);
        verify(pointHistoryTable, times(1)).insert(userId, chargeAmount, TransactionType.CHARGE, anyLong());
    }

    @Test
    @DisplayName("특정 유저의 포인트를 사용하는 기능")
    void useUserPoint() {
        // given
        long userId = 1L;
        long useAmount = 50L;
        UserPoint userPoint = new UserPoint(userId, 100L, System.currentTimeMillis());
        UserPoint updatedUserPoint = new UserPoint(userId, 50L, System.currentTimeMillis());

        when(userPointTable.selectById(userId)).thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(userId, 50L)).thenReturn(updatedUserPoint);

        // when
        UserPoint result = pointService.useUserPoint(userId, useAmount);

        // then
        assertEquals(50L, result.point());
        verify(userPointTable, times(1)).selectById(userId);
        verify(userPointTable, times(1)).insertOrUpdate(userId, 50L);
        verify(pointHistoryTable, times(1)).insert(userId, useAmount, TransactionType.USE, anyLong());
    }

    @Test
    void useUserOutOfPoint() {
        // given
        long userId = 1L;
        long amount = 150L;
        UserPoint mockUserPoint = new UserPoint(userId, 100, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(mockUserPoint);

        // when & then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            pointService.useUserPoint(userId, amount);
        });

        assertEquals("포인트가 부족합니다. 현재 잔액: 100", exception.getMessage());
    }
}