package ru.ticketflowplatform.eventinventoryservice.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.EventNotFoundException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.IdempotencyConflictException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.InvalidEventReferenceException;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.InvalidEventRequestException;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto.CategoryRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto.ImageRequestDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.out.CreateEventResponseDto;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Category;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Event;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.EventCategory;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.EventImage;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.IdempotencyKey;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.Image;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.AgeRestriction;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.CategoryStatus;
import ru.ticketflowplatform.eventinventoryservice.domain.model.entity.enums.ImagePosition;
import ru.ticketflowplatform.eventinventoryservice.domain.repository.CategoryRepository;
import ru.ticketflowplatform.eventinventoryservice.domain.repository.EventCategoryRepository;
import ru.ticketflowplatform.eventinventoryservice.domain.repository.EventImageRepository;
import ru.ticketflowplatform.eventinventoryservice.domain.repository.EventRepository;
import ru.ticketflowplatform.eventinventoryservice.domain.repository.IdempotencyKeyRepository;
import ru.ticketflowplatform.eventinventoryservice.domain.repository.ImageRepository;
import ru.ticketflowplatform.eventinventoryservice.mapper.EventCategoryMapper;
import ru.ticketflowplatform.eventinventoryservice.mapper.EventImageMapper;
import ru.ticketflowplatform.eventinventoryservice.mapper.EventMapper;
import ru.ticketflowplatform.eventinventoryservice.mapper.IdempotencyKeyMapper;
import ru.ticketflowplatform.eventinventoryservice.util.CollectionUtil;

@Service
@RequiredArgsConstructor
public class EventService {

    private static final int MATCHING_POSITION_PRIORITY = 0;
    private static final int NON_MATCHING_POSITION_PRIORITY = 1;
    private static final String COMMA = ", ";
    private static final String IDEMPOTENCY_UNIQUE_CONSTRAINT = "uq_idempotency_organizer_key";
    private final EventRepository eventRepository;

    private final CategoryRepository categoryRepository;
    private final ImageRepository imageRepository;
    private final EventCategoryRepository eventCategoryRepository;

    private final EventImageRepository eventImageRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final EventMapper eventMapper;
    private final EventCategoryMapper eventCategoryMapper;
    private final EventImageMapper eventImageMapper;
    private final IdempotencyKeyMapper idempotencyKeyMapper;

    private final HashService hashService;

    @Transactional
    public CreateEventResponseDto create(
        CreateEventRequestDto request,
        String organizerId,
        String idempotencyKey) {
            String requestHash = hashService.hash(request);
            Optional<IdempotencyKey> storedKey = idempotencyKeyRepository.findByOrganizerIdAndKey(
                organizerId,
                idempotencyKey);

            if (storedKey.isPresent()) {
                IdempotencyKey key = storedKey.orElseThrow();
                if (!key.getRequestHash().equals(requestHash)) {
                    throw new IdempotencyConflictException(
                        "api.event.idempotency_conflict.message",
                        "api.event.idempotency_conflict.detail",
                        key.getKey());
                }

                UUID eventId = key.getEvent().getId();
                return eventRepository.findById(eventId)
                    .map(this::toResponse)
                    .orElseThrow(
                        () -> new EventNotFoundException(
                            "api.event.not_found.message",
                            "api.event.not_found.detail",
                            eventId,
                            key.getKey()));
            }

            Integer ageRestriction = request.ageRestriction();
            if (ageRestriction != null && !AgeRestriction.isSupport(ageRestriction)) {
                throw new InvalidEventRequestException(
                    "api.event.age_restriction.invalid.message",
                    "api.event.age_restriction.invalid.detail",
                    ageRestriction);
            }

            List<UUID> categoryIds = CollectionUtil.collectOrEmpty(
                request.categoryIds(),
                Collectors.mapping(CategoryRequestDto::id, Collectors.toCollection(ArrayList::new)));

            ensureUnique(
                categoryIds,
                "api.event.categories.duplicate.message",
                "api.event.categories.duplicate.detail");

            List<ImageRequestDto> imageRequests = CollectionUtil.collectOrEmpty(
                request.images(),
                Collectors.toCollection(ArrayList::new));

            List<UUID> imageIds = CollectionUtil.collectOrEmpty(
                imageRequests,
                Collectors.mapping(ImageRequestDto::id, Collectors.toCollection(ArrayList::new)));

            ensureUnique(
                imageIds,
                "api.event.images.duplicate.message",
                "api.event.images.duplicate.detail");

            long coverCount = imageRequests.stream().filter(image -> image.position() == ImagePosition.COVER).count();
            if (!imageRequests.isEmpty() && coverCount != 1) {
                throw new InvalidEventRequestException(
                    "api.event.images.cover.invalid.message",
                    "api.event.images.cover.invalid.detail");
            }

            List<Category> categories = findActiveCategories(categoryIds);
            List<Image> images = findOwnedImages(imageIds, organizerId);

            Map<UUID, Category> categoriesById = prepareCategoriesByIdMap(categories);

            Map<UUID, Image> imagesById = CollectionUtil.collectOrEmpty(images,
                Collectors.toMap(Image::getId, Function.identity()));

            Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
            Event newEvent = eventMapper.toEntity(request, organizerId, ageRestriction, now);
            Event savedEvent = eventRepository.save(newEvent);

            List<EventCategory> eventCategories = prepareEventCategories(
                categoryIds.stream().map(categoriesById::get).toList(),
                savedEvent,
                now);

            Event eventWithEventCategories = savedEvent.addEventCategories(eventCategories);
            eventCategoryRepository.saveAll(eventCategories);

            List<EventImage> eventImages = prepareEventImages(imageRequests, savedEvent, imagesById, now);
            Event eventWithImages = eventWithEventCategories.addEventImages(eventImages);
            eventImageRepository.saveAll(eventImages);

            try {
                idempotencyKeyRepository.saveAndFlush(
                    idempotencyKeyMapper.toEntity(
                        organizerId,
                        idempotencyKey,
                        requestHash,
                        eventWithImages,
                        now));
            } catch (DataIntegrityViolationException dataIntegrityViolationException) {
                if (isIdempotencyKeyConflict(dataIntegrityViolationException)) {
                    throw new IdempotencyConflictException(
                        "api.event.idempotency_conflict.message",
                        "api.event.idempotency_conflict.detail",
                        idempotencyKey);
                }
                throw dataIntegrityViolationException;
            }

            return toResponse(savedEvent);
    }

