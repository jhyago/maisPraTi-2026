package com.clarim.api.repository;

import com.clarim.api.model.Assinatura;
import com.clarim.api.model.StatusAssinatura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AssinaturaRepository extends JpaRepository<Assinatura, Long> {
    Optional<Assinatura> findByStripeSubscriptionId(String stripeSubscriptionId);
    List<Assinatura> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);

    boolean existsByUsuarioIdAndStatusInAndPeriodoFimAfter(Long usuarioId, Collection<StatusAssinatura> status, OffsetDateTime agora);
}
