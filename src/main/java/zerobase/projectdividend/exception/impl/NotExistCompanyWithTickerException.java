package zerobase.projectdividend.exception.impl;

import org.springframework.http.HttpStatus;
import zerobase.projectdividend.exception.AbstractException;

public class NotExistCompanyWithTickerException extends AbstractException {
    @Override
    public int getStatusCode() {
        return HttpStatus.BAD_REQUEST.value();
    }

    @Override
    public String getMessage() {
        return "Ticker에 해당하는 회사가 없습니다.";
    }
}
