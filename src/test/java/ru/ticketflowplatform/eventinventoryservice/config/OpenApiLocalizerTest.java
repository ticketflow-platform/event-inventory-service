package ru.ticketflowplatform.eventinventoryservice.config;

import helper.TestMessageSources;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Locale;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.MessageSource;
import scenario.cases.java.config.OpenApiLocalizerTestCases.ExpectedOpenApi;

class OpenApiLocalizerTest {

    private final MessageSource messageSource = TestMessageSources.i18n();
    private final OpenApiLocalizer localizer = new OpenApiLocalizer(messageSource);

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.config.OpenApiLocalizerTestCases#localize_whenLocaleIsRequested_thenTranslateOperationTagsAndSchemaPropertiesTestCases")
    @DisplayName("Локализует OpenAPI по указанной локали")
    void localize_whenLocaleIsRequested_thenTranslateOperationTagsAndSchemaPropertiesTest(
        String scenario, Locale locale, OpenAPI openApi, ExpectedOpenApi expected) {
        localizer.localize(openApi, locale);

        Assertions.assertThat(openApi.getInfo().getTitle()).isEqualTo(expected.infoTitle());
        Assertions.assertThat(openApi.getInfo().getDescription())
            .isEqualTo(expected.infoDescription());
        Assertions.assertThat(openApi.getPaths().get("/api/v1/events").getPost().getSummary())
            .isEqualTo(expected.operationSummary());
        Assertions.assertThat(openApi.getPaths().get("/api/v1/events").getPost().getDescription())
            .isEqualTo(expected.operationDescription());
        Assertions.assertThat(openApi.getTags().getFirst().getName()).isEqualTo(expected.tagName());
        Assertions.assertThat(openApi.getTags().getFirst().getDescription())
            .isEqualTo(expected.tagDescription());
        Assertions.assertThat(
            openApi.getPaths().get("/api/v1/events").getPost().getTags().getFirst())
            .isEqualTo(expected.tagName());
        Assertions.assertThat(
            openApi.getPaths()
                .get("/api/v1/events")
                .getPost()
                .getResponses()
                .get("201")
                .getDescription())
            .isEqualTo(expected.responseDescription());
        Assertions.assertThat(
            ((Schema<?>) openApi.getComponents()
                .getSchemas()
                .get("Request")
                .getProperties()
                .get("title"))
                .getDescription())
            .isEqualTo(expected.schemaDescription());
        Assertions.assertThat(
            ((Schema<?>) openApi.getComponents()
                .getSchemas()
                .get("Request")
                .getProperties()
                .get("title"))
                .getExample())
            .isEqualTo(expected.schemaExample());
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.config.OpenApiLocalizerTestCases#localize_whenOptionalSectionsAreMissing_thenStillSetInfoTestCases")
    @DisplayName("Обрабатывает OpenAPI без необязательных разделов по сценариям")
    void localize_whenOptionalSectionsAreMissing_thenStillSetInfoTest(
        String scenario, OpenAPI openApi, String expectedTitle) {
        localizer.localize(openApi, Locale.ENGLISH);

        Assertions.assertThat(openApi.getInfo().getTitle()).isEqualTo(expectedTitle);
    }
}
