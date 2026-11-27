package ru.ticketflowplatform.eventinventoryservice.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.EventNotFoundException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.IdempotencyConflictException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.InvalidEventReferenceException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.InvalidEventRequestException;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto.ImageRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.CreateEventResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Category;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Event;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.IdempotencyKey;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Image;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.CategoryStatus;
import ru.ticketflowplatform.eventinventoryservice.domain.repository.CategoryRepository;
import ru.ticketflowplatform.eventinventoryservice.domain.repository.EventCategoryRepository;
import ru.ticketflowplatform.eventinventoryservice.domain.repository.EventImageRepository;
import ru.ticketflowplatform.eventinventoryservice.domain.repository.EventRepository;
import ru.ticketflowplatform.eventinventoryservice.domain.repository.IdempotencyKeyRepository;
import ru.ticketflowplatform.eventinventoryservice.domain.repository.ImageRepository;
import ru.ticketflowplatform.eventinventoryservice.mapper.EventCategoryMapper;
import ru.ticketflowplatform.eventinventoryservice.mapper.EventCategoryMapperImpl;
import ru.ticketflowplatform.eventinventoryservice.mapper.EventImageMapper;
import ru.ticketflowplatform.eventinventoryservice.mapper.EventImageMapperImpl;
import ru.ticketflowplatform.eventinventoryservice.mapper.EventMapper;
import ru.ticketflowplatform.eventinventoryservice.mapper.EventMapperImpl;
import ru.ticketflowplatform.eventinventoryservice.mapper.IdempotencyKeyMapper;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ImageRepository imageRepository;
    @Mock
    private EventCategoryRepository eventCategoryRepository;
    @Mock
    private EventImageRepository eventImageRepository;
    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Spy
    private EventMapper eventMapper = new EventMapperImpl(new EventCategoryMapperImpl(), new EventImageMapperImpl());

    @Spy
    private EventCategoryMapper eventCategoryMapper = new EventCategoryMapperImpl();
    @Spy
    private EventImageMapper eventImageMapper = new EventImageMapperImpl();
    @Mock
    private IdempotencyKeyMapper idempotencyKeyMapper;
    @Mock
    private HashService hashService;
    @InjectMocks
    private EventService eventService;

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.service.EventServiceTestCases#create_whenStoredImagesAreUnordered_thenReturnOrderedResponseTestCases")
    @DisplayName("Возвращает обложку первой без сортировки галереи по id при идемпотентном повторе")
    void create_whenStoredImagesAreUnordered_thenReturnOrderedResponseTest(
        String scenario,
        CreateEventRequestDto request,
        IdempotencyKey storedKey,
        CreateEventResponseDto expected) {
            Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
            Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.of(storedKey));
            Mockito.when(eventRepository.findById(storedKey.getEvent().getId()))
                .thenReturn(Optional.of(storedKey.getEvent()));

            Assertions.assertThat(eventService.create(request, "organizer-1", "request-1"))
                .isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.service.EventServiceTestCases#create_whenStoredKeyMatchesRequest_thenReturnStoredResponseTestCases")
    @DisplayName("Возвращает исходный ответ при повторе запроса по сценариям")
    void create_whenStoredKeyMatchesRequest_thenReturnStoredResponseTest(
        String scenario,
        CreateEventRequestDto request,
        IdempotencyKey storedKey,
        CreateEventResponseDto expected) {
            Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
            Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.of(storedKey));
            Mockito.when(eventRepository.findById(storedKey.getEvent().getId()))
                .thenReturn(Optional.of(storedKey.getEvent()));
            Mockito.when(eventMapper.toResponse(Mockito.eq(storedKey.getEvent()), Mockito.anyList()))
                .thenReturn(expected);
            Assertions.assertThat(eventService.create(request, "organizer-1", "request-1"))
                .isSameAs(expected);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.service.EventServiceTestCases#create_whenStoredKeyReferencesMissingEvent_thenThrowNotFoundTestCases")
    @DisplayName("Возвращает 404, если ключ ссылается на отсутствующее событие")
    void create_whenStoredKeyReferencesMissingEvent_thenThrowNotFoundTest(
        String scenario,
        CreateEventRequestDto request,
        IdempotencyKey storedKey,
        UUID eventId) {
            Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
            Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.of(storedKey));
            Mockito.when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            Assertions.assertThatThrownBy(
                () -> eventService.create(request, "organizer-1", "request-1"))
                .isInstanceOfSatisfying(
                    EventNotFoundException.class,
                    exception -> Assertions.assertThat(exception.getArguments())
                        .containsExactly(eventId, "request-1"));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.service.EventServiceTestCases#create_whenStoredKeyHasDifferentFingerprint_thenThrowConflictTestCases")
    @DisplayName("Отклоняет повторное использование ключа с другим содержимым по сценариям")
    void create_whenStoredKeyHasDifferentFingerprint_thenThrowConflictTest(
        String scenario, CreateEventRequestDto request, IdempotencyKey storedKey) {
            Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
            Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.of(storedKey));

            Assertions.assertThatThrownBy(
                () -> eventService.create(request, "organizer-1", "request-1"))
                .isInstanceOf(IdempotencyConflictException.class);
            Mockito.verifyNoInteractions(eventRepository);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.service.EventServiceTestCases#create_whenAgeRestrictionIsUnsupported_thenRejectRequestTestCases")
    @DisplayName("Отклоняет неподдерживаемое возрастное ограничение по сценариям")
    void create_whenAgeRestrictionIsUnsupported_thenRejectRequestTest(
        String scenario, CreateEventRequestDto request, Integer expectedValue) {
            Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
            Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.empty());

            Assertions.assertThatThrownBy(
                () -> eventService.create(request, "organizer-1", "request-1"))
                .isInstanceOfSatisfying(
                    InvalidEventRequestException.class,
                    exception -> Assertions.assertThat(exception.getArguments())
                        .containsExactly(expectedValue));
            Mockito.verifyNoInteractions(eventRepository);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.service.EventServiceTestCases#create_whenImageListDoesNotHaveExactlyOneCover_thenRejectRequestTestCases")
    @DisplayName("Отклоняет непустой список изображений без единственной обложки по сценариям")
    void create_whenImageListDoesNotHaveExactlyOneCover_thenRejectRequestTest(
        String scenario, CreateEventRequestDto request) {
            Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
            Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.empty());

            Assertions.assertThatThrownBy(
                () -> eventService.create(request, "organizer-1", "request-1"))
                .isInstanceOfSatisfying(
                    InvalidEventRequestException.class,
                    exception -> Assertions.assertThat(exception.getMessageCode())
                        .isEqualTo("api.event.images.cover.invalid.message"));
            Mockito.verifyNoInteractions(categoryRepository, imageRepository, eventRepository);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.service.EventServiceTestCases#create_whenCategoryIdsContainDuplicates_thenRejectRequestTestCases")
    @DisplayName("Отклоняет повторяющиеся категории по сценариям")
    void create_whenCategoryIdsContainDuplicates_thenRejectRequestTest(
        String scenario, CreateEventRequestDto request) {
            Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
            Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.empty());

            Assertions.assertThatThrownBy(
                () -> eventService.create(request, "organizer-1", "request-1"))
                .isInstanceOf(InvalidEventRequestException.class);
            Mockito.verifyNoInteractions(categoryRepository, imageRepository, eventRepository);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.service.EventServiceTestCases#create_whenImageIdsContainDuplicates_thenRejectRequestTestCases")
    @DisplayName("Отклоняет повторяющиеся изображения по сценариям")
    void create_whenImageIdsContainDuplicates_thenRejectRequestTest(
        String scenario, CreateEventRequestDto request) {
            Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
            Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.empty());

            Assertions.assertThatThrownBy(
                () -> eventService.create(request, "organizer-1", "request-1"))
                .isInstanceOf(InvalidEventRequestException.class);
            Mockito.verifyNoInteractions(categoryRepository, imageRepository, eventRepository);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.service.EventServiceTestCases#create_whenCategoryIsMissingOrInactive_thenRejectRequestTestCases")
    @DisplayName("Отклоняет отсутствующую или неактивную категорию по сценариям")
    void create_whenCategoryIsMissingOrInactive_thenRejectRequestTest(
        String scenario,
        CreateEventRequestDto request,
        List<Category> foundCategories,
        String expectedMessageCode) {
            Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
            Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.empty());
            Mockito.when(
                categoryRepository.findAllByIdInAndStatus(
                    request.categoryIds().stream()
                        .map(CreateEventRequestDto.CategoryRequestDto::id)
                        .toList(),
                    CategoryStatus.ACTIVE))
                .thenReturn(foundCategories);

            Assertions.assertThatThrownBy(
                () -> eventService.create(request, "organizer-1", "request-1"))
                .isInstanceOfSatisfying(
                    InvalidEventReferenceException.class,
                    exception -> Assertions.assertThat(exception.getMessageCode())
                        .isEqualTo(expectedMessageCode));
            Mockito.verifyNoInteractions(imageRepository, eventRepository);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.service.EventServiceTestCases#create_whenImageIsMissingOrForeign_thenRejectRequestTestCases")
    @DisplayName("Отклоняет отсутствующее или чужое изображение по сценариям")
    void create_whenImageIsMissingOrForeign_thenRejectRequestTest(
        String scenario,
        CreateEventRequestDto request,
        List<Category> categories,
        List<Image> foundImages,
        String expectedMessageCode) {
            Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
            Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.empty());
            Mockito.when(
                categoryRepository.findAllByIdInAndStatus(
                    request.categoryIds().stream()
                        .map(CreateEventRequestDto.CategoryRequestDto::id)
                        .toList(),
                    CategoryStatus.ACTIVE))
                .thenReturn(categories);
            Mockito.when(
                imageRepository.findAllByIdInAndOrganizerId(
                    request.images().stream().map(ImageRequestDto::id).toList(),
                    "organizer-1"))
                .thenReturn(foundImages);

            Assertions.assertThatThrownBy(
                () -> eventService.create(request, "organizer-1", "request-1"))
                .isInstanceOfSatisfying(
                    InvalidEventReferenceException.class,
                    exception -> Assertions.assertThat(exception.getMessageCode())
                        .isEqualTo(expectedMessageCode));
            Mockito.verifyNoInteractions(eventRepository);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.service.EventServiceTestCases#create_whenOnlyTitleIsProvided_thenCreateDraftTestCases")
    @DisplayName("Создает черновик только с названием")
    void create_whenOnlyTitleIsProvided_thenCreateDraftTest(
        String scenario,
        CreateEventRequestDto request,
        Event event,
        CreateEventResponseDto expected) {
            Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
            Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.empty());
            Mockito.when(
                eventMapper.toEntity(
                    Mockito.eq(request),
                    Mockito.eq("organizer-1"),
                    Mockito.isNull(),
                    Mockito.any()))
                .thenReturn(event);
            Mockito.when(eventRepository.save(event)).thenReturn(event);
            Mockito.when(eventMapper.toResponse(Mockito.eq(event), Mockito.anyList()))
                .thenReturn(expected);
            Mockito.when(
                idempotencyKeyMapper.toEntity(
                    Mockito.anyString(),
                    Mockito.anyString(),
                    Mockito.anyString(),
                    Mockito.eq(event),
                    Mockito.any()))
                .thenReturn(new IdempotencyKey());
            Assertions.assertThat(eventService.create(request, "organizer-1", "request-1"))
                .isSameAs(expected);
            Mockito.verifyNoInteractions(categoryRepository, imageRepository);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.service.EventServiceTestCases#create_whenReferencesAreValid_thenAttachAllLinksToEventTestCases")
    @DisplayName("Добавляет связи категорий и изображений к мероприятию по сценариям")
    void create_whenReferencesAreValid_thenAttachAllLinksToEventTest(
        String scenario,
        CreateEventRequestDto request,
        Event event,
        Integer expectedAgeRestriction,
        List<Category> categories,
        List<Image> images,
        int expectedCategoryLinks,
        int expectedImageLinks,
        CreateEventResponseDto expected) {
            Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
            Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.empty());
            Mockito.when(
                categoryRepository.findAllByIdInAndStatus(
                    Mockito.anyCollection(), Mockito.any()))
                .thenReturn(categories);
            Mockito.when(
                imageRepository.findAllByIdInAndOrganizerId(
                    Mockito.anyCollection(), Mockito.eq("organizer-1")))
                .thenReturn(images);
            Mockito.when(
                eventMapper.toEntity(
                    Mockito.eq(request),
                    Mockito.eq("organizer-1"),
                    Mockito.eq(expectedAgeRestriction),
                    Mockito.any(Instant.class)))
                .thenReturn(event);
            Mockito.when(eventRepository.save(event)).thenReturn(event);
            Mockito.when(eventMapper.toResponse(Mockito.eq(event), Mockito.anyList()))
                .thenReturn(expected);
            Mockito.when(
                idempotencyKeyMapper.toEntity(
                    Mockito.eq("organizer-1"),
                    Mockito.eq("request-1"),
                    Mockito.eq("fingerprint"),
                    Mockito.eq(event),
                    Mockito.any(Instant.class)))
                .thenReturn(new IdempotencyKey());
            Assertions.assertThat(eventService.create(request, "organizer-1", "request-1"))
                .isSameAs(expected);

            Assertions.assertThat(event.getEventCategories())
                .hasSize(expectedCategoryLinks)
                .allSatisfy(link -> Assertions.assertThat(link.getEvent()).isSameAs(event));
            Assertions.assertThat(event.getEventImages())
                .hasSize(expectedImageLinks)
                .allSatisfy(link -> Assertions.assertThat(link.getEvent()).isSameAs(event));
            Mockito.verify(eventCategoryRepository).saveAll(event.getEventCategories());
            Mockito.verify(eventImageRepository).saveAll(event.getEventImages());
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.service.EventServiceTestCases#create_whenIdempotencyKeyUniqueConstraintConflicts_thenReturn409TestCases")
    @DisplayName("Преобразует конфликт уникальности ключа в 409 по сценариям")
    void create_whenIdempotencyKeyUniqueConstraintConflicts_thenReturn409Test(
        String scenario,
        CreateEventRequestDto request,
        Event event,
        DataIntegrityViolationException failure,
        int expectedStatus) {
            Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
            Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.empty());
            Mockito.when(
                eventMapper.toEntity(
                    Mockito.eq(request),
                    Mockito.eq("organizer-1"),
                    Mockito.isNull(),
                    Mockito.any()))
                .thenReturn(event);
            Mockito.when(eventRepository.save(event)).thenReturn(event);
            Mockito.when(
                idempotencyKeyMapper.toEntity(
                    Mockito.anyString(),
                    Mockito.anyString(),
                    Mockito.anyString(),
                    Mockito.eq(event),
                    Mockito.any()))
                .thenReturn(new IdempotencyKey());
            Mockito.doThrow(failure)
                .when(idempotencyKeyRepository)
                .saveAndFlush(Mockito.any(IdempotencyKey.class));

            Assertions.assertThatThrownBy(
                () -> eventService.create(request, "organizer-1", "request-1"))
                .isInstanceOfSatisfying(
                    IdempotencyConflictException.class,
                    exception -> Assertions.assertThat(exception.getStatus())
                        .isEqualTo(expectedStatus));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.service.EventServiceTestCases#create_whenAnotherConstraintFails_thenRethrowDatabaseErrorTestCases")
    @DisplayName("Не маскирует другие нарушения ограничений базы данных по сценариям")
    void create_whenAnotherConstraintFails_thenRethrowDatabaseErrorTest(
        String scenario,
        CreateEventRequestDto request,
        Event event,
        DataIntegrityViolationException failure) {
            Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
            Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.empty());
            Mockito.when(
                eventMapper.toEntity(
                    Mockito.eq(request),
                    Mockito.eq("organizer-1"),
                    Mockito.isNull(),
                    Mockito.any()))
                .thenReturn(event);
            Mockito.when(eventRepository.save(event)).thenReturn(event);
            Mockito.when(
                idempotencyKeyMapper.toEntity(
                    Mockito.anyString(),
                    Mockito.anyString(),
                    Mockito.anyString(),
                    Mockito.eq(event),
                    Mockito.any()))
                .thenReturn(new IdempotencyKey());
            Mockito.doThrow(failure)
                .when(idempotencyKeyRepository)
                .saveAndFlush(Mockito.any(IdempotencyKey.class));

            Assertions.assertThatThrownBy(
                () -> eventService.create(request, "organizer-1", "request-1"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isSameAs(failure);
    }
}
