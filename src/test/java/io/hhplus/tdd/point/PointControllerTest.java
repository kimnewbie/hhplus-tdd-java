package io.hhplus.tdd.point;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


// Controller의 메서드가 제대로 동작하는지 검증
@SpringBootTest
@AutoConfigureMockMvc
class PointControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PointService pointService;

    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("특정 유저의 포인트를 조회")
    void getUserPoint() throws Exception {
        // given
        long userId = 1L;
        UserPoint userPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        given(pointService.getUserPoint(userId)).willReturn(userPoint);

        // when - then
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk());
    }


    @Test
    @DisplayName("특정 유저의 포인트 충전/이용 내역을 조회")
    void getUserPointHistory() {
        // given
        long userId = 1L;
        List<PointHistory> historyList = Collections.emptyList();
        given(pointService.getUserPointHistory(userId)).willReturn(historyList);

        // when-then
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("특정 유저의 포인트를 충전하는 기능")
    void chargeUserPoint() {
        // given
        long userId = 1L;
        long amount = 1000L;
        UserPoint updated = new UserPoint(userId, amount, System.currentTimeMillis());
        given(pointService.chargeUserPoint(userId, amount)).willReturn(updated);

        // when-then
        mockMvc.perform(MockMvcRequestBuilders.patch("/point/{id}/use", userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Collections.singletonMap("amount", amount)));
    }
}