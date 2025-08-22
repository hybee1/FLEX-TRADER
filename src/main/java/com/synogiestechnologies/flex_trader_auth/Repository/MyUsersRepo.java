package com.synogiestechnologies.flex_trader_auth.Repository;

import com.synogiestechnologies.flex_trader_auth.Models.MyUsers;
import jakarta.persistence.Tuple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MyUsersRepo extends JpaRepository<MyUsers, Long> {

        Optional<MyUsers> findByUsername(String username);
        Optional<MyUsers> findByUsernameIgnoreCase(String username);
        Optional<MyUsers> findByEmailIgnoreCase(String username);
        boolean existsByUsername(String username);
        boolean existsByEmail(String email);
        boolean existsByUsernameOrEmail(String username, String email);

        @Query
                (" SELECT m FROM MyUsers m WHERE LOWER(m.username) = LOWER(:keyword)" +
                        "OR LOWER(m.email) = LOWER(:keyword)")
        Optional<MyUsers> usernameOrEmail(@Param("keyword") String keyword);

        @Query("SELECT COUNT(m) > 0 FROM MyUsers m WHERE LOWER(m.username) = LOWER(:keyword)" +
                " OR LOWER(m.email) = LOWER(:keyword)")
        boolean userAlreadyExist(@Param("keyword") String keyword);

        // the below query is same as user already exist just above this line. this one just count the
        // number of occurrence for each field (username and password). so that we can return the that
        // already exist or both if both already exist

        @Query("""
                  SELECT
                  COUNT(CASE WHEN LOWER(m.username) = LOWER(:username) THEN 1 END) as usernameMatch,
                  COUNT(CASE WHEN LOWER(m.email) = LOWER(:email) THEN 1 END) as emailMatch
                  FROM MyUsers m
            """)
        Tuple findUsernameAndEmailMatchCounts(@Param("username") String username,
                                              @Param("email") String email);

}


