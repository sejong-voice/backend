package com.example.sejong_voice.affiliation;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AffiliationRepository extends JpaRepository<Affiliation, UUID> {

  Optional<Affiliation> findByName(String name);
}
