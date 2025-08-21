package com.synogiestechnologies.flex_trader_auth.Models;


import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;


@ToString(exclude = {"user"})
@EqualsAndHashCode(exclude = {"user"})
@Entity
@Table(name = "users_tokens_tbl", uniqueConstraints = {@UniqueConstraint(columnNames =
        {"token", "isRevoked", "tokenExpiryDate", "user_id",})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class JwtToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // or use @Lob for columnDefinition = "TEXT"
    @Column(nullable = false, unique = true, updatable = false, columnDefinition = "TEXT")
    private String token;

    @Column(nullable = false)
    private boolean isRevoked;

    @Column(nullable = false, updatable = false)
    private Instant tokenExpiryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    @JoinColumn(name = "user_id", nullable = false)
    private MyUsers user;

    @CreatedDate
    @Column(nullable = false, updatable = false)  // can only be inserted
    private Instant createdDate;

}
