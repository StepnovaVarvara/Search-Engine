package searchengine.controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import searchengine.dto.indexPage.ErrorIndexingPageResponse;
import searchengine.dto.indexing.ErrorIndexingResponse;
import searchengine.dto.search.ErrorSearchResponse;
import searchengine.exceptions.IndexingException;
import searchengine.exceptions.IndexingPageException;
import searchengine.exceptions.SearchException;

@ControllerAdvice
public class AdviceController extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {

status.value() // return ERORR CODE 400 401 500
        status.is4xxClientError()
                status.is5xxServerError()

        return switch (status.value()) {
            case 500 -> new ResponseEntity<>(
                    new ErrorIndexingResponse()
                            .setError("ex.getError()")
                            .setResult(false), HttpStatus.INTERNAL_SERVER_ERROR);
            case 400 -> new ResponseEntity<>(
                    new ErrorIndexingResponse()
                            .setError("ex.getError()")
                            .setResult(false), HttpStatus.INTERNAL_SERVER_ERROR);
            default -> new ResponseEntity<>(
                    new ErrorIndexingResponse()
                            .setError("ex.getError()")
                            .setResult(false), HttpStatus.valueOf(status.value()));
        };

    }



    @ExceptionHandler(IndexingException.class)
    public ResponseEntity<Object> handleConflict(IndexingException ex) {
        return new ResponseEntity<>(new ErrorIndexingResponse()
                .setError(ex.getError())
                .setResult(false), HttpStatus.OK);
    }

    @ExceptionHandler(IndexingPageException.class)
    public ResponseEntity<Object> handleConflict(IndexingPageException ex) {
        return new ResponseEntity<>(new ErrorIndexingPageResponse()
                .setError(ex.getError())
                .setResult(false), HttpStatus.OK);
    }

    @ExceptionHandler(SearchException.class)
    public ResponseEntity<Object> handleConflict(SearchException ex) {
        return new ResponseEntity<>(new ErrorSearchResponse()
                .setError(ex.getError())
                .setResult(false), HttpStatus.OK);
    }
}
