package com.nag.atithi.security;

import com.nag.atithi.models.User;
import com.nag.atithi.repositories.UserRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;

public class CustomUserDetailsServiceTestNGTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private AutoCloseable mocks;

    @BeforeClass
    public void setUpClass() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterClass
    public void tearDownClass() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @BeforeMethod
    public void setUp() {
        Mockito.reset(userRepository);
    }

    @Test
    public void testLoadUserByUsernameSuccess() {
        User user = new User();
        user.setId(1L);
        user.setEmail("admin@example.com");
        user.setPassword("hashedpassword");

        Mockito.when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("admin@example.com");

        Assert.assertNotNull(userDetails);
        Assert.assertEquals(userDetails.getUsername(), "admin@example.com");
    }

    @Test(expectedExceptions = UsernameNotFoundException.class)
    public void testLoadUserByUsernameNotFound() {
        Mockito.when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        userDetailsService.loadUserByUsername("unknown@example.com");
    }
}
