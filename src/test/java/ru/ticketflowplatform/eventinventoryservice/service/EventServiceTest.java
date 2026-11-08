package ru.ticketflowplatform.eventinventoryservice.service;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.IdempotencyConflictException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.InvalidEventReferenceException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.InvalidEventRequestException;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto.ImageRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.CreateEventResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Category;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Event;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.EventCategory;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.EventImage;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.IdempotencyKey;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Image;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.ImageType;
import ru.ticketflowplatform.eventinventoryservice.domain.repository.CategoryRepository;
import ru.ticketflowplatform.eventinventoryservice.domain.repository.EventRepository;
import ru.ticketflowplatform.eventinventoryservice.domain.repository.IdempotencyKeyRepository;
import ru.ticketflowplatform.eventinventoryservice.domain.repository.ImageRepository;
import ru.ticketflowplatform.eventinventoryservice.mapper.EventCategoryMapper;
import ru.ticketflowplatform.eventinventoryservice.mapper.EventImageMapper;
import ru.ticketflowplatform.eventinventoryservice.mapper.EventMapper;
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
    private IdempotencyKeyRepository idempotencyKeyRepository;
    @Mock
    private EventMapper eventMapper;
    @Mock
    private EventCategoryMapper eventCategoryMapper;
    @Mock
    private EventImageMapper eventImageMapper;
    @Mock
    private IdempotencyKeyMapper idempotencyKeyMapper;
    @Mock
    private HashService hashService;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private EntityManager entityManager;
    @InjectMocks
    private EventService eventService;

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventServiceTestCases#create_whenStoredKeyMatchesRequest_shouldReturnStoredResponseTestCases")
    @DisplayName("Возвращает исходный ответ при повторе запроса по сценариям")
    void create_whenStoredKeyMatchesRequest_shouldReturnStoredResponseTest(
            String scenario,
            CreateEventRequestDto request,
            IdempotencyKey storedKey,
            CreateEventResponseDto expected
    ) {
        Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
        Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.of(storedKey));
        Mockito.when(eventRepository.findById(storedKey.getEvent().getId())).thenReturn(Optional.of(storedKey.getEvent()));
        Mockito.when(eventMapper.toResponse(storedKey.getEvent())).thenReturn(expected);
        Mockito.when(transactionTemplate.execute(Mockito.any())).thenAnswer(invocation -> {
            TransactionCallback<CreateEventResponseDto> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        Assertions.assertSame(expected, eventService.create(request, "organizer-1", "request-1"));

        Mockito.verify(transactionTemplate).execute(Mockito.any());
        Mockito.verifyNoInteractions(entityManager);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventServiceTestCases#create_whenStoredKeyHasDifferentFingerprint_shouldThrowConflictTestCases")
    @DisplayName("Отклоняет повторное использование ключа с другим содержимым по сценариям")
    void create_whenStoredKeyHasDifferentFingerprint_shouldThrowConflictTest(
            String scenario,
            CreateEventRequestDto request,
            IdempotencyKey storedKey
    ) {
        Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
        Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.of(storedKey));

        Assertions.assertThrows(
                IdempotencyConflictException.class,
                () -> eventService.create(request, "organizer-1", "request-1")
        );
        Mockito.verifyNoInteractions(transactionTemplate, eventRepository, entityManager);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventServiceTestCases#create_whenAgeRestrictionIsUnsupported_shouldRejectRequestTestCases")
    @DisplayName("Отклоняет неподдерживаемое возрастное ограничение по сценариям")
    void create_whenAgeRestrictionIsUnsupported_shouldRejectRequestTest(
            String scenario,
            CreateEventRequestDto request
    ) {
        Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
        Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(
                InvalidEventRequestException.class,
                () -> eventService.create(request, "organizer-1", "request-1")
        );
        Mockito.verifyNoInteractions(transactionTemplate, eventRepository, entityManager);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventServiceTestCases#create_whenCategoryIdsContainDuplicates_shouldRejectRequestTestCases")
    @DisplayName("Отклоняет повторяющиеся категории по сценариям")
    void create_whenCategoryIdsContainDuplicates_shouldRejectRequestTest(
            String scenario,
            CreateEventRequestDto request
    ) {
        Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
        Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(
                InvalidEventRequestException.class,
                () -> eventService.create(request, "organizer-1", "request-1")
        );
        Mockito.verifyNoInteractions(categoryRepository, imageRepository, transactionTemplate, eventRepository);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventServiceTestCases#create_whenImageIdsContainDuplicates_shouldRejectRequestTestCases")
    @DisplayName("Отклоняет повторяющиеся изображения по сценариям")
    void create_whenImageIdsContainDuplicates_shouldRejectRequestTest(
            String scenario,
            CreateEventRequestDto request
    ) {
        Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
        Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(
                InvalidEventRequestException.class,
                () -> eventService.create(request, "organizer-1", "request-1")
        );
        Mockito.verifyNoInteractions(categoryRepository, imageRepository, transactionTemplate, eventRepository);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventServiceTestCases#create_whenCategoryIsMissingOrInactive_shouldRejectRequestTestCases")
    @DisplayName("Отклоняет отсутствующую или неактивную категорию по сценариям")
    void create_whenCategoryIsMissingOrInactive_shouldRejectRequestTest(
            String scenario,
            CreateEventRequestDto request,
            List<Category> foundCategories
    ) {
        Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
        Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.empty());
        Mockito.when(categoryRepository.findAllByIdInAndStatus(
                request.categoryIds().stream().map(CreateEventRequestDto.CategoryRequestDto::categoryId).toList(),
                ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.CategoryStatus.ACTIVE
        )).thenReturn(foundCategories);

        Assertions.assertThrows(
                InvalidEventReferenceException.class,
                () -> eventService.create(request, "organizer-1", "request-1")
        );
        Mockito.verifyNoInteractions(imageRepository, transactionTemplate, eventRepository, entityManager);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventServiceTestCases#create_whenImageIsMissingOrForeign_shouldRejectRequestTestCases")
    @DisplayName("Отклоняет отсутствующее или чужое изображение по сценариям")
    void create_whenImageIsMissingOrForeign_shouldRejectRequestTest(
            String scenario,
            CreateEventRequestDto request,
            List<Image> foundImages
    ) {
        Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
        Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.empty());
        List<Category> categories = request.categoryIds().stream().map(categoryRequest -> {
            Category category = new Category();
            category.setId(categoryRequest.categoryId());
            category.setStatus(ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.CategoryStatus.ACTIVE);
            return category;
        }).toList();
        Mockito.when(categoryRepository.findAllByIdInAndStatus(
                request.categoryIds().stream().map(CreateEventRequestDto.CategoryRequestDto::categoryId).toList(),
                ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.CategoryStatus.ACTIVE
        )).thenReturn(categories);
        Mockito.when(imageRepository.findAllByIdInAndOrganizerId(
                request.images().stream().map(ImageRequestDto::imageId).toList(), "organizer-1"
        )).thenReturn(foundImages);

        Assertions.assertThrows(
                InvalidEventReferenceException.class,
                () -> eventService.create(request, "organizer-1", "request-1")
        );
        Mockito.verifyNoInteractions(transactionTemplate, eventRepository, entityManager);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventServiceTestCases#create_whenRequiredReferencesAreMissing_shouldRejectRequestTestCases")
    @DisplayName("Создаёт черновик только с названием")
    void create_whenOnlyTitleIsProvided_shouldCreateDraft(String scenario, CreateEventRequestDto request) {
        Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
        Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.empty());
        Event event = new Event();
        event.setEventCategories(new java.util.ArrayList<>());
        event.setEventImages(new java.util.ArrayList<>());
        Mockito.when(eventMapper.toEntity(Mockito.eq(request), Mockito.eq("organizer-1"), Mockito.isNull(), Mockito.any()))
                .thenReturn(event);
        Mockito.when(eventRepository.save(event)).thenReturn(event);
        Mockito.when(eventMapper.toResponse(event)).thenReturn(Mockito.mock(CreateEventResponseDto.class));
        Mockito.when(idempotencyKeyMapper.toEntity(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.eq(event), Mockito.any())).thenReturn(new IdempotencyKey());
        Mockito.when(transactionTemplate.execute(Mockito.any())).thenAnswer(invocation -> {
            TransactionCallback<CreateEventResponseDto> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        Assertions.assertNotNull(eventService.create(request, "organizer-1", "request-1"));
        Mockito.verifyNoInteractions(categoryRepository, imageRepository);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventServiceTestCases#create_whenReferencesAreValid_shouldPersistAllLinksInOneTransactionTestCases")
    @DisplayName("Создаёт связи категорий и изображений в одной транзакции по сценариям")
    void create_whenReferencesAreValid_shouldPersistAllLinksInOneTransactionTest(
            String scenario,
            CreateEventRequestDto request,
            Event event,
            Integer expectedAgeRestriction,
            List<Category> categories,
            List<Image> images,
            int expectedCategoryLinks,
            int expectedImageLinks,
            CreateEventResponseDto expected
    ) {
        Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
        Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.empty());
        Mockito.when(categoryRepository.findAllByIdInAndStatus(Mockito.anyCollection(), Mockito.any())).thenReturn(categories);
        Mockito.when(imageRepository.findAllByIdInAndOrganizerId(Mockito.anyCollection(), Mockito.eq("organizer-1")))
                .thenReturn(images);
        Mockito.when(eventMapper.toEntity(
                Mockito.eq(request), Mockito.eq("organizer-1"), Mockito.eq(expectedAgeRestriction), Mockito.any(Instant.class)
        )).thenReturn(event);
        Mockito.when(eventRepository.save(event)).thenReturn(event);
        Mockito.when(eventMapper.toResponse(event)).thenReturn(expected);
        Mockito.when(idempotencyKeyMapper.toEntity(
                Mockito.eq("organizer-1"), Mockito.eq("request-1"), Mockito.eq("fingerprint"),
                Mockito.eq(event), Mockito.any(Instant.class)
        )).thenReturn(new IdempotencyKey());
        Mockito.when(eventCategoryMapper.toEntity(
                Mockito.any(Event.class), Mockito.any(Category.class), Mockito.any(Instant.class)
        )).thenAnswer(invocation -> {
            Event linkedEvent = invocation.getArgument(0);
            Category linkedCategory = invocation.getArgument(1);
            Instant createdAt = invocation.getArgument(2);
            return new EventCategory(
                    new EventCategory.EventCategoryId(linkedEvent.getId(), linkedCategory.getId()),
                    createdAt,
                    linkedEvent,
                    linkedCategory
            );
        });
        Mockito.when(eventImageMapper.toEntity(
                Mockito.any(Event.class), Mockito.any(Image.class), Mockito.any(), Mockito.any(Instant.class)
        )).thenAnswer(invocation -> {
            Event linkedEvent = invocation.getArgument(0);
            Image linkedImage = invocation.getArgument(1);
            ImageType type = invocation.getArgument(2);
            Instant createdAt = invocation.getArgument(3);
            return new EventImage(
                    new EventImage.EventImageId(linkedEvent.getId(), linkedImage.getId()),
                    type,
                    createdAt,
                    linkedEvent,
                    linkedImage
            );
        });
        Mockito.when(transactionTemplate.execute(Mockito.any())).thenAnswer(invocation -> {
            TransactionCallback<CreateEventResponseDto> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        Assertions.assertSame(expected, eventService.create(request, "organizer-1", "request-1"));

        Mockito.verify(transactionTemplate).execute(Mockito.any());
        Mockito.verify(entityManager, Mockito.times(expectedCategoryLinks)).persist(Mockito.any(EventCategory.class));
        Mockito.verify(entityManager, Mockito.times(expectedImageLinks)).persist(Mockito.any(EventImage.class));
        Mockito.verify(idempotencyKeyRepository).save(Mockito.any(IdempotencyKey.class));
    }
}
