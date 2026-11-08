package ru.ticketflowplatform.eventinventoryservice.domain.repository;

import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.EventCategory;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.EventCategory.EventCategoryId;

public interface EventCategoryRepository extends JpaRepository<@NonNull EventCategory, @NonNull EventCategoryId> {
}
