package com.example.flight.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    private String secret = "1e561ab13dc3ac0ba177c453aba2e676a895ce13169eed7682e3479762ffb3f7";
    private TokenProperties access = new TokenProperties(900000L); // 15 min default
    private TokenProperties refresh = new TokenProperties(604800000L); // 7 days default
    private CookieProperties cookie = new CookieProperties();

    @Getter
    @Setter
    public static class TokenProperties {
        private long expiration;

        public TokenProperties() {
            this.expiration = 900000L;
        }

        public TokenProperties(long expiration) {
            this.expiration = expiration;
        }
    }

    @Getter
    @Setter
    public static class CookieProperties {
        private String name = "refreshToken";
        private boolean secure = false;
        private String sameSite = "Lax";
        private String path = "/";
    }
}
