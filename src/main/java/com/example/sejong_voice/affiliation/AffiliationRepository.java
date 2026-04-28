package com.example.sejong_voice.affiliation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AffiliationRepository extends JpaRepository<Affiliation, UUID> {

    Optional<Affiliation> findByName(String name);
}
