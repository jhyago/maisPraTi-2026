package com.clarim.api.repository;

import com.clarim.api.model.Plano;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanoRepository extends JpaRepository<Plano, Long> {
    List<Plano> findByAtivoTrue();
    Optional<Plano> findByStripePriceId(String stripePriceId);
}
