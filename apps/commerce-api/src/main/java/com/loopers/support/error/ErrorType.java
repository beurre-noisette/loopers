package com.loopers.support.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorType {
    /** 범용 에러 */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "일시적인 오류가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.getReasonPhrase(), "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(), "존재하지 않는 요청입니다."),
    CONFLICT(HttpStatus.CONFLICT, HttpStatus.CONFLICT.getReasonPhrase(), "이미 존재하는 리소스입니다."),
    DUPLICATE_VALUE(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.getReasonPhrase(), "중복된 값입니다."),

    /** User 관련 예외 **/
    INVALID_INPUT_FORMAT(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.getReasonPhrase(), "입력 형식이 올바르지 않습니다."),
    ALREADY_REGISTERED_USER(HttpStatus.CONFLICT, HttpStatus.CONFLICT.getReasonPhrase(), "이미 회원가입이 되어 있습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(), "회원이 존재하지 않습니다."),
    
    /** A에서 B를 빼고자 하는데 A가 충분하지 않은 경우 **/
    NOT_ENOUGH(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.getReasonPhrase(), "충분하지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
