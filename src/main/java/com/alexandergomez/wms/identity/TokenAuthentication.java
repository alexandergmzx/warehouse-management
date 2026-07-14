package com.alexandergomez.wms.identity;

/**
 * Result of validating a bearer token: either an authenticated principal or a
 * {@link TokenFailure}. Exactly one is non-null.
 */
public record TokenAuthentication(AuthenticatedUser user, TokenFailure failure) {

    public static TokenAuthentication success(AuthenticatedUser user) {
        return new TokenAuthentication(user, null);
    }

    public static TokenAuthentication failure(TokenFailure failure) {
        return new TokenAuthentication(null, failure);
    }

    public boolean isAuthenticated() {
        return user != null;
    }
}
