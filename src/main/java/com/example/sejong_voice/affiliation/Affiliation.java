package com.example.sejong_voice.affiliation;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "affiliation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Affiliation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false, length = 30)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = true) // 루트 허용이면 nullable=true, 총학은 루트노드라서 parent가 없음
    private Affiliation parent;
}
