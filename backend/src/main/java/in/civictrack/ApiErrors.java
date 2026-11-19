package in.civictrack;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.*;
import org.springframework.dao.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.util.Map;
@RestControllerAdvice
class ApiErrors {
 @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"Request denied":e.getReason()));}
 @ExceptionHandler({OptimisticLockingFailureException.class,DataIntegrityViolationException.class}) ResponseEntity<?> conflict(Exception e){return ResponseEntity.status(409).body(Map.of("message","A concurrent update or duplicate request was detected. Refresh and retry with the same request key."));}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException e){return ResponseEntity.badRequest().body(Map.of("message",e.getBindingResult().getFieldErrors().stream().map(f->f.getField()+": "+f.getDefaultMessage()).findFirst().orElse("Invalid input")));}
 @ExceptionHandler(MaxUploadSizeExceededException.class) ResponseEntity<?> upload(Exception e){return ResponseEntity.status(413).body(Map.of("message","Choose an image smaller than 3 MB"));}
}
