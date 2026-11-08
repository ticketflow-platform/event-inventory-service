package ru.ticketflowplatform.eventinventoryservice.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Service;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto;

@Service
public class HashService {

    public String hash(CreateEventRequestDto request) {
        try {
            String canonical = String.format("%s|%s|%s|%s|%s", request.title(), request.description(),
                    request.ageRestriction(), request.categoryIds(), request.images());

            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);

            for (byte value : digest) {
                result.append(String.format("%02x", value));
            }

            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to fingerprint event request", exception);
        }
    }
}
