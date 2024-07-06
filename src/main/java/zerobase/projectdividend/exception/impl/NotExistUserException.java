package zerobase.projectdividend.exception.impl;

import org.springframework.http.HttpStatus;
import zerobase.projectdividend.exception.AbstractException;

public class NotExistUserException extends AbstractException {
    @Override
    public int getStatusCode() {
        return HttpStatus.BAD_REQUEST.value();
    }

    @Override
    public String getMessage() {
        return "가입되지 않은 사용자명입니다.";
    }
}
