package com.alexandergomez.wms.label;

/** Resolved label content: the exact scan payload and text printed for a human. */
public record LabelPayload(String qrValue, String humanReadableText) {
}
