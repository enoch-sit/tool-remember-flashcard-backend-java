package com.flashcardapp.payload.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class VerifyEmailRequest {
    @NotBlank
    private String token;
}