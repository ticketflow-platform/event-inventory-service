package ru.ticketflowplatform.eventinventoryservice.config;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import lombok.RequiredArgsConstructor;
import ru.ticketflowplatform.eventinventoryservice.domain.model.consts.I18N;

@Component
@RequiredArgsConstructor
public class OpenApiLocalizer {

    private final MessageSource messageSource;

    public void localize(OpenAPI openApi, Locale locale) {
        openApi.setInfo(
            new Info().title(toMessage("swagger.info.title", locale))
                .description(toMessage("swagger.info.description", locale))
                .version("1.0.0")
        );

        if (openApi.getTags() != null) {
            openApi.getTags().forEach(tag -> {
                tag.setName(resolve(tag.getName(), locale));
                tag.setDescription(resolve(tag.getDescription(), locale));
            });
        }

        if (openApi.getPaths() != null) {
            openApi.getPaths().values().stream().flatMap(path -> path.readOperations().stream())
                .forEach(operation -> localize(operation, locale));
        }

        if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
            openApi.getComponents().getSchemas().values().forEach(schema -> localize(schema, locale));
        }
    }

    private void localize(Operation operation, Locale locale) {
        operation.setSummary(resolve(operation.getSummary(), locale));
        operation.setDescription(resolve(operation.getDescription(), locale));

        if (operation.getTags() != null) {
            operation.setTags(operation.getTags().stream().map(tag -> resolve(tag, locale)).toList());
        }

        if (operation.getParameters() != null) {
            operation.getParameters().forEach(parameter -> parameter.setDescription(resolve(parameter.getDescription(), locale)));
        }

        if (operation.getRequestBody() != null) {
            operation.getRequestBody().setDescription(resolve(operation.getRequestBody().getDescription(), locale));
        }

        if (operation.getResponses() != null) {
            operation.getResponses().values().forEach(response -> response.setDescription(resolve(response.getDescription(), locale)));
        }
    }

    private void localize(Schema<?> schema, Locale locale) {
        schema.setTitle(resolve(schema.getTitle(), locale));
        schema.setDescription(resolve(schema.getDescription(), locale));

        if (schema.getExample() instanceof String example) {
            schema.setExample(resolve(example, locale));
        }

        if (schema.getProperties() != null) {
            schema.getProperties().values().forEach(property -> localize((Schema<?>) property, locale));
        }

        if (schema.getItems() != null) {
            localize(schema.getItems(), locale);
        }
    }

    private String resolve(String value, Locale locale) {
        if (value == null || !value.startsWith(I18N.MESSAGE_PREFIX)) {
            return value;
        }

        return messageSource.getMessage(value.substring(I18N.MESSAGE_PREFIX.length()), null, locale);
    }

    private String toMessage(String code, Locale locale) {
        return messageSource.getMessage(code, null, locale);
    }
}
