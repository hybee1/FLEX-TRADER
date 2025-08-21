package com.synogiestechnologies.flex_trader_auth.Repository;

import com.synogiestechnologies.flex_trader_auth.Models.MyUsers;
import com.synogiestechnologies.flex_trader_auth.Models.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepo extends JpaRepository<Subscription, Long> {
        Optional<Subscription> findBySubExpiryDate(Instant expiryDate);

        Optional<Subscription> findByIsCancelled(boolean isCancelled);

        Optional<Subscription> findBySubscriptionPlanType(String subPlanType);

        @Query("SELECT s FROM Subscription s LEFT JOIN FETCH s.user u " +
                "WHERE LOWER(u.username) =  LOWER(:name)")
        List<Subscription> findSubscriptionsWithUsername(@Param("name") String name);

        @Query("SELECT s FROM Subscription s JOIN FETCH s.user u " +
                "WHERE  LOWER(u.username) =  LOWER(:name) AND s.subExpiryDate >= :now")
        Optional<Subscription> findNonExpiredSubscriptionWithUsername(@Param("name") String name,
                                                            @Param("now") Instant now);

        @Query("SELECT s FROM Subscription s JOIN FETCH s.user u " +
                "WHERE LOWER(u.username) =  LOWER(:name) AND s.subExpiryDate < :now")
        List<Subscription> findExpiredSubscriptionsWithUsername(@Param("name") String name,
                                                                      @Param("now") Instant now);

        boolean existsByUserAndIsCancelledFalseAndSubExpiryDateAfter(MyUsers user,
                                                                 Instant expiryDate);

        Optional<Subscription> findByUserAndIsCancelledFalseAndSubExpiryDateAfter(MyUsers user,
                                                                              Instant expiryDate);

        @Query("SELECT EXISTS ( SELECT 1 FROM Subscription s JOIN s.user u WHERE " +
                "u.username = :username AND s.hasUseFreePlan = true ")
        boolean userHasUsedFreePlan(@Param("username") String username );

        boolean findByUser_UsernameAndHasUseFreePlanTRue(String username);


}