    private CreateEventResponseDto toResponse(Event event) {
            List<EventImage> images = sortByPosition(event.getEventImages(), ImagePosition.COVER);
            return eventMapper.toResponse(event, images);
    }

    private List<EventImage> sortByPosition(List<EventImage> eventImages, ImagePosition desiderPosition) {
            return eventImages.stream()
                .sorted(Comparator.comparingInt(
                    image -> image.getPosition() == desiderPosition
                        ? MATCHING_POSITION_PRIORITY
                        : NON_MATCHING_POSITION_PRIORITY))
                .toList();
    }

    private void ensureUnique(List<UUID> ids, String messageCode, String detailCode) {
            Set<UUID> seen = new HashSet<>();
            List<UUID> duplicates = CollectionUtil.filterDistinctOrEmpty(ids, id -> !seen.add(id), Collectors.toList());
            if (!duplicates.isEmpty()) {
                throw new InvalidEventRequestException(messageCode, detailCode, formatIds(duplicates));
            }
    }

    private String formatIds(List<UUID> ids) {
            return String.join(COMMA, CollectionUtil.collectOrEmpty(
                ids, Collectors.mapping(UUID::toString, Collectors.toList())));
    }

    private List<Category> findActiveCategories(List<UUID> ids) {
            List<Category> categories = findAllByIdInAndStatusOrEmpty(ids);
            Set<UUID> foundIds = CollectionUtil.collectOrEmpty(
                categories, Collectors.mapping(Category::getId, Collectors.toSet()));

            List<UUID> missingIds = CollectionUtil.filterDistinctOrEmpty(
                ids, id -> !foundIds.contains(id), Collectors.toList());
            if (!missingIds.isEmpty()) {
                throw new InvalidEventReferenceException(
                    selectMessageCode(
                        missingIds,
                        "api.event.categories.not_found.message.one",
                        "api.event.categories.not_found.message.many"),
                    selectMessageCode(
                        missingIds,
                        "api.event.categories.not_found.detail.one",
                        "api.event.categories.not_found.detail.many"),
                    formatIds(missingIds));
            }

            return categories;
    }

    private List<Category> findAllByIdInAndStatusOrEmpty(List<UUID> ids) {
            return ids.isEmpty()
                ? List.of()
                : categoryRepository.findAllByIdInAndStatus(ids, CategoryStatus.ACTIVE);
    }

    private String selectMessageCode(List<UUID> ids, String one, String many) {
            return ids.size() == 1 ? one : many;
    }

    private List<Image> findOwnedImages(List<UUID> ids, String organizerId) {
            List<Image> images = findAllByIdInAndOrganizerIdOrEmpty(ids, organizerId);
            Set<UUID> foundIds = CollectionUtil.collectOrEmpty(
                images, Collectors.mapping(Image::getId, Collectors.toSet()));
            List<UUID> missingIds = CollectionUtil.filterDistinctOrEmpty(
                ids, id -> !foundIds.contains(id), Collectors.toList());

            if (!missingIds.isEmpty()) {
                throw new InvalidEventReferenceException(
                    selectMessageCode(
                        missingIds,
                        "api.event.images.not_found.message.one",
                        "api.event.images.not_found.message.many"),
                    selectMessageCode(
                        missingIds,
                        "api.event.images.not_found.detail.one",
                        "api.event.images.not_found.detail.many"),
                    formatIds(missingIds));
            }

            return images;
    }

    private List<Image> findAllByIdInAndOrganizerIdOrEmpty(List<UUID> ids, String organizerId) {
            return ids.isEmpty()
                ? List.of()
                : imageRepository.findAllByIdInAndOrganizerId(ids, organizerId);
    }

    private Map<UUID, Category> prepareCategoriesByIdMap(List<Category> categories) {
            return CollectionUtil.collectOrEmpty(categories, Collectors.toMap(Category::getId, Function.identity()));
    }

    private List<EventCategory> prepareEventCategories(
        List<Category> categories,
        Event savedEvent,
        Instant now) {
            return categories.stream()
                .map(category -> eventCategoryMapper.toEntity(savedEvent, category, now))
                .toList();
    }

    private List<EventImage> prepareEventImages(
        List<ImageRequestDto> imageRequests,
        Event savedEvent,
        Map<UUID, Image> imagesById,
        Instant now) {
            return imageRequests.stream()
                .map(
                    imageRequest -> eventImageMapper.toEntity(
                        savedEvent,
                        imagesById.get(imageRequest.id()),
                        imageRequest.position(),
                        now))
                .toList();
    }

    private boolean isIdempotencyKeyConflict(Throwable exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation
                    && IDEMPOTENCY_UNIQUE_CONSTRAINT.equals(violation.getConstraintName())) {
                    return true;
                }
            }

            return false;
    }
}
