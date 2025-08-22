package com.synogiestechnologies.flex_trader_auth.Models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.synogiestechnologies.flex_trader_auth.AllEnums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;
import java.util.List;


@ToString(exclude = {"tokens", "subscriptions"})
@EqualsAndHashCode(exclude = {"tokens", "subscriptions"})
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users_tbl", indexes = { @Index(name = "idx_username", columnList = "username") })
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class MyUsers {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    @Email
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;  // e.g., ROLE_USER, ROLE_ADMIN

    @CreatedDate
    @Column(nullable = false, updatable = false)  // can only be inserted
    private Instant createdDate;

    @LastModifiedDate
    @Column(insertable = false) // can only be updated
    private Instant lastLogin;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    // @JoinColumn(name = "order_id", referencedColumnName = "id", nullable = false)
    @JsonManagedReference
    private List<JwtToken> tokens;

//    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
//    // @JoinColumn(name = "order_id", referencedColumnName = "id", nullable = false)
//    @JsonManagedReference
//    private List<JwtWebsocketToken> websocketTokens;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    // @JoinColumn(name = "order_id", referencedColumnName = "id", nullable = false)
    @JsonManagedReference
    private List<Subscription> subscriptions;

    @Builder.Default
    @Column(nullable = false)
    private boolean isAccountNonExpired = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean isAccountNonLocked = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean isCredentialsNonExpired = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean isEnabled = true;


}
