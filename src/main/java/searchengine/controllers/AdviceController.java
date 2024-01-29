package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import searchengine.dto.indexing.ErrorIndexingPageResponse;
import searchengine.dto.indexing.ErrorIndexingResponse;
import searchengine.exceptions.IndexingException;
import searchengine.exceptions.IndexingPageException;

@ControllerAdvice
public class AdviceController extends ResponseEntityExceptionHandler {

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
}
