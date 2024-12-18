package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


// 비즈니스 로직이 기대대로 동작하는 지 검증
// PointService는 UserPointTable과 PointHistoryTable에 의존적 > mock처리
@SpringBootTest
class PointServiceTest {

    @Autowired
    private PointService pointService;

    @MockBean
    private UserPointTable userPointTable;

    @MockBean
    private PointHistoryTable pointHistoryTable;

    @InjectMocks
    private PointValidator pointValidator;


    @BeforeEach
    void setUp() {
        pointService = new PointService(userPointTable, pointHistoryTable, pointValidator);
    }

    @Test
    @DisplayName("유저ID가 없으면 포인트 조회 실패")
    void checkUserId() {
        // when - then
        assertThrows(PointValidationException.class, () -> {
            pointValidator.checkUserId(0);
        });
    }

    @Test
    @DisplayName("특정 유저의 포인트 조회")
    void getUserPoint() {
        // given
        long userId = 1L;
        UserPoint expected = new UserPoint(userId, 100L, System.currentTimeMillis());

        // when
        when(userPointTable.selectById(userId)).thenReturn(expected);
        UserPoint result = pointService.getUserPoint(userId);

        // then
        assertNotNull(result);
        assertThat(result).isEqualTo(expected);
        verify(userPointTable, times(1)).selectById(userId);
    }

    @Test
    @DisplayName("충전하려는 포인트가 0보다 크지 않으면 실패")
    void checkReloadPoint() {
        // given
        long reloadPoint = 0L;

        // when-then
        assertThrows(PointValidationException.class, () -> {
            pointValidator.checkReloadPoint(reloadPoint);
        });
    }


    @Test
    @DisplayName("충전하려는 최소 금액은 100포인트 이상이다.")
    void checkUsePoint100() {
        // given
        long reloadPoint = 50L;

        // when-then
        assertThrows(PointValidationException.class, () -> {
            pointValidator.checkUsePoint100(reloadPoint);
        });
    }

    @Test
    @DisplayName("조건에 맞는 특정 유저의 포인트를 충전하는 기능")
    void reloadUserPoint() {
        // given
        long userId = 2L;
        long reloadPoint = 3000L;

        UserPoint updatedUserPoint = new UserPoint(userId, reloadPoint, System.currentTimeMillis());

        // when
        when(userPointTable.insertOrUpdate(userId, reloadPoint)).thenReturn(updatedUserPoint);
        UserPoint result = pointService.chargeUserPoint(userId, reloadPoint);

        // then
        assertEquals(result.point(), reloadPoint);
    }

    @Test
    @DisplayName("포인트 변화는 유저 history 목록에 기록된다.")
    void addPointHistory() {
        // given
        long userId = 3L;
        long reloadPoint = 3000L;
        UserPoint currBalance = new UserPoint(userId, reloadPoint, System.currentTimeMillis());

        // 포인트 충전 후 반환될 값 설정
        when(userPointTable.insertOrUpdate(userId, reloadPoint)).thenReturn(currBalance);

        // 인트 충전 내역이 history에 기록되도록 설정
        PointHistory pointHistory = new PointHistory(1L, userId, reloadPoint, TransactionType.CHARGE, System.currentTimeMillis());
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(List.of(pointHistory));

        // when
        pointService.chargeUserPoint(userId, reloadPoint);

        // then
        List<PointHistory> pointHistoryList = pointService.getPointHistory(userId);

        assertNotNull(pointHistoryList, "포인트 히스토리 리스트는 null이면 안 됩니다.");
        assertFalse(pointHistoryList.isEmpty(), "히스토리가 비어있으면 안 됩니다.");
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
    @DisplayName("잔고가 부족할 경우, 포인트 사용은 실패")
    void pointCompareBalance() {
        // given
        long userId = 1L;
        long currPoint = 1000L;
        long usePoint = 2000L;

        UserPoint currBalance = new UserPoint(userId, currPoint, System.currentTimeMillis());

        // when
        when(userPointTable.selectById(userId)).thenReturn(currBalance);

        assertThrows(PointValidationException.class, () -> {
            pointValidator.checkBalance(currBalance.point(), usePoint);
        });
    }

    @Test
    @DisplayName("특정 유저의 포인트를 사용하는 기능")
    void useUserPoint() {
        // given
        long userId = 1L;
        long useAmount = 50L;
        UserPoint userPoint = new UserPoint(userId, 100L, System.currentTimeMillis());
        UserPoint updatedUserPoint = new UserPoint(userId, 50L, System.currentTimeMillis());

        // when
        when(userPointTable.selectById(userId)).thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(userId, userPoint.point() - useAmount)).thenReturn(updatedUserPoint);
        when(userPointTable.selectById(userId)).thenReturn(updatedUserPoint);

        UserPoint result = pointService.useUserPoint(userId, useAmount);

        // then
        assertNotNull(result);
        assertEquals(50L, result.point());
    }

    @Test
    @DisplayName("사용자의 포인트 이용 내역 조회")
    void getUserPointHistory() {
        // given
        long userId = 1L;
        List<PointHistory> pointHistoryList = List.of(
                new PointHistory(1L, userId, 10000L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, userId, 3000L, TransactionType.USE, System.currentTimeMillis())
        );

        // when
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(pointHistoryList);
        List<PointHistory> result = pointService.getUserPointHistory(userId);

        // then
        assertNotNull(result);
        assertEquals(result.size(), 2);
    }
}