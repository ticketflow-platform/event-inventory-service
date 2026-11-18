package ru.ticketflowplatform.eventinventoryservice.config;

import static ru.ticketflowplatform.eventinventoryservice.domain.model.consts.I18N.ENGLISH_LANGUAGE_TAG;
import static ru.ticketflowplatform.eventinventoryservice.domain.model.consts.I18N.RUSSIAN_LANGUAGE_TAG;

import java.util.Locale;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiLocaleConfig {

    private final String openApiPath;

    OpenApiLocaleConfig(@Value("${api.urls.openapi}") String openApiPath) {
        this.openApiPath = openApiPath;
    }

    @Bean
    OpenApiCustomizer defaultRussianOpenApiCustomizer(OpenApiLocalizer localizer) {
        return openApi -> localizer.localize(openApi, Locale.forLanguageTag(RUSSIAN_LANGUAGE_TAG));
    }

    @Bean
    GroupedOpenApi russianOpenApi(OpenApiLocalizer localizer) {
        return localizedGroup(RUSSIAN_LANGUAGE_TAG, Locale.forLanguageTag(RUSSIAN_LANGUAGE_TAG), localizer);
    }

    @Bean
    GroupedOpenApi englishOpenApi(OpenApiLocalizer localizer) {
        return localizedGroup(ENGLISH_LANGUAGE_TAG, Locale.ENGLISH, localizer);
    }

    private GroupedOpenApi localizedGroup(String name, Locale locale, OpenApiLocalizer localizer) {
        return GroupedOpenApi.builder().group(name).pathsToMatch(openApiPath)
                .addOpenApiCustomizer(openApi -> localizer.localize(openApi, locale)).build();
    }
}
