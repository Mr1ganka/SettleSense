package com.kelvin.settlesense.api;

import com.kelvin.settlesense.domain.service.AuthWorkflowService;
import com.kelvin.settlesense.domain.model.dto.LoginUserDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthWorkflowService authService;

    public AuthController(AuthWorkflowService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public String login(@Valid @RequestBody LoginUserDto loginRequest) {
        String email = loginRequest.email();
        String password = loginRequest.password();

        return authService.generateToken(email, password);
    }


}
