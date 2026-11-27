package ru.ticketflowplatform.eventinventoryservice.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ru.ticketflowplatform.eventinventoryservice.domain.exception.HashGenerationException;
import ru.ticketflowplatform.eventinventoryservice.domain.model.dto.in.CreateEventRequestDto;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class HashService {

    private static final String SHA_256_ALGORITHM = "SHA-256";

    private final JsonMapper requestMapper;

    public String hash(CreateEventRequestDto request) {
            try {
                byte[] canonical = requestMapper.writeValueAsBytes(request);
                byte[] digest = MessageDigest.getInstance(SHA_256_ALGORITHM).digest(canonical);
                return HexFormat.of().formatHex(digest);
            } catch (NoSuchAlgorithmException exception) {
                throw new HashGenerationException(exception);
            }
    }
}
