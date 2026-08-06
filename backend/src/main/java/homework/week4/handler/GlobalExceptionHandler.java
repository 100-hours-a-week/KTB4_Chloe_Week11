package homework.week4.handler;

import homework.week4.exception.*;
import homework.week4.response.ValidErrorResponse;
import homework.week4.response.ErrorResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidErrorResponse> methodArgumentNoVaildException (MethodArgumentNotValidException e){

        List<String> messages = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ValidErrorResponse.of(messages));
    }


    //리소스 검사 예외 처리
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> notFoundException(
            NotFoundException exception) {

        return ResponseEntity
                .status(exception.getStatus())
                .body(ErrorResponse.of(exception.getMessage()));
    }

    //인증 검사 예외처리
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> unauthorizedException(
            UnauthorizedException exception) {

        return ResponseEntity
                .status(exception.getStatus())
                .body(ErrorResponse.of(exception.getMessage()));
    }

    //인가 검사 예외처리
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> forbiddenException(
            ForbiddenException exception) {

        return ResponseEntity
                .status(exception.getStatus())
                .body(ErrorResponse.of(exception.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accessDeniedException(
            AccessDeniedException exception) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of("권한이 없는 작업입니다"));
    }


    //깨진 JSON 요청 예외 처리
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> httpMessageNotReadableException(
            HttpMessageNotReadableException exception) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(exception.getMessage()));
    }

    //중복 예외 처리
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> duplicateResourceException(
            DuplicateResourceException exception) {

        return ResponseEntity
                .status(exception.getStatus())
                .body(ErrorResponse.of(
                        exception.getMessage(),
                        exception.getField()
                ));
    }

    //예상하지 못한 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> exception(
            Exception exception) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(exception.getMessage()));
    }

    //요청 횟수 초과 예외 처리
    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> tooManyRequestsException(
            TooManyRequestsException exception) {

        return ResponseEntity
                .status(exception.getStatus())
                .body(ErrorResponse.of(exception.getMessage()));
    }

    //잘못된 요청 파라미터 예외 처리 (cursor, limit 등)
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> invalidRequestException(
            InvalidRequestException exception) {

        return ResponseEntity
                .status(exception.getStatus())
                .body(ErrorResponse.of(exception.getMessage()));
    }

}
