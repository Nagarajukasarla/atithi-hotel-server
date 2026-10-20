package com.nag.atithi.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.mockito.Mockito;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;

public class CookieServiceTestNGTest {

    private CookieService cookieService;

    @BeforeMethod
    public void setUp() {
        cookieService = new CookieService();
        ReflectionTestUtils.setField(cookieService, "cookieName", "jwt");
        ReflectionTestUtils.setField(cookieService, "expirationSeconds", 86400L);
    }

    @Test
    public void testCreateJwtCookie() {
        ResponseCookie cookie = cookieService.createJwtCookie("dummy-token");

        Assert.assertNotNull(cookie);
        Assert.assertEquals(cookie.getName(), "jwt");
        Assert.assertEquals(cookie.getValue(), "dummy-token");
        Assert.assertTrue(cookie.isHttpOnly());
        Assert.assertEquals(cookie.getPath(), "/");
    }

    @Test
    public void testClearJwtCookie() {
        ResponseCookie cookie = cookieService.clearJwtCookie();

        Assert.assertNotNull(cookie);
        Assert.assertEquals(cookie.getName(), "jwt");
        Assert.assertEquals(cookie.getValue(), "");
        Assert.assertEquals(cookie.getMaxAge().getSeconds(), 0);
    }

    @Test
    public void testExtractTokenFromCookieSuccess() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Cookie[] cookies = new Cookie[]{
                new Cookie("otherCookie", "123"),
                new Cookie("jwt", "valid-jwt-token")
        };
        Mockito.when(request.getCookies()).thenReturn(cookies);

        Optional<String> token = cookieService.extractTokenFromCookie(request);

        Assert.assertTrue(token.isPresent());
        Assert.assertEquals(token.get(), "valid-jwt-token");
    }

    @Test
    public void testExtractTokenFromCookieNullCookies() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getCookies()).thenReturn(null);

        Optional<String> token = cookieService.extractTokenFromCookie(request);

        Assert.assertFalse(token.isPresent());
    }

    @Test
    public void testExtractTokenFromCookieNotFound() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Cookie[] cookies = new Cookie[]{
                new Cookie("otherCookie", "123")
        };
        Mockito.when(request.getCookies()).thenReturn(cookies);

        Optional<String> token = cookieService.extractTokenFromCookie(request);

        Assert.assertFalse(token.isPresent());
    }
}
