package commitcapstone.commit.common.response;

import lombok.Getter;

@Getter

public class SuccessResponse<T>{
    private final boolean success;
    private final String message;
    private final T data;

    public SuccessResponse(String message, T data) {
        this.success = true;
        this.message = message;
        this.data = data;
    }
}
