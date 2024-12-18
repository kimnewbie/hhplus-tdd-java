package io.hhplus.tdd.point;

import org.springframework.stereotype.Component;

@Component
public class PointValidator {

    // id 조회 및 확인
    void checkUserId(long userId) {
        if (userId == 0) {
            throw new PointValidationException("유저 아이디를 입력해주세요.");
        }
    }

    // 포인트가 0보다 큰지 확인
    void checkReloadPoint(long point) {
        if (point <= 0) {
            throw new PointValidationException("충전할 포인트는 0원 이상이어야 합니다.");
        }
    }

    // 포인트는 100 이상부터 사용 가능
    void checkUsePoint100(long usePoint) {
        long minUsePoint = 100;
        if (usePoint < minUsePoint) {
            throw new PointValidationException("사용 가능한 최소 금액은 100포인트입니다.");
        }
    }

    // 잔고가 부족할 경우, 포인트 사용은 실패
    void checkBalance(long currPoint, long usePoint) {
        if (currPoint < usePoint) {
            throw new PointValidationException("잔고가 부족합니다.");
        }
    }
}