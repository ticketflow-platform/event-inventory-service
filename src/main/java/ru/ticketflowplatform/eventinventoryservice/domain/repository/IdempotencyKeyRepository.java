package ru.ticketflowplatform.eventinventoryservice.domain.repository;

import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.IdempotencyKey;

public interface IdempotencyKeyRepository extends JpaRepository<@NonNull IdempotencyKey, @NonNull UUID> {

    Optional<IdempotencyKey> findByOrganizerIdAndKey(String organizerId, String key);
}
