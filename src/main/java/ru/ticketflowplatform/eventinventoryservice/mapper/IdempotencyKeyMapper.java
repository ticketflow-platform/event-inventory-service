package ru.ticketflowplatform.eventinventoryservice.mapper;

import java.time.Instant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Event;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.IdempotencyKey;

@Mapper(componentModel = ComponentModel.SPRING)
public interface IdempotencyKeyMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organizerId", source = "organizerId")
    @Mapping(target = "key", source = "key")
    @Mapping(target = "requestHash", source = "requestHash")
    @Mapping(target = "event", source = "event")
    @Mapping(target = "createdAt", source = "now")
    IdempotencyKey toEntity(String organizerId, String key, String requestHash, Event event, Instant now);
}
