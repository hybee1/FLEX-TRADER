package com.synogiestechnologies.flex_trader_auth.Repository;

import com.synogiestechnologies.flex_trader_auth.Models.JwtToken;
import com.synogiestechnologies.flex_trader_auth.Models.MyUsers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface JwtTokenRepo extends JpaRepository<JwtToken, Long> {
        Optional<JwtToken> findByTokenExpiryDate(Instant expiryDate);

        Optional<JwtToken> findByIsRevoked(boolean isRevoked);

        Optional<JwtToken> findByToken(String token);

        @Query("SELECT j FROM JwtToken j LEFT JOIN FETCH j.user u " +
                "WHERE LOWER(u.username) =  LOWER(:name)")
        List<JwtToken> findJwtTokensWithUsername(@Param("name") String name);

        @Query("SELECT j FROM JwtToken j JOIN FETCH j.user u " +
                "WHERE  LOWER(u.username) =  LOWER(:name) AND j.tokenExpiryDate >= :now")
        Optional<JwtToken> findNonExpiredJwtTokenWithUsername(@Param("name") String name,
                                                            @Param("now") Instant now);

        @Query("SELECT j FROM JwtToken j JOIN FETCH j.user u " +
                "WHERE LOWER(u.username) =  LOWER(:name) AND j.tokenExpiryDate < :now")
        List<JwtToken> findExpiredJwtTokensWithUsername(@Param("name") String name,
                                                                      @Param("now") Instant now);

//        @Query("SELECT COUNT(j) > 0 FROM JwtToken j WHERE j.token = :token AND j.isRevoked = true")
//        boolean isJwtTokenRevoked(@Param("token") String token);

//        @Query("SELECT j.isRevoked FROM JwtToken j WHERE j.token = :token")
        @Query(value = "SELECT j.is_revoked FROM users_tokens_tbl j WHERE j.token = :token",
                nativeQuery = true)
        Optional<Boolean> getRevocationStatus(@Param("token") String token);

        boolean existsByUserAndIsRevokedFalseAndTokenExpiryDateAfter(MyUsers user, Instant now);

        Optional<JwtToken> findByUserAndIsRevokedFalseAndTokenExpiryDateAfter(MyUsers user, Instant now);

        Optional<JwtToken> findByUserAndIsRevokedFalse(MyUsers user);
}


