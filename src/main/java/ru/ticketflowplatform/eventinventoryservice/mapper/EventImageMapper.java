package ru.ticketflowplatform.eventinventoryservice.mapper;

import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.CreateEventResponseDto.EventImageResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Event;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.EventImage;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Image;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.ImageType;

@Mapper(componentModel = ComponentModel.SPRING)
public interface EventImageMapper {

    @Mapping(target = "eventImageId.eventId", source = "event.id")
    @Mapping(target = "eventImageId.imageId", source = "image.id")
    @Mapping(target = "event", source = "event")
    @Mapping(target = "image", source = "image")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "createdAt", source = "now")
    EventImage toEntity(Event event, Image image, ImageType type, Instant now);

    @Mapping(target = "imageId", source = "image.id")
    EventImageResponseDto toResponse(EventImage eventImage);

    List<EventImageResponseDto> toResponses(List<EventImage> eventImages);
}
