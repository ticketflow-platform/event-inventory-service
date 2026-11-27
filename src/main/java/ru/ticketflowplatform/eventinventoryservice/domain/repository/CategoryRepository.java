package ru.ticketflowplatform.eventinventoryservice.domain.repository;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Category;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.CategoryStatus;

public interface CategoryRepository extends JpaRepository<@NonNull Category, @NonNull UUID> {

    @Lock(LockModeType.PESSIMISTIC_READ)
    List<Category> findAllByIdInAndStatus(Collection<UUID> ids, CategoryStatus status);
}
