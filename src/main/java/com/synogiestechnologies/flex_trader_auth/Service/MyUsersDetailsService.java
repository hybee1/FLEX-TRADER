package com.synogiestechnologies.flex_trader_auth.Service;


import com.synogiestechnologies.flex_trader_auth.Exceptions.NoUserFoundException;
import com.synogiestechnologies.flex_trader_auth.UserDetails.MyUsersDetails;
import com.synogiestechnologies.flex_trader_auth.Models.MyUsers;
import com.synogiestechnologies.flex_trader_auth.Repository.MyUsersRepo;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
public class MyUsersDetailsService implements UserDetailsService {

    private final MyUsersRepo myUsersRepo;

    public MyUsersDetailsService(MyUsersRepo myUsersRepo) {
        this.myUsersRepo = myUsersRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        MyUsers user = myUsersRepo.usernameOrEmail(username)
                .orElseThrow(() -> new NoUserFoundException("User not found", HttpStatus.NOT_FOUND));

        return new MyUsersDetails(user);
    }

}


