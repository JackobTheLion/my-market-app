package ru.practicum.yakovlev.mymarketapp.security;

import lombok.Getter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

@Getter
public class CustomerPrincipal extends User {
    private final long userId;

    public CustomerPrincipal(long userId, String username, String passwordHash, boolean enabled) {
        super(username, passwordHash, enabled, true, true, true, AuthorityUtils.createAuthorityList("ROLE_USER"));
        this.userId = userId;
    }
}
