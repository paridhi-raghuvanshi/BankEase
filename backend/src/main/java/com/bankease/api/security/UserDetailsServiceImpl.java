package com.bankease.api.security;

import com.bankease.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link UserDetailsService} for Spring Security authentication.
 *
 * <p>Loads a {@link com.bankease.api.model.User} entity from the database by email address
 * and maps it to a Spring Security {@link UserDetails} object, including role-based
 * {@link SimpleGrantedAuthority} entries.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user by their email address (used as the username in this application).
     *
     * <p>This method is called by Spring Security during authentication. It fetches the user
     * entity from the database, maps all assigned roles to {@link SimpleGrantedAuthority}
     * objects with the {@code ROLE_} prefix, and builds a Spring Security {@link UserDetails}
     * instance.</p>
     *
     * @param email the email address identifying the user
     * @return a fully-populated {@link UserDetails} object
     * @throws UsernameNotFoundException if no user with the given email exists
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Attempting to load user by email: {}", email);

        com.bankease.api.model.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new UsernameNotFoundException(
                            "User not found with email: " + email);
                });

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toList());

        log.debug("Successfully loaded user '{}' with {} role(s)", email, authorities.size());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(!user.isAccountNonExpired())
                .accountLocked(!user.isAccountNonLocked())
                .credentialsExpired(!user.isCredentialsNonExpired())
                .disabled(!user.isEnabled())
                .build();
    }
}
