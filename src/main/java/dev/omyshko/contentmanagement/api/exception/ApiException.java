package dev.omyshko.contentmanagement.api.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Data
public class ApiException extends RuntimeException {
    private String code;
    private HttpStatus status;
}
