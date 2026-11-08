package ru.ticketflowplatform.eventinventoryservice.service;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
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

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final ImageRepository imageRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final EventMapper eventMapper;
    private final EventCategoryMapper eventCategoryMapper;
    private final EventImageMapper eventImageMapper;
    private final IdempotencyKeyMapper idempotencyKeyMapper;
    private final HashService hashService;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;

    public CreateEventResponseDto create(CreateEventRequestDto request, String organizerId, String idempotencyKey) {
        String requestHash = hashService.hash(request);
        Optional<IdempotencyKey> storedKey = idempotencyKeyRepository.findByOrganizerIdAndKey(organizerId, idempotencyKey);

        if (storedKey.isEmpty()) {
            Integer ageRestriction = request.ageRestriction();
            if (ageRestriction != null && !AgeRestriction.isSupport(ageRestriction)) {
                throw new InvalidEventRequestException("Неверная возрастная группа", "Возрастная группа должна быть: 0, 6, 12, 16, or 18");
            }

            List<UUID> categoryIds = request.categoryIds() == null
                    ? List.of() : request.categoryIds().stream().map(CategoryRequestDto::categoryId).toList();
            List<ImageRequestDto> imageRequests = request.images() == null ? List.of() : request.images();

            ensureUnique(categoryIds, "Категории должны быть без повторов", "Повторяющиеся категории: %s");

            List<UUID> imageIds = imageRequests.stream().map(ImageRequestDto::imageId).toList();
            ensureUnique(imageIds, "Изображения должны быть без повторов", "Повторяющиеся изображения: %s");

            List<Category> categories = categoryIds.isEmpty() ? List.of() : findActiveCategories(categoryIds);
            List<Image> images = findOwnedImages(imageRequests, organizerId);
            Map<UUID, Image> imagesById = images.stream().collect(Collectors.toMap(Image::getId, image -> image));

            Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
            Event newEvent = eventMapper.toEntity(request, organizerId, ageRestriction, now);
            return Objects.requireNonNull(transactionTemplate.execute(_ -> {
                Event savedEvent = eventRepository.save(newEvent);

                for (Category category : categories) {
                    EventCategory link = eventCategoryMapper.toEntity(savedEvent, category, now);
                    savedEvent.getEventCategories().add(link);
                    entityManager.persist(link);
                }

                for (ImageRequestDto imageRequest : imageRequests) {
                    EventImage link = eventImageMapper.toEntity(
                            savedEvent, imagesById.get(imageRequest.imageId()), imageRequest.type(), now
                    );
                    savedEvent.getEventImages().add(link);
                    entityManager.persist(link);
                }

                idempotencyKeyRepository.save(
                        idempotencyKeyMapper.toEntity(organizerId, idempotencyKey, requestHash, savedEvent, now)
                );

                return eventMapper.toResponse(savedEvent);
            }));
        }

        IdempotencyKey stored = storedKey.orElseThrow();
        if (!stored.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException("Такое мероприятие уже создано", String.format("Мероприятие с ключом: %s уже создано", stored.getId()));
        }

        return Objects.requireNonNull(transactionTemplate.execute(_ ->
                eventRepository.findById(stored.getEvent().getId())
                        .map(eventMapper::toResponse)
                        .orElseThrow()
        ));
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

    private List<Image> findOwnedImages(
            List<ImageRequestDto> imageRequests,
            String organizerId
    ) {
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
}
