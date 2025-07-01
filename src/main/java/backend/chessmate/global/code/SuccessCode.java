package commitcapstone.commit.common.code;

import org.springframework.http.HttpStatus;

public interface SuccessCode {
    HttpStatus getHttpstatus();
    String getMessage();
}
