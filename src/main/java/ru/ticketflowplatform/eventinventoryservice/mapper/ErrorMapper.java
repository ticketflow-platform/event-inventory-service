package ru.ticketflowplatform.eventinventoryservice.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;
import org.springframework.validation.FieldError;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.ErrorResponseDto.FieldErrorResponseDto;

@Mapper(componentModel = ComponentModel.SPRING)
public interface ErrorMapper {

    @Mapping(target = "status", source = "status")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "detail", source = "detail")
    @Mapping(target = "errors", source = "errors")
    ErrorResponseDto toResponse(int status, String message, String detail, List<FieldErrorResponseDto> errors);

    @Mapping(target = "field", source = "field")
    @Mapping(target = "message", source = "defaultMessage")
    FieldErrorResponseDto toFieldError(FieldError error);
}
