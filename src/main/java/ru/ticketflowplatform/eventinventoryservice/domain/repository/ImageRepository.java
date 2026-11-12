package ru.ticketflowplatform.eventinventoryservice.domain.repository;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Image;

public interface ImageRepository extends JpaRepository<@NonNull Image, @NonNull UUID> {

    @Lock(LockModeType.PESSIMISTIC_READ)
    List<Image> findAllByIdInAndOrganizerId(Collection<UUID> ids, String organizerId);
}
