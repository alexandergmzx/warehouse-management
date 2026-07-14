package com.alexandergomez.wms.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alexandergomez.wms.api.ProblemCode;
import com.alexandergomez.wms.api.ProblemException;
import com.alexandergomez.wms.catalog.Article;
import com.alexandergomez.wms.catalog.ArticleRepository;
import com.alexandergomez.wms.catalog.Location;
import com.alexandergomez.wms.catalog.LocationRepository;

/** Master-data creation for articles and locations (API.md). */
@Service
public class CatalogAdminService {

    private final ArticleRepository articles;
    private final LocationRepository locations;

    public CatalogAdminService(ArticleRepository articles, LocationRepository locations) {
        this.articles = articles;
        this.locations = locations;
    }

    @Transactional
    public CreateArticleResponse createArticle(CreateArticleRequest request) {
        if (articles.findBySku(request.sku()).isPresent()) {
            throw new ProblemException(ProblemCode.ARTICLE_ALREADY_EXISTS, "An article with this SKU already exists.");
        }
        Article article = articles.save(Article.create(request.sku(), request.description()));
        return new CreateArticleResponse(
                article.getId(), article.getSku(), article.getDescription(), article.getQrValue(), article.isActive());
    }

    @Transactional
    public CreateLocationResponse createLocation(CreateLocationRequest request) {
        if (locations.findByCode(request.code()).isPresent()) {
            throw new ProblemException(ProblemCode.LOCATION_ALREADY_EXISTS,
                    "A location with this code already exists.");
        }
        if (locations.findByPickSequence(request.pickSequence()).isPresent()) {
            throw new ProblemException(ProblemCode.PICK_SEQUENCE_ALREADY_EXISTS,
                    "This pick sequence is already assigned to another location.");
        }
        Location location = locations.save(Location.create(request.code(), request.pickSequence()));
        return new CreateLocationResponse(
                location.getId(), location.getCode(), location.getQrValue(),
                location.getPickSequence(), location.isActive());
    }
}
