package com.synogiestechnologies.flex_trader_auth.UserDetails;


import com.synogiestechnologies.flex_trader_auth.Models.MyUsers;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class MyUsersDetails implements UserDetails {

    private final MyUsers myUsers;

    public MyUsersDetails(MyUsers myUsers) {
        this.myUsers = myUsers;
    }

    public MyUsers getMyUser() {
        return myUsers;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
//        return List.of();
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_"+myUsers.getRole().name()));
    }

    @Override
    public String getPassword() {
        return myUsers.getPassword();
    }

    @Override
    public String getUsername() {
        return myUsers.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return myUsers.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return myUsers.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return myUsers.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return myUsers.isEnabled();
    }
}
