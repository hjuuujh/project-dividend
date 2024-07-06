package zerobase.projectdividend.exception.impl;

import org.springframework.http.HttpStatus;
import zerobase.projectdividend.exception.AbstractException;

public class FailToScrapException extends AbstractException {
    @Override
    public int getStatusCode() {
        return HttpStatus.BAD_REQUEST.value();
    }

    @Override
    public String getMessage() {
        return "Scrap을 실패하였습니다.";
    }
}
