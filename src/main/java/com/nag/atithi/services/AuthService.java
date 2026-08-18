package com.nag.atithi.services;

import com.nag.atithi.requests.LoginRequest;
import com.nag.atithi.requests.SignupRequest;
import com.nag.atithi.responses.AuthResponse;

public interface AuthService {
    AuthResponse signup(SignupRequest request);
    AuthResponse login(LoginRequest request);
}
