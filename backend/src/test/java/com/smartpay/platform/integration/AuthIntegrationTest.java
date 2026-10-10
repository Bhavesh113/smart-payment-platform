package com.smartpay.platform.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smartpay.platform.entity.Role;
import com.smartpay.platform.entity.User;
import com.smartpay.platform.entity.UserStatus;
import com.smartpay.platform.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registerLoginAndReadProfileWithoutLeakingPassword() throws Exception {
        String email = uniqueEmail("alice");
        String token = registerAndLogin("Alice", email);

        authGet(token, "/api/users/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(content().string(not(containsString("password"))));
    }

    @Test
    void newUserGetsEmptyWallet() throws Exception {
        String token = registerAndLogin("Wally", uniqueEmail("wally"));

        authGet(token, "/api/wallet")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(0.0))
                .andExpect(jsonPath("$.data.currency").value("INR"));
    }

    @Test
    void duplicateRegistrationIsRejectedWithConflict() throws Exception {
        String email = uniqueEmail("dup");
        register("Dup", email);

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Dup", "email", email, "password", PASSWORD, "phone", "9876543210"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void registrationValidationReturnsFieldErrors() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "X", "email", "not-an-email", "password", "short", "phone", "abc"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.phone").exists());
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        String email = uniqueEmail("bob");
        register("Bob", email);

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "WrongPass1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void garbageTokenReturns401() throws Exception {
        authGet("not.a.jwt", "/api/wallet").andExpect(status().isUnauthorized());
    }

    @Test
    void regularUserCannotReachAdminEndpoints() throws Exception {
        String token = registerAndLogin("Eve", uniqueEmail("eve"));

        authGet(token, "/api/admin/analytics").andExpect(status().isForbidden());
        authGet(token, "/api/admin/users").andExpect(status().isForbidden());
        authGet(token, "/api/admin/transactions").andExpect(status().isForbidden());
    }

    @Test
    void adminCanReachAdminEndpoints() throws Exception {
        String email = uniqueEmail("admin");
        userRepository.save(User.builder().name("Admin").email(email).phone("9876543210")
                .password(passwordEncoder.encode(PASSWORD)).role(Role.ADMIN).status(UserStatus.ACTIVE).build());
        String token = login(email);

        authGet(token, "/api/admin/analytics")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overview.totalTransactions").exists())
                .andExpect(jsonPath("$.data.totalUsers").exists());
        authGet(token, "/api/admin/users").andExpect(status().isOk());
    }

    @Test
    void blockedUserCannotLogIn() throws Exception {
        String email = uniqueEmail("blocked");
        userRepository.save(User.builder().name("Blocked").email(email).phone("9876543210")
                .password(passwordEncoder.encode(PASSWORD)).role(Role.USER).status(UserStatus.BLOCKED).build());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isForbidden());
    }
}
