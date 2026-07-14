package com.alexandergomez.wms.label;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.awt.image.BufferedImage;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import com.alexandergomez.wms.api.ProblemCode;
import com.alexandergomez.wms.api.ProblemException;
import com.alexandergomez.wms.catalog.Article;
import com.alexandergomez.wms.catalog.ArticleRepository;
import com.alexandergomez.wms.catalog.Location;
import com.alexandergomez.wms.catalog.LocationRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * Renders the location/article QR payloads fixed by ADR 0007 as printable PNG
 * and single-label A4 PDF sheets. Both outputs are deterministic: the PNG
 * carries no embedded metadata, and the PDF's info-dictionary dates and
 * trailer document ID (the two fields the reproducible-builds ecosystem
 * otherwise pins via {@code SOURCE_DATE_EPOCH}) are fixed rather than
 * generated from wall-clock time.
 */
@Service
public class LabelService {

    private static final int IMAGE_SIZE = 300;
    private static final int QUIET_ZONE_MODULES = 4;
    private static final String FONT_RESOURCE = "/fonts/liberation-sans/LiberationSans-Regular.ttf";
    private static final long FIXED_DOCUMENT_ID_SEED = 0L;
    private static final float QR_SIZE_POINTS = 200f;
    private static final float TOP_MARGIN_POINTS = 150f;
    private static final float TEXT_GAP_POINTS = 30f;
    private static final float FONT_SIZE_POINTS = 18f;

    private static final byte[] FONT_BYTES = loadFontBytes();

    private final LocationRepository locations;
    private final ArticleRepository articles;

    public LabelService(LocationRepository locations, ArticleRepository articles) {
        this.locations = locations;
        this.articles = articles;
    }

    public LabelPayload locationLabel(String code) {
        Location location = locations.findByCode(code)
                .orElseThrow(() -> new ProblemException(ProblemCode.LOCATION_NOT_FOUND,
                        "No location with this code exists."));
        return new LabelPayload(location.getQrValue(), "Location " + location.getCode());
    }

    public LabelPayload articleLabel(String sku) {
        Article article = articles.findBySku(sku)
                .orElseThrow(() -> new ProblemException(ProblemCode.ARTICLE_NOT_FOUND,
                        "No article with this SKU exists."));
        return new LabelPayload(article.getQrValue(), "Article " + article.getSku());
    }

    /** 300x300 PNG, error correction M, four-module quiet zone, black-on-white (ADR 0007). */
    public byte[] png(String qrValue) {
        try {
            BufferedImage image = MatrixToImageWriter.toBufferedImage(encode(qrValue));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Failed to render QR PNG", e);
        }
    }

    /** Single-label A4 PDF: the QR code plus embedded-font human-readable text (ADR 0007). */
    public byte[] pdf(String qrValue, String humanReadableText) {
        try (PDDocument document = new PDDocument()) {
            document.setDocumentId(FIXED_DOCUMENT_ID_SEED);
            document.setDocumentInformation(fixedDocumentInformation());

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType0Font font;
            try (InputStream fontStream = new ByteArrayInputStream(FONT_BYTES)) {
                font = PDType0Font.load(document, fontStream, true);
            }

            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(encode(qrValue));
            PDImageXObject qrObject = LosslessFactory.createFromImage(document, qrImage);

            float pageWidth = PDRectangle.A4.getWidth();
            float pageHeight = PDRectangle.A4.getHeight();
            float qrX = (pageWidth - QR_SIZE_POINTS) / 2f;
            float qrY = pageHeight - TOP_MARGIN_POINTS - QR_SIZE_POINTS;
            float textWidth = font.getStringWidth(humanReadableText) / 1000f * FONT_SIZE_POINTS;
            float textX = (pageWidth - textWidth) / 2f;
            float textY = qrY - TEXT_GAP_POINTS;

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(qrObject, qrX, qrY, QR_SIZE_POINTS, QR_SIZE_POINTS);
                contentStream.beginText();
                contentStream.setFont(font, FONT_SIZE_POINTS);
                contentStream.newLineAtOffset(textX, textY);
                contentStream.showText(humanReadableText);
                contentStream.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException | WriterException e) {
            throw new IllegalStateException("Failed to render QR label PDF", e);
        }
    }

    private static BitMatrix encode(String qrValue) throws WriterException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, QUIET_ZONE_MODULES);
        return new QRCodeWriter().encode(qrValue, BarcodeFormat.QR_CODE, IMAGE_SIZE, IMAGE_SIZE, hints);
    }

    private static PDDocumentInformation fixedDocumentInformation() {
        PDDocumentInformation info = new PDDocumentInformation();
        Calendar fixed = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT);
        fixed.clear();
        fixed.set(2026, Calendar.JANUARY, 1, 0, 0, 0);
        info.setCreationDate(fixed);
        info.setModificationDate(fixed);
        info.setProducer("Miniature WMS label generator");
        return info;
    }

    private static byte[] loadFontBytes() {
        try (InputStream in = LabelService.class.getResourceAsStream(FONT_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Embedded font resource not found: " + FONT_RESOURCE);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load embedded font", e);
        }
    }
}
