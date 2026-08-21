package com.aitechskill.book.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aitechskill.book.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class CommunityLinkComplianceServiceTest {

    private final CommunityLinkComplianceService service = new CommunityLinkComplianceService();

    @Test
    void acceptsPublicHttpsCloudDriveLinkWithoutFetchingIt() {
        assertThat(service.validate("https://pan.example.com/s/engineering-notes"))
                .isEqualTo("pan.example.com");
    }

    @Test
    void rejectsPrivateAndNonHttpTargets() {
        assertThatThrownBy(() -> service.validate("http://127.0.0.1/private"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.validate("ftp://pan.example.com/archive"))
                .isInstanceOf(BusinessException.class);
    }
}
