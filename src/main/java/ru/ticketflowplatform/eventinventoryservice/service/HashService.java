package ru.ticketflowplatform.eventinventoryservice.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Service;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.HashGenerationException;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto;

@Service
public class HashService {

    private static final String EVENT_REQUEST_CANONICAL_FORMAT = "%s|%s|%s|%s|%s";
    private static final String SHA_256_ALGORITHM = "SHA-256";
    private static final String HASH_BYTE_FORMAT = "%02x";

    public String hash(CreateEventRequestDto request) {
        try {
            String canonical = String.format(
                EVENT_REQUEST_CANONICAL_FORMAT,
                request.title(),
                request.description(),
                request.ageRestriction(),
                request.categoryIds(),
                request.images()
            );

            byte[] digest = MessageDigest.getInstance(SHA_256_ALGORITHM).digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);

            for (byte value : digest) {
                result.append(String.format(HASH_BYTE_FORMAT, value));
            }

            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new HashGenerationException(exception);
        }
    }
}
