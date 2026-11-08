package ru.ticketflowplatform.eventinventoryservice.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Category;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.CategoryStatus;

public interface CategoryRepository extends JpaRepository<@NonNull Category, @NonNull UUID> {

    List<Category> findAllByIdInAndStatus(Collection<UUID> ids, CategoryStatus status);
}
