package ru.ticketflowplatform.eventinventoryservice.mapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Category;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Event;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.EventCategory;

@Mapper(componentModel = ComponentModel.SPRING)
public interface EventCategoryMapper {

    @Mapping(target = "eventCategoryId.eventId", source = "event.id")
    @Mapping(target = "eventCategoryId.categoryId", source = "category.id")
    @Mapping(target = "event", source = "event")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "createdAt", source = "now")
    EventCategory toEntity(Event event, Category category, Instant now);

    default UUID toCategoryId(EventCategory eventCategory) {
        return eventCategory.getCategory().getId();
    }

    List<UUID> toCategoryIds(List<EventCategory> eventCategories);
}
