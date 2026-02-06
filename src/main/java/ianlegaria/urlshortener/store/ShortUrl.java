package ianlegaria.urlshortener.store;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "short_url",
        indexes = {
                @Index(name = "idx_short_url_long_url", columnList = "longUrl")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_short_url_code", columnNames = "code")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    private String code;

    @Column(nullable = false, length = 2048)
    private String longUrl;
}
