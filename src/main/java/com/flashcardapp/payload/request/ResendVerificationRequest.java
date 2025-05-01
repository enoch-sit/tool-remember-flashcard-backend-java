package com.flashcardapp.payload.request;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class ResendVerificationRequest {
    @NotBlank
    @Email
    private String email;
}