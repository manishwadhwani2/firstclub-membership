package com.firstclub.membership.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MembershipException extends RuntimeException {
    public MembershipException(String message) {
        super(message);
    }
}
