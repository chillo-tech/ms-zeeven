package com.cs.ge.services;

import com.cs.ge.repositories.LoginRepository;
import org.springframework.stereotype.Service;

@Service
public class LoginService {
    private final LoginRepository loginRepository;

    public LoginService(final LoginRepository loginRepository) {
        this.loginRepository = loginRepository;
    }
}
