package ru.ticketflowplatform.eventinventoryservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

class OpenApiLocalizerTest {

    private final ResourceBundleMessageSource messageSource = messageSource();
    private final OpenApiLocalizer localizer = new OpenApiLocalizer(messageSource);

    @Test
    @DisplayName("Локализует OpenAPI по указанной локали")
    void localize_shouldTranslateOperationTagsAndSchemaProperties() {
        StringSchema title = new StringSchema();
        title.setDescription("i18n:swagger.event.create.title");
        title.setExample("i18n:swagger.event.create.title.example");
        OpenAPI openApi = new OpenAPI()
                .tags(List.of(new Tag().name("i18n:swagger.events.tag.name").description("i18n:swagger.events.tag.description")))
                .paths(
                        new Paths().addPathItem(
                                "/api/v1/events",
                                new PathItem().post(
                                        new Operation().summary("i18n:swagger.events.create.summary")
                                                .description("i18n:swagger.events.create.description")
                                                .tags(List.of("i18n:swagger.events.tag.name"))
                                                .responses(
                                                        new ApiResponses().addApiResponse(
                                                                "201",
                                                                new ApiResponse()
                                                                        .description("i18n:swagger.events.create.response.201"))))))
                .components(new Components().schemas(java.util.Map.of("Request", new ObjectSchema().addProperty("title", title))));

        localizer.localize(openApi, Locale.ENGLISH);

        Assertions.assertEquals("TicketFlow Events API", openApi.getInfo().getTitle());
        Assertions.assertEquals("Create an event", openApi.getPaths().get("/api/v1/events").getPost().getSummary());
        Assertions.assertEquals("Events", openApi.getTags().getFirst().getName());
        Assertions.assertEquals(
                "Event title. The only field required to create a draft.",
                ((Schema<?>) openApi.getComponents().getSchemas().get("Request").getProperties().get("title")).getDescription());
        Assertions.assertEquals(
                "Festival",
                ((Schema<?>) openApi.getComponents().getSchemas().get("Request").getProperties().get("title")).getExample());
    }

    private static ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        return source;
    }
}
