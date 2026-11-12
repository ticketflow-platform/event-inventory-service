package ru.ticketflowplatform.eventinventoryservice.service;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import ru.ticketflowplatform.eventinventoryservice.domain.repository.CategoryRepository;
import ru.ticketflowplatform.eventinventoryservice.domain.repository.EventRepository;
import ru.ticketflowplatform.eventinventoryservice.domain.repository.IdempotencyKeyRepository;
import ru.ticketflowplatform.eventinventoryservice.domain.repository.ImageRepository;
import ru.ticketflowplatform.eventinventoryservice.mapper.EventCategoryMapper;
import ru.ticketflowplatform.eventinventoryservice.mapper.EventImageMapper;
import ru.ticketflowplatform.eventinventoryservice.mapper.EventMapper;
import ru.ticketflowplatform.eventinventoryservice.mapper.IdempotencyKeyMapper;

@Service
@RequiredArgsConstructor
public class EventService {

    private static final String IDEMPOTENCY_UNIQUE_CONSTRAINT = "uq_idempotency_organizer_key";

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final ImageRepository imageRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final EventMapper eventMapper;
    private final EventCategoryMapper eventCategoryMapper;
    private final EventImageMapper eventImageMapper;
    private final IdempotencyKeyMapper idempotencyKeyMapper;
    private final HashService hashService;
    private final EntityManager entityManager;

    @Transactional
    public CreateEventResponseDto create(CreateEventRequestDto request, String organizerId, String idempotencyKey) {
        String requestHash = hashService.hash(request);
        Optional<IdempotencyKey> storedKey = idempotencyKeyRepository.findByOrganizerIdAndKey(organizerId, idempotencyKey);
        if (storedKey.isPresent()) {
            IdempotencyKey key = storedKey.orElseThrow();
            if (!key.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException("Такое мероприятие уже создано", String.format("Мероприятие с ключом: %s уже создано", key.getKey()));
            }

            return eventRepository.findById(key.getEvent().getId())
                    .map(eventMapper::toResponse)
                    .orElseThrow();
        }

        Integer ageRestriction = request.ageRestriction();
        if (ageRestriction != null && !AgeRestriction.isSupport(ageRestriction)) {
            throw new InvalidEventRequestException("Неверная возрастная группа", "Возрастная группа должна быть: 0, 6, 12, 16, or 18");
        }

        List<UUID> categoryIds = request.categoryIds() == null ? List.of() : request.categoryIds().stream().map(CategoryRequestDto::categoryId).toList();
        List<ImageRequestDto> imageRequests = request.images() == null ? List.of() : request.images();

        ensureUnique(categoryIds, "Категории должны быть без повторов", "Повторяющиеся категории: %s");

        List<UUID> imageIds = imageRequests.stream().map(ImageRequestDto::imageId).toList();
        ensureUnique(imageIds, "Изображения должны быть без повторов", "Повторяющиеся изображения: %s");

        List<Category> categories = categoryIds.isEmpty() ? List.of() : findActiveCategories(categoryIds);
        List<Image> images = findOwnedImages(imageRequests, organizerId);
        Map<UUID, Image> imagesById = images.stream().collect(Collectors.toMap(Image::getId, image -> image));

        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Event newEvent = eventMapper.toEntity(request, organizerId, ageRestriction, now);
        Event savedEvent = eventRepository.save(newEvent);

        List<EventCategory> eventCategories = categories.stream().map(category -> eventCategoryMapper.toEntity(savedEvent, category, now)).toList();
        Event eventWithEventCategories = savedEvent.addEventCategories(eventCategories);

        List<EventImage> eventImages = imageRequests.stream().map(imageRequest -> eventImageMapper.toEntity(savedEvent, imagesById.get(imageRequest.imageId()), imageRequest.type(), now)).toList();
        Event eventWithImages = eventWithEventCategories.addEventImages(eventImages);

        try {
            idempotencyKeyRepository.saveAndFlush(
                    idempotencyKeyMapper.toEntity(organizerId, idempotencyKey, requestHash, eventWithImages, now)
            );
        } catch (DataIntegrityViolationException exception) {
            if (isIdempotencyKeyConflict(exception)) {
                throw new IdempotencyConflictException(
                        "Такое мероприятие уже создано",
                        String.format("Мероприятие с ключом идемпотентности: %s уже создано", idempotencyKey)
                );
            }
            throw exception;
        }

        return eventMapper.toResponse(savedEvent);
    }

    private void ensureUnique(List<UUID> ids, String message, String detail) {
        Set<UUID> seen = new HashSet<>();
        List<UUID> duplicates = ids.stream().filter(id -> !seen.add(id)).distinct().toList();
        if (!duplicates.isEmpty()) {
            throw new InvalidEventRequestException(message,
                    detail.formatted(String.join(", ", duplicates.stream().map(UUID::toString).toList())));
        }
    }

    private List<Category> findActiveCategories(List<UUID> ids) {
        List<Category> categories = ids.isEmpty() ? List.of() : categoryRepository.findAllByIdInAndStatus(ids, CategoryStatus.ACTIVE);

        Set<UUID> foundIds = categories.stream().map(Category::getId).collect(Collectors.toSet());
        List<UUID> missingIds = ids.stream().filter(id -> !foundIds.contains(id)).distinct().toList();
        if (!missingIds.isEmpty()) {
            throw new InvalidEventReferenceException("Категория не найдена", categoryNotFoundDetail(missingIds));
        }

        return categories;
    }

    private List<Image> findOwnedImages(List<ImageRequestDto> imageRequests, String organizerId) {
        List<UUID> ids = imageRequests.stream().map(ImageRequestDto::imageId).toList();
        List<Image> images = ids.isEmpty() ? List.of() : imageRepository.findAllByIdInAndOrganizerId(ids, organizerId);
        Set<UUID> foundIds = images.stream().map(Image::getId).collect(Collectors.toSet());
        List<UUID> missingIds = ids.stream().filter(id -> !foundIds.contains(id)).distinct().toList();
        if (!missingIds.isEmpty()) {
            throw new InvalidEventReferenceException("Изображение не найдено", imageNotFoundDetail(missingIds));
        }
        return images;
    }

    private String categoryNotFoundDetail(List<UUID> ids) {
        String noun = switch (ids.size()) {
            case 1 -> "Категория с id: %s не найдена";
            case 2, 3, 4 -> "Категории с id: %s не найдены";
            default -> "Категорий с id: %s не найдено";
        };
        return noun.formatted(formatIds(ids));
    }

    private String imageNotFoundDetail(List<UUID> ids) {
        String noun = switch (ids.size()) {
            case 1 -> "Изображение с id: %s не найдено";
            case 2, 3, 4 -> "Изображения с id: %s не найдены";
            default -> "Изображений с id: %s не найдено";
        };
        return noun.formatted(formatIds(ids));
    }

    private String formatIds(List<UUID> ids) {
        return String.join(", ", ids.stream().map(UUID::toString).toList());
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
