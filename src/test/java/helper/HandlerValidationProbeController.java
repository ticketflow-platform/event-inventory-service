package helper;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HandlerValidationProbeController {

    @GetMapping("/__test/validation/header")
    public String validateHeader(
        @RequestHeader @NotBlank(message = "Probe must not be blank") String probe) {
        return probe;
    }

    @GetMapping("/__test/validation/query")
    public String validateQuery(
        @RequestParam("probe") @NotBlank(message = "Probe must not be blank") String probe) {
        return probe;
    }

    @GetMapping("/__test/validation/query/implicit")
    public String validateImplicitQuery(
        @RequestParam @NotBlank(message = "Probe must not be blank") String probe) {
        return probe;
    }

    @PostMapping("/__test/validation/body")
    public String validateBody(@Valid @RequestBody ProbeRequest request) {
        return request.probe();
    }

    public record ProbeRequest(@NotBlank(message = "Probe must not be blank") String probe) {
    }
}
