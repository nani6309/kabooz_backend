package com.kabooz.backend.security;

import com.kabooz.backend.entity.AdminUser;
import com.kabooz.backend.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loads admin user details from the database for Spring Security authentication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AdminUserRepository adminUserRepository;

    /**
     * Load an admin user by username for Spring Security to authenticate against.
     *
     * @param username the login username
     * @return UserDetails with BCrypt password hash and ROLE_ADMIN authority
     * @throws UsernameNotFoundException if no admin with that username exists
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser adminUser = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Admin user not found: {}", username);
                    return new UsernameNotFoundException("Admin not found: " + username);
                });

        return new User(
                adminUser.getUsername(),
                adminUser.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }
}
