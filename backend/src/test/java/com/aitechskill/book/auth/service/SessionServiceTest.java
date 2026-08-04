package com.aitechskill.book.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.aitechskill.book.auth.domain.SessionRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * Redis 用户会话服务测试。
 */
@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        sessionService = new SessionService(redisTemplate, objectMapper, Duration.ofDays(7));
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @Test
    void storesOnlyTokenHashAsRedisKey() throws Exception {
        String token = sessionService.createSession(25L);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), org.mockito.ArgumentMatchers.eq(Duration.ofDays(7)));

        assertThat(token).hasSizeGreaterThanOrEqualTo(40);
        assertThat(keyCaptor.getValue()).startsWith("auth:session:").doesNotContain(token);
        SessionRecord record = objectMapper.readValue(valueCaptor.getValue(), SessionRecord.class);
        assertThat(record.userId()).isEqualTo(25L);
    }
}
