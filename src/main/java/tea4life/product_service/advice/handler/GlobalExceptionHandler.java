package tea4life.product_service.advice.handler;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tea4life.product_service.advice.exception.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import tea4life.product_service.dto.base.ApiResponse;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        // Tạo một cấu trúc JSON đẹp để trả về cho Frontend
        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("timestamp", Instant.now());
        errorResponse.put("status", HttpStatus.NOT_FOUND.value());
        errorResponse.put("error", "Not Found");
        errorResponse.put("message", ex.getMessage()); // Sẽ in ra: "Danh mục không tồn tại"

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // Bắt lỗi toàn vẹn dữ liệu (VD: Thêm trùng sản phẩm yêu thích)
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // Trả về HTTP Status 400
    public ApiResponse<Void> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.warn("DataIntegrityViolationException: {}", ex.getMessage());
        return new ApiResponse<>(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
    }

    // Tiện tay bắt luôn lỗi không tìm thấy sản phẩm
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleEntityNotFoundException(EntityNotFoundException ex) {
        log.warn("EntityNotFoundException: {}", ex.getMessage());
        return new ApiResponse<>(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
    }

    // Bắt các lỗi Runtime khác để tránh rò rỉ Stack Trace ra ngoài
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // Trả về HTTP Status 500
    public ApiResponse<Void> handleRuntimeException(RuntimeException ex) {
        log.error("RuntimeException: {}", ex.getMessage(), ex);
        return new ApiResponse<>("Lỗi hệ thống nội bộ, vui lòng thử lại sau!", HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Objects.toString(error.getDefaultMessage(), "Du lieu khong hop le"))
                .findFirst()
                .orElse("Du lieu khong hop le");
        return new ApiResponse<>(message, HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException ex) {
        return new ApiResponse<>(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
    }
}
