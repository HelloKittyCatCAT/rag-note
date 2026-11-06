package com.sya.ragnote.exception;

import com.sya.ragnote.enums.result.ResultCode;

public class ServiceSystemException extends RuntimeException {
    private final ResultCode resultCode;

    public ServiceSystemException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.resultCode = resultCode;
    }

    public ServiceSystemException(ResultCode resultCode, Throwable cause) {
        super(resultCode.getMsg(), cause);
        this.resultCode = resultCode;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }
}
