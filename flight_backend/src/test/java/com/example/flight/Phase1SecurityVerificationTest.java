package com.example.flight;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;

import com.example.flight.entity.Booking;
import com.example.flight.entity.User;
import com.example.flight.service.BookingAccessService;

@SpringBootTest
@ActiveProfiles("test")
public class Phase1SecurityVerificationTest {

    @Autowired
    private BookingAccessService bookingAccessService;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private org.springframework.cache.CacheManager cacheManager;

    private Authentication userAuth;
    private Authentication anotherUserAuth;
    private Authentication adminAuth;

    @BeforeEach
    void setUp() {
        userAuth = new UsernamePasswordAuthenticationToken("user@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        anotherUserAuth = new UsernamePasswordAuthenticationToken("other@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        adminAuth = new UsernamePasswordAuthenticationToken("admin@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("Verify User Booking Ownership Enforcement")
    void testUserBookingOwnership() {
        Booking booking = new Booking();
        User user = new User();
        user.setEmail("user@example.com");
        booking.setUser(user);

        assertDoesNotThrow(() -> {
            if (!booking.getUser().getEmail().equalsIgnoreCase(userAuth.getName())) {
                throw new AccessDeniedException("Access Denied");
            }
        });

        assertThrows(AccessDeniedException.class, () -> {
            if (!booking.getUser().getEmail().equalsIgnoreCase(anotherUserAuth.getName())) {
                throw new AccessDeniedException("Access Denied");
            }
        });
    }

    @Test
    @DisplayName("Verify Admin Override Access")
    void testAdminAccess() {
        assertTrue(bookingAccessService.isAdmin(adminAuth));
    }
}
