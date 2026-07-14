package com.alexandergomez.wms.label;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Printable QR label endpoints for locations and articles (ADR 0007, admin-only). */
@RestController
@RequestMapping("/api/v1/admin/labels")
public class LabelAdminController {

    private static final MediaType APPLICATION_PDF = MediaType.valueOf("application/pdf");

    private final LabelService labelService;

    public LabelAdminController(LabelService labelService) {
        this.labelService = labelService;
    }

    @GetMapping(value = "/locations/{code}/png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> locationPng(@PathVariable String code) {
        LabelPayload label = labelService.locationLabel(code);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(labelService.png(label.qrValue()));
    }

    @GetMapping(value = "/locations/{code}/pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> locationPdf(@PathVariable String code) {
        LabelPayload label = labelService.locationLabel(code);
        return ResponseEntity.ok().contentType(APPLICATION_PDF)
                .body(labelService.pdf(label.qrValue(), label.humanReadableText()));
    }

    @GetMapping(value = "/articles/{sku}/png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> articlePng(@PathVariable String sku) {
        LabelPayload label = labelService.articleLabel(sku);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(labelService.png(label.qrValue()));
    }

    @GetMapping(value = "/articles/{sku}/pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> articlePdf(@PathVariable String sku) {
        LabelPayload label = labelService.articleLabel(sku);
        return ResponseEntity.ok().contentType(APPLICATION_PDF)
                .body(labelService.pdf(label.qrValue(), label.humanReadableText()));
    }
}
