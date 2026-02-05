package com.example.sejong_voice.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record DepartmentRequest (@NotNull String department){}
