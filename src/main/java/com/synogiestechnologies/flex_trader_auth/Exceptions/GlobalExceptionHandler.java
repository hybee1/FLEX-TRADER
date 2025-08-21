package com.synogiestechnologies.flex_trader_auth.Exceptions;


import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.HashMap;
import java.util.Map;


@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value={TokenNotFoundException.class})
    public ResponseEntity<Object> handleTokenNotFoundException(
            TokenNotFoundException tokenNotFoundException) {

        Map<String, String> errorMessageObj = Map.of(
                "errorMessage", tokenNotFoundException.getMessage());
        Map<String, Object> error = Map.of(
                "error", errorMessageObj);

        MyUsersException userException = new MyUsersException(
                error, tokenNotFoundException.getCause()  );

        return new ResponseEntity<>(userException, tokenNotFoundException.httpStatus );

    }

    @ExceptionHandler(value={UserHasActiveTokenException.class})
    public ResponseEntity<Object> handleUserHasActiveTokenException(
            UserHasActiveTokenException userHasActiveTokenException) {

        Map<String, String> errorMessageObj = Map.of(
                "errorMessage", userHasActiveTokenException.getMessage());
        Map<String, Object> error = Map.of(
                "error", errorMessageObj);

        MyUsersException userException = new MyUsersException(
                error, userHasActiveTokenException.getCause()  );

        return new ResponseEntity<>(userException, userHasActiveTokenException.httpStatus );

    }

    @ExceptionHandler(value={SaveToDBException.class})
    public ResponseEntity<Object> handleSaveToDBException(
            SaveToDBException saveToDBException) {

        Map<String, String> errorMessageObj = Map.of(
                "errorMessage", saveToDBException.getMessage());
        Map<String, Object> error = Map.of(
                "error", errorMessageObj);

        MyUsersException userException = new MyUsersException(
                error, saveToDBException.getCause()  );

        return new ResponseEntity<>(userException, saveToDBException.httpStatus );

    }

    @ExceptionHandler(value={SigningKeyException.class})
    public ResponseEntity<Object> handleSigningKeyException(
            SigningKeyException signingKeyException) {

        Map<String, String> errorMessageObj = Map.of(
                "errorMessage", signingKeyException.getMessage());
        Map<String, Object> error = Map.of(
                "error", errorMessageObj);

        MyUsersException userException = new MyUsersException(
                error, signingKeyException.getCause()  );

        return new ResponseEntity<>(userException, signingKeyException.httpStatus );

    }

    @ExceptionHandler(value={NoUserFoundException.class})
    public ResponseEntity<Object> handleNoUserFoundException(
            NoUserFoundException noUserFoundException) {

        Map<String, String> errorMessageObj = Map.of(
                "errorMessage", noUserFoundException.getMessage());
        Map<String, Object> error = Map.of(
                "error", errorMessageObj);

        MyUsersException userException = new MyUsersException(
                error, noUserFoundException.getCause()  );

        return new ResponseEntity<>(userException, noUserFoundException.httpStatus );

    }

    @ExceptionHandler(value={InvalidCredentialsException.class})
    public ResponseEntity<Object> handleInvalidCredentialsException(
            InvalidCredentialsException invalidCredentialsException) {

        Map<String, String> errorMessageObj = Map.of(
                "errorMessage", invalidCredentialsException.getMessage());

        Map<String, Object> error = Map.of(
                "error", errorMessageObj);

        MyUsersException userException = new MyUsersException(
                error, invalidCredentialsException.getCause()  );

        return new ResponseEntity<>(userException, invalidCredentialsException.httpStatus );

    }

    @ExceptionHandler(value={UserCreationException.class})
    public ResponseEntity<Object> handleUserCreationException(
            UserCreationException userCreationException) {

        Map<String, String> errorMessageObj = Map.of(
                "errorMessage", userCreationException.getMessage());
        Map<String, Object> error = Map.of(
                "error", errorMessageObj);
        MyUsersException userException = new MyUsersException(
                error, userCreationException.getCause()  );

        return new ResponseEntity<>(userException, userCreationException.httpStatus);

    }

    @ExceptionHandler(value={SubscriptionException.class})
    public ResponseEntity<Object> handleSubscriptionException(
            SubscriptionException subscriptionException) {

        Map<String, String> errorMessageObj = Map.of(
                "errorMessage", subscriptionException.getMessage());

        Map<String, Object> error = Map.of(
                "error", errorMessageObj);

        MyUsersException userException = new MyUsersException(
                error, subscriptionException.getCause()  );

        return new ResponseEntity<>(userException, subscriptionException.httpStatus );

    }

    @ExceptionHandler(value={LoginException.class})
    public ResponseEntity<Object> handleLoginException(
            LoginException loginException) {

        Map<String, String> errorMessageObj = Map.of(
                "errorMessage", loginException.getMessage());

        Map<String, Object> error = Map.of(
                "error", errorMessageObj);

        MyUsersException userException = new MyUsersException(
                error, loginException.getCause()  );

        return new ResponseEntity<>(userException, loginException.httpStatus );

    }

    @ExceptionHandler(value={ExpiredTokenException.class})
    public ResponseEntity<Object> handleExpiredTokenException(
            ExpiredTokenException expiredOrInvalidTokenException) {

        Map<String, String> errorMessageObj = Map.of(
                "errorMessage", expiredOrInvalidTokenException.getMessage());

        Map<String, Object> error = Map.of(
                "error", errorMessageObj);

        MyUsersException userException = new MyUsersException(
                error, expiredOrInvalidTokenException.getCause()  );

        return new ResponseEntity<>(userException, expiredOrInvalidTokenException.httpStatus );

    }

    @ExceptionHandler(value={InvalidTokenException.class})
    public ResponseEntity<Object> handleInvalidTokenException(
            InvalidTokenException invalidTokenException) {

        Map<String, String> errorMessageObj = Map.of(
                "errorMessage", invalidTokenException.getMessage());

        Map<String, Object> error = Map.of(
                "error", errorMessageObj);

        MyUsersException userException = new MyUsersException(
                error, invalidTokenException.getCause()  );

        return new ResponseEntity<>(userException, invalidTokenException.httpStatus );

    }

    @ExceptionHandler(value={UserAlreadyExistException.class})
    public ResponseEntity<Object> handleUserAlreadyExistException(
            UserAlreadyExistException userAlreadyExistException) {

        Map<String, String> errorMessageObj = Map.of(
                "errorMessage", userAlreadyExistException.getMessage());

        Map<String, Object> error = Map.of(
                "error", errorMessageObj);

        MyUsersException userException = new MyUsersException(
                error, userAlreadyExistException.getCause()  );

        return new ResponseEntity<>(userException, userAlreadyExistException.httpStatus );

    }

    @ExceptionHandler(value={MethodArgumentNotValidException.class})
    public ResponseEntity<Object> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException validationEx) {

        Map<String, String> errors  = new HashMap<>();

        validationEx.getBindingResult().getAllErrors().forEach(
                error-> {
                    String fieldName = ((FieldError) error).getField();
                    String fieldValue = error.getDefaultMessage();
                    errors .put(fieldName, fieldValue);
                });

        Map<String, Object> error = Map.of( "error", errors);

        return new ResponseEntity<>(error , HttpStatus.BAD_REQUEST);

    }

    @ExceptionHandler(value = {IllegalArgumentException.class})
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = {ExpiredJwtException.class})
    public ResponseEntity<Object> handleExpiredJwtException(ExpiredJwtException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "JWT token has expired");
        error.put("details", ex.getMessage());

        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED); // Or HttpStatus.FORBIDDEN
    }

    @ExceptionHandler(value = {SecurityException.class})
    public ResponseEntity<Object> handleSecurityException(SecurityException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Security violation detected");
        error.put("details", ex.getMessage());

        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

}


