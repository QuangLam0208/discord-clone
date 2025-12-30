package hcmute.edu.vn.discord.exception;

import hcmute.edu.vn.discord.dto.response.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.validation.FieldError;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<ErrorResponse> buildError(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        if (status.is5xxServerError()) {
            log.error("[{} {}] {}", request.getMethod(), request.getRequestURI(), message);
        } else {
            log.warn("[{} {}] {} {}", request.getMethod(), request.getRequestURI(), status.value(), message);
        }
        return ResponseEntity.status(status).body(error);
    }

    private static String rootMessage(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String msg = cause.getMessage();
        return (msg != null && !msg.isBlank()) ? msg : ex.getMessage();
    }

    // ===== 400 BAD REQUEST =====

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        // Gom tất cả lỗi field + global vào một chuỗi
        String msg = ex.getBindingResult().getAllErrors().stream()
                .map(err -> {
                    if (err instanceof FieldError fe) {
                        return fe.getField() + ": " + fe.getDefaultMessage();
                    }
                    return err.getObjectName() + ": " + err.getDefaultMessage();
                })
                .collect(Collectors.joining("; "));
        if (msg.isBlank()) msg = "Dữ liệu không hợp lệ";
        return buildError(HttpStatus.BAD_REQUEST, msg, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex,
                                                          HttpServletRequest request) {
        String msg = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        if (msg.isBlank()) msg = "Dữ liệu không hợp lệ";
        return buildError(HttpStatus.BAD_REQUEST, msg, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex,
                                                          HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJson(HttpMessageNotReadableException ex,
                                                           HttpServletRequest request) {
        String msg = "JSON không hợp lệ";
        String cause = rootMessage(ex);
        if (cause != null && !cause.isBlank()) {
            msg += ": " + cause;
        }
        return buildError(HttpStatus.BAD_REQUEST, msg, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex,
                                                            HttpServletRequest request) {
        String msg = "Thiếu tham số: " + ex.getParameterName();
        return buildError(HttpStatus.BAD_REQUEST, msg, request);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex,
                                                             HttpServletRequest request) {
        String msg = "Thiếu header: " + ex.getHeaderName();
        return buildError(HttpStatus.BAD_REQUEST, msg, request);
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ErrorResponse> handleMissingPathVar(MissingPathVariableException ex,
                                                              HttpServletRequest request) {
        String msg = "Thiếu biến đường dẫn: " + ex.getVariableName();
        return buildError(HttpStatus.BAD_REQUEST, msg, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        String expected = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "đúng kiểu";
        String msg = "Tham số '" + ex.getName() + "' không đúng kiểu (mong đợi " + expected + ")";
        return buildError(HttpStatus.BAD_REQUEST, msg, request);
    }

    // ===== 401 UNAUTHORIZED =====

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex,
                                                              HttpServletRequest request) {
        return buildError(HttpStatus.UNAUTHORIZED, "Sai tài khoản hoặc mật khẩu", request);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwt(ExpiredJwtException ex,
                                                          HttpServletRequest request) {
        return buildError(HttpStatus.UNAUTHORIZED, "Token đã hết hạn", request);
    }

    @ExceptionHandler({MalformedJwtException.class, SignatureException.class, UnsupportedJwtException.class})
    public ResponseEntity<ErrorResponse> handleInvalidJwt(Exception ex,
                                                          HttpServletRequest request) {
        return buildError(HttpStatus.UNAUTHORIZED, "Token không hợp lệ", request);
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientAuth(InsufficientAuthenticationException ex,
                                                                HttpServletRequest request) {
        return buildError(HttpStatus.UNAUTHORIZED, "Yêu cầu xác thực", request);
    }

    // ===== 403 FORBIDDEN =====

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex,
                                                         HttpServletRequest request) {
        String msg = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : "Không có quyền truy cập";
        return buildError(HttpStatus.FORBIDDEN, msg, request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex,
                                                            HttpServletRequest request) {
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    // ===== 404 NOT FOUND =====

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex,
                                                        HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandler(NoHandlerFoundException ex,
                                                         HttpServletRequest request) {
        String msg = "Không tìm thấy endpoint: " + request.getMethod() + " " + request.getRequestURI();
        return buildError(HttpStatus.NOT_FOUND, msg, request);
    }

    // ===== 405 METHOD NOT ALLOWED =====

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                  HttpServletRequest request) {
        String supported = ex.getSupportedHttpMethods() != null
                ? ex.getSupportedHttpMethods().toString()
                : "";
        String msg = "Phương thức " + ex.getMethod() + " không được hỗ trợ" +
                (supported.isEmpty() ? "" : ". Hỗ trợ: " + supported);
        return buildError(HttpStatus.METHOD_NOT_ALLOWED, msg, request);
    }

    // ===== 406 NOT ACCEPTABLE =====

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> handleNotAcceptable(HttpMediaTypeNotAcceptableException ex,
                                                             HttpServletRequest request) {
        return buildError(HttpStatus.NOT_ACCEPTABLE, "Định dạng phản hồi không được chấp nhận", request);
    }

    // ===== 415 UNSUPPORTED MEDIA TYPE =====

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex,
                                                                    HttpServletRequest request) {
        String supported = ex.getSupportedMediaTypes().toString();
        String msg = "Content-Type không được hỗ trợ" + (supported.isEmpty() ? "" : ". Hỗ trợ: " + supported);
        return buildError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, msg, request);
    }

    // ===== 409 CONFLICT =====

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConflict(DataIntegrityViolationException ex,
                                                        HttpServletRequest request) {
        String base = "Dữ liệu bị trùng hoặc vi phạm ràng buộc";
        String cause = rootMessage(ex);
        String msg = base + (cause != null && !cause.isBlank() ? (": " + cause) : "");
        return buildError(HttpStatus.CONFLICT, msg, request);
    }

    // ===== 500 INTERNAL SERVER ERROR =====

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex,
                                                     HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/v3/api-docs")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        log.error("Unhandled exception", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống", request);
    }
}