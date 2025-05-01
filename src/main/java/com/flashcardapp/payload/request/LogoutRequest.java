package com.flashcardapp.payload.request;

import lombok.Data;

@Data
public class LogoutRequest {
    private String refreshToken;
}