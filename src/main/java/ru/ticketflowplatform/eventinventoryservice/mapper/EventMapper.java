package ru.ticketflowplatform.eventinventoryservice.mapper;

import java.time.Instant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;
import org.mapstruct.InjectionStrategy;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.CreateEventResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Event;

@Mapper(
        componentModel = ComponentModel.SPRING,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {EventCategoryMapper.class, EventImageMapper.class}
)
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organizerId", source = "organizerId")
    @Mapping(target = "title", source = "request.title")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "eventCategories", ignore = true)
    @Mapping(target = "eventImages", ignore = true)
    @Mapping(target = "ageRestriction", source = "ageRestriction")
    @Mapping(target = "status", expression = "java(EventStatus.DRAFT)")
    @Mapping(target = "createdAt", source = "now")
    @Mapping(target = "updatedAt", source = "now")
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    Event toEntity(CreateEventRequestDto request, String organizerId, Integer ageRestriction, Instant now);

    @Mapping(target = "categoryIds", source = "eventCategories")
    @Mapping(target = "images", source = "eventImages")
    CreateEventResponseDto toResponse(Event event);
}
