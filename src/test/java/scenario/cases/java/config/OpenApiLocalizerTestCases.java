package scenario.cases.java.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public final class OpenApiLocalizerTestCases {

    public record ExpectedOpenApi(
        String infoTitle,
        String infoDescription,
        String tagName,
        String tagDescription,
        String operationSummary,
        String operationDescription,
        String responseDescription,
        String schemaDescription,
        String schemaExample) {
    }

    public static Stream<Arguments> localize_whenLocaleIsRequested_thenTranslateOperationTagsAndSchemaPropertiesTestCases() {
            return Stream.of(
                Arguments.of(
                    "Русская локаль",
                    Locale.forLanguageTag("ru"),
                    OPEN_API.get(),
                    new ExpectedOpenApi(
                        "TicketFlow — API мероприятий",
                        "API для создания и управления черновиками мероприятий.",
                        "Мероприятия",
                        "Создание черновиков мероприятий.",
                        "Создать мероприятие",
                        "Создаёт черновик мероприятия. Обязательно только название. Для каждой логической команды создания используйте новый ключ Idempotency-Key; повтор с тем же ключом и содержимым возвращает первоначальный результат.",
                        "Черновик мероприятия создан",
                        "Название мероприятия. Единственное обязательное поле при создании черновика.",
                        "Фестиваль")),
                Arguments.of(
                    "Английская локаль",
                    Locale.ENGLISH,
                    OPEN_API.get(),
                    new ExpectedOpenApi(
                        "TicketFlow Events API",
                        "API for creating and managing event drafts.",
                        "Events",
                        "Operations for creating event drafts.",
                        "Create an event",
                        "Creates an event draft. Only the title is required. Use a new Idempotency-Key for each logical create command; a retry with the same key and body returns the original result.",
                        "Event draft created",
                        "Event title. The only field required to create a draft.",
                        "Festival")));
    }

    public static Stream<Arguments> localize_whenOptionalSectionsAreMissing_thenStillSetInfoTestCases() {
            OpenAPI empty = new OpenAPI();
            OpenAPI operationWithoutOptionalSections = new OpenAPI()
                .paths(
                    new Paths()
                        .addPathItem(
                            "/events", new PathItem().post(new Operation())));
            OpenAPI componentsWithoutSchemas = new OpenAPI().components(new Components());
            return Stream.of(
                Arguments.of("Нет разделов", empty, "TicketFlow Events API"),
                Arguments.of(
                    "Есть операция без параметров и ответов",
                    operationWithoutOptionalSections,
                    "TicketFlow Events API"),
                Arguments.of(
                    "Компоненты без схем", componentsWithoutSchemas, "TicketFlow Events API"));
    }

    private OpenApiLocalizerTestCases() {
    }

    private static final Supplier<OpenAPI> OPEN_API = () -> {
        StringSchema title = new StringSchema();
        title.setDescription("i18n:swagger.event.create.title");
        title.setExample("i18n:swagger.event.create.title.example");

        return new OpenAPI()
            .tags(
                List.of(
                    new Tag()
                        .name("i18n:swagger.events.tag.name")
                        .description(
                            "i18n:swagger.events.tag.description")))
            .paths(
                new Paths()
                    .addPathItem(
                        "/api/v1/events",
                        new PathItem()
                            .post(
                                new Operation()
                                    .summary(
                                        "i18n:swagger.events.create.summary")
                                    .description(
                                        "i18n:swagger.events.create.description")
                                    .tags(
                                        List.of(
                                            "i18n:swagger.events.tag.name"))
                                    .responses(
                                        new ApiResponses()
                                            .addApiResponse(
                                                "201",
                                                new ApiResponse()
                                                    .description(
                                                        "i18n:swagger.events.create.response.201"))))))
            .components(
                new Components()
                    .schemas(
                        Map.of(
                            "Request",
                            new ObjectSchema()
                                .addProperty("title", title))));
    };
}
