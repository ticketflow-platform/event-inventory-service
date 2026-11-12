package ru.ticketflowplatform.eventinventoryservice.service;

import jakarta.persistence.EntityManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
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
    private EntityManager entityManager;
    @InjectMocks
    private EventService eventService;

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventServiceTestCases#create_whenStoredKeyMatchesRequest_shouldReturnStoredResponseTestCases")
    @DisplayName("╨Т╨╛╨╖╨▓╤А╨░╤Й╨░╨╡╤В ╨╕╤Б╤Е╨╛╨┤╨╜╤Л╨╣ ╨╛╤В╨▓╨╡╤В ╨┐╤А╨╕ ╨┐╨╛╨▓╤В╨╛╤А╨╡ ╨╖╨░╨┐╤А╨╛╤Б╨░ ╨┐╨╛ ╤Б╤Ж╨╡╨╜╨░╤А╨╕╤П╨╝")
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
        Assertions.assertSame(expected, eventService.create(request, "organizer-1", "request-1"));

        Mockito.verifyNoInteractions(entityManager);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventServiceTestCases#create_whenStoredKeyHasDifferentFingerprint_shouldThrowConflictTestCases")
    @DisplayName("╨Ю╤В╨║╨╗╨╛╨╜╤П╨╡╤В ╨┐╨╛╨▓╤В╨╛╤А╨╜╨╛╨╡ ╨╕╤Б╨┐╨╛╨╗╤М╨╖╨╛╨▓╨░╨╜╨╕╨╡ ╨║╨╗╤О╤З╨░ ╤Б ╨┤╤А╤Г╨│╨╕╨╝ ╤Б╨╛╨┤╨╡╤А╨╢╨╕╨╝╤Л╨╝ ╨┐╨╛ ╤Б╤Ж╨╡╨╜╨░╤А╨╕╤П╨╝")
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
        Mockito.verifyNoInteractions(eventRepository, entityManager);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventServiceTestCases#create_whenAgeRestrictionIsUnsupported_shouldRejectRequestTestCases")
    @DisplayName("╨Ю╤В╨║╨╗╨╛╨╜╤П╨╡╤В ╨╜╨╡╨┐╨╛╨┤╨┤╨╡╤А╨╢╨╕╨▓╨░╨╡╨╝╨╛╨╡ ╨▓╨╛╨╖╤А╨░╤Б╤В╨╜╨╛╨╡ ╨╛╨│╤А╨░╨╜╨╕╤З╨╡╨╜╨╕╨╡ ╨┐╨╛ ╤Б╤Ж╨╡╨╜╨░╤А╨╕╤П╨╝")
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
        Mockito.verifyNoInteractions(eventRepository, entityManager);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventServiceTestCases#create_whenCategoryIdsContainDuplicates_shouldRejectRequestTestCases")
    @DisplayName("╨Ю╤В╨║╨╗╨╛╨╜╤П╨╡╤В ╨┐╨╛╨▓╤В╨╛╤А╤П╤О╤Й╨╕╨╡╤Б╤П ╨║╨░╤В╨╡╨│╨╛╤А╨╕╨╕ ╨┐╨╛ ╤Б╤Ж╨╡╨╜╨░╤А╨╕╤П╨╝")
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
        Mockito.verifyNoInteractions(categoryRepository, imageRepository, eventRepository);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventServiceTestCases#create_whenImageIdsContainDuplicates_shouldRejectRequestTestCases")
    @DisplayName("╨Ю╤В╨║╨╗╨╛╨╜╤П╨╡╤В ╨┐╨╛╨▓╤В╨╛╤А╤П╤О╤Й╨╕╨╡╤Б╤П ╨╕╨╖╨╛╨▒╤А╨░╨╢╨╡╨╜╨╕╤П ╨┐╨╛ ╤Б╤Ж╨╡╨╜╨░╤А╨╕╤П╨╝")
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
        Mockito.verifyNoInteractions(categoryRepository, imageRepository, eventRepository);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventServiceTestCases#create_whenCategoryIsMissingOrInactive_shouldRejectRequestTestCases")
    @DisplayName("╨Ю╤В╨║╨╗╨╛╨╜╤П╨╡╤В ╨╛╤В╤Б╤Г╤В╤Б╤В╨▓╤Г╤О╤Й╤Г╤О ╨╕╨╗╨╕ ╨╜╨╡╨░╨║╤В╨╕╨▓╨╜╤Г╤О ╨║╨░╤В╨╡╨│╨╛╤А╨╕╤О ╨┐╨╛ ╤Б╤Ж╨╡╨╜╨░╤А╨╕╤П╨╝")
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
        Mockito.verifyNoInteractions(imageRepository, eventRepository, entityManager);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventServiceTestCases#create_whenImageIsMissingOrForeign_shouldRejectRequestTestCases")
    @DisplayName("╨Ю╤В╨║╨╗╨╛╨╜╤П╨╡╤В ╨╛╤В╤Б╤Г╤В╤Б╤В╨▓╤Г╤О╤Й╨╡╨╡ ╨╕╨╗╨╕ ╤З╤Г╨╢╨╛╨╡ ╨╕╨╖╨╛╨▒╤А╨░╨╢╨╡╨╜╨╕╨╡ ╨┐╨╛ ╤Б╤Ж╨╡╨╜╨░╤А╨╕╤П╨╝")
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
        Mockito.verifyNoInteractions(eventRepository, entityManager);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventServiceTestCases#create_whenRequiredReferencesAreMissing_shouldRejectRequestTestCases")
    @DisplayName("╨б╨╛╨╖╨┤╨░╤С╤В ╤З╨╡╤А╨╜╨╛╨▓╨╕╨║ ╤В╨╛╨╗╤М╨║╨╛ ╤Б ╨╜╨░╨╖╨▓╨░╨╜╨╕╨╡╨╝")
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
        Assertions.assertNotNull(eventService.create(request, "organizer-1", "request-1"));
        Mockito.verifyNoInteractions(categoryRepository, imageRepository);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ru.ticketflowplatform.eventinventoryservice.scenario.cases.EventServiceTestCases#create_whenReferencesAreValid_shouldPersistAllLinksInOneTransactionTestCases")
    @DisplayName("╨б╨╛╨╖╨┤╨░╤С╤В ╤Б╨▓╤П╨╖╨╕ ╨║╨░╤В╨╡╨│╨╛╤А╨╕╨╣ ╨╕ ╨╕╨╖╨╛╨▒╤А╨░╨╢╨╡╨╜╨╕╨╣ ╨▓ ╨╛╨┤╨╜╨╛╨╣ ╤В╤А╨░╨╜╨╖╨░╨║╤Ж╨╕╨╕ ╨┐╨╛ ╤Б╤Ж╨╡╨╜╨░╤А╨╕╤П╨╝")
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
        Assertions.assertSame(expected, eventService.create(request, "organizer-1", "request-1"));

        Mockito.verify(entityManager, Mockito.times(expectedCategoryLinks)).persist(Mockito.any(EventCategory.class));
        Mockito.verify(entityManager, Mockito.times(expectedImageLinks)).persist(Mockito.any(EventImage.class));
        Mockito.verify(idempotencyKeyRepository).saveAndFlush(Mockito.any(IdempotencyKey.class));
    }

    @Test
    @DisplayName("Преобразует конфликт уникальности ключа в 409")
    void create_whenIdempotencyKeyUniqueConstraintConflicts_shouldReturn409() {
        CreateEventRequestDto request = new CreateEventRequestDto("Concert", null, null, null, null);
        Event event = new Event();
        event.setEventCategories(new java.util.ArrayList<>());
        event.setEventImages(new java.util.ArrayList<>());
        Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
        Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.empty());
        Mockito.when(eventMapper.toEntity(Mockito.eq(request), Mockito.eq("organizer-1"), Mockito.isNull(), Mockito.any()))
                .thenReturn(event);
        Mockito.when(eventRepository.save(event)).thenReturn(event);
        Mockito.when(idempotencyKeyMapper.toEntity(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.eq(event), Mockito.any())).thenReturn(new IdempotencyKey());
        Mockito.doThrow(violation("uq_idempotency_organizer_key"))
                .when(idempotencyKeyRepository).saveAndFlush(Mockito.any(IdempotencyKey.class));

        IdempotencyConflictException exception = Assertions.assertThrows(IdempotencyConflictException.class,
                () -> eventService.create(request, "organizer-1", "request-1"));

        Assertions.assertEquals(409, exception.getStatus());
    }

    @Test
    @DisplayName("Не маскирует другие нарушения ограничений базы данных")
    void create_whenAnotherConstraintFails_shouldRethrowDatabaseError() {
        CreateEventRequestDto request = new CreateEventRequestDto("Concert", null, null, null, null);
        Event event = new Event();
        event.setEventCategories(new java.util.ArrayList<>());
        event.setEventImages(new java.util.ArrayList<>());
        DataIntegrityViolationException expected = violation("events_images_image_id_fkey");
        Mockito.when(hashService.hash(request)).thenReturn("fingerprint");
        Mockito.when(idempotencyKeyRepository.findByOrganizerIdAndKey("organizer-1", "request-1"))
                .thenReturn(Optional.empty());
        Mockito.when(eventMapper.toEntity(Mockito.eq(request), Mockito.eq("organizer-1"), Mockito.isNull(), Mockito.any()))
                .thenReturn(event);
        Mockito.when(eventRepository.save(event)).thenReturn(event);
        Mockito.when(idempotencyKeyMapper.toEntity(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.eq(event), Mockito.any())).thenReturn(new IdempotencyKey());
        Mockito.doThrow(expected).when(idempotencyKeyRepository).saveAndFlush(Mockito.any(IdempotencyKey.class));

        Assertions.assertSame(expected, Assertions.assertThrows(DataIntegrityViolationException.class,
                () -> eventService.create(request, "organizer-1", "request-1")));
    }

    private DataIntegrityViolationException violation(String constraintName) {
        return new DataIntegrityViolationException("Constraint violation",
                new ConstraintViolationException("Constraint violation", new SQLException("Constraint violation"), constraintName));
    }
}
