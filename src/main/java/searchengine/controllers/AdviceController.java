package searchengine.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import searchengine.dto.indexing.ErrorResponse;
import searchengine.exceptions.*;

@Slf4j
@ControllerAdvice
public class AdviceController extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleConflict(Exception ex) {
        return new ResponseEntity<>(new ErrorResponse()
                    .setError("Внутренняя ошибка сервера")
                    .setResult(false), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleConflict(BadRequestException ex) {
        return new ResponseEntity<>(new ErrorResponse()
                .setError(ex.getError())
                .setResult(false), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IndexingException.class)
    public ResponseEntity<Object> handleConflict(IndexingException ex) {
        return new ResponseEntity<>(new ErrorResponse()
                .setError(ex.getError())
                .setResult(false), HttpStatus.OK);
    }

    @ExceptionHandler(IndexingPageException.class)
    public ResponseEntity<Object> handleConflict(IndexingPageException ex) {
        return new ResponseEntity<>(new ErrorResponse()
                .setError(ex.getError())
                .setResult(false), HttpStatus.OK);
    }

    @ExceptionHandler(SearchException.class)
    public ResponseEntity<Object> handleConflict(SearchException ex) {
        return new ResponseEntity<>(new ErrorResponse()
                .setError(ex.getError())
                .setResult(false), HttpStatus.OK);
    }
}
