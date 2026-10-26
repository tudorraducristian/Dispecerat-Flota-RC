package ro.flota.server.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Toate erorile ies la fel, ca JSON {status, mesaj}, cu codul potrivit. */
@RestControllerAdvice
public class EroriApi {

    public record Eroare(int status, String mesaj) {
    }

    @ExceptionHandler(CerereGresita.class)
    ResponseEntity<Eroare> cerereGresita(CerereGresita e) {
        return raspuns(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(Negasit.class)
    ResponseEntity<Eroare> negasit(Negasit e) {
        return raspuns(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** 401: nu știm cine ești. 403: știm, dar rolul tău nu ajunge. */
    @ExceptionHandler(Neautorizat.class)
    ResponseEntity<Eroare> neautorizat(Neautorizat e) {
        return raspuns(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(Interzis.class)
    ResponseEntity<Eroare> interzis(Interzis e) {
        return raspuns(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(Conflict.class)
    ResponseEntity<Eroare> conflict(Conflict e) {
        return raspuns(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ServiciuIndisponibil.class)
    ResponseEntity<Eroare> indisponibil(ServiciuIndisponibil e) {
        return raspuns(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    static ResponseEntity<Eroare> raspuns(HttpStatus status, String mesaj) {
        return ResponseEntity.status(status).body(new Eroare(status.value(), mesaj));
    }
}
