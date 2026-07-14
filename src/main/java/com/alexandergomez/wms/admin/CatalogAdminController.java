package com.alexandergomez.wms.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
public class CatalogAdminController {

    private final CatalogAdminService catalogAdminService;

    public CatalogAdminController(CatalogAdminService catalogAdminService) {
        this.catalogAdminService = catalogAdminService;
    }

    @PostMapping("/articles")
    public ResponseEntity<CreateArticleResponse> createArticle(@Valid @RequestBody CreateArticleRequest request) {
        return ResponseEntity.status(201).body(catalogAdminService.createArticle(request));
    }

    @PostMapping("/locations")
    public ResponseEntity<CreateLocationResponse> createLocation(@Valid @RequestBody CreateLocationRequest request) {
        return ResponseEntity.status(201).body(catalogAdminService.createLocation(request));
    }
}
