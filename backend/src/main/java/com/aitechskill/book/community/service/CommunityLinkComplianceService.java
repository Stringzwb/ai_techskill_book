package com.aitechskill.book.community.service;

import com.aitechskill.book.common.exception.BusinessException;
import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** 链接分享的基础合规校验，不由服务端请求用户提供的 URL。 */
@Service
public class CommunityLinkComplianceService {
    public String validate(String value) {
        try {
            URI uri = URI.create(value.trim());
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw invalid();
            }
            if (uri.getUserInfo() != null || uri.getHost() == null || uri.getHost().isBlank()) {
                throw invalid();
            }
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            if (host.equals("localhost") || host.endsWith(".local") || isPrivateLiteralIp(host)) {
                throw invalid();
            }
            return host;
        } catch (IllegalArgumentException exception) {
            throw invalid();
        }
    }

    private boolean isPrivateLiteralIp(String host) {
        boolean ipv4 = host.matches("\\d{1,3}(?:\\.\\d{1,3}){3}");
        boolean ipv6 = host.contains(":") && host.matches("[0-9a-fA-F:.]+");
        if (!ipv4 && !ipv6) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isAnyLocalAddress() || address.isLoopbackAddress()
                    || address.isSiteLocalAddress() || address.isLinkLocalAddress();
        } catch (Exception ignored) {
            return false;
        }
    }

    private BusinessException invalid() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "LINK_COMPLIANCE_FAILED", "链接未通过公开 HTTP(S) 地址合规校验");
    }
}
