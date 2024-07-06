package zerobase.projectdividend.exception.impl;

import org.springframework.http.HttpStatus;
import zerobase.projectdividend.exception.AbstractException;

public class AlreadyExistCompanyException extends AbstractException {
    @Override
    public int getStatusCode() {
        return HttpStatus.BAD_REQUEST.value();
    }

    @Override
    public String getMessage() {
        return "이미 정보가 존재하는 회사입니다.";
    }
}
