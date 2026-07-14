package com.alexandergomez.wms.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateArticleRequest(
        @NotBlank @Pattern(regexp = "^[A-Z0-9_-]{1,50}$") String sku,
        @NotBlank @Size(min = 1, max = 200) String description) {
}
