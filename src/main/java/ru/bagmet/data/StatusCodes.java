package ru.bagmet.data;

public enum StatusCodes {
    OK(200),
    CREATED(201),
    BAD_REQUEST(400),
    NOT_FOUND(404),
    CONFLICT(409);

    private final int code;

    StatusCodes(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
