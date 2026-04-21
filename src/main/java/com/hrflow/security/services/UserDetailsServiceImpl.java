package com.hrflow.security.services;

import com.hrflow.roles.entities.Role;
import com.hrflow.users.entities.User;
import com.hrflow.users.repositories.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User appUser = userRepository.findWithRolesByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        for (Role role : appUser.getRoles()) {
            authorities.add(new SimpleGrantedAuthority(role.getRoleName()));
        }

        return new org.springframework.security.core.userdetails.User(
                appUser.getEmail(),
                appUser.getPassword(),
                appUser.isEnabled(),
                true, true, true,
                authorities
        );
    }
}
