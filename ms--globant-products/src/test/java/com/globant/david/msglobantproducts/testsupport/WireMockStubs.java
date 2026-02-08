package com.globant.david.msglobantproducts.testsupport;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Utility class for WireMock stub configurations in integration tests.
 * <p>
 * Provides methods to configure common WireMock stubs for the external product API.
 * This class should be used by tests extending {@link WireMockIntegrationTest}
 * to configure mock responses for external API calls.
 */
public class WireMockStubs {

    private final WireMockServer wireMockServer;

    public WireMockStubs(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
    }

    /**
     * Stubs a successful response for the similar IDs endpoint.
     *
     * @param productId  the product ID
     * @param similarIds the list of similar product IDs to return
     */
    public void stubSimilarIds(String productId, String... similarIds) {
        wireMockServer.stubFor(get(urlEqualTo("/product/" + productId + "/similarids"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(similarIdsJson(similarIds))));
    }

    /**
     * Stubs a 404 response for the similar IDs endpoint.
     *
     * @param productId the product ID
     */
    public void stubSimilarIdsNotFound(String productId) {
        wireMockServer.stubFor(get(urlEqualTo("/product/" + productId + "/similarids"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("{\"error\": \"Product not found\"}")));
    }

    /**
     * Stubs an error response for the similar IDs endpoint.
     *
     * @param productId the product ID
     * @param status    the HTTP status code to return
     */
    public void stubSimilarIdsError(String productId, int status) {
        wireMockServer.stubFor(get(urlEqualTo("/product/" + productId + "/similarids"))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withBody("{\"error\": \"Service error\"}")));
    }

    /**
     * Stubs a successful response for the product details endpoint.
     *
     * @param productId    the product ID
     * @param name         the product name
     * @param price        the product price
     * @param availability the product availability
     */
    public void stubProduct(String productId, String name, double price, boolean availability) {
        wireMockServer.stubFor(get(urlEqualTo("/product/" + productId))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(productJson(productId, name, price, availability))));
    }

    /**
     * Stubs a 404 response for the product details endpoint.
     *
     * @param productId the product ID
     */
    public void stubProductNotFound(String productId) {
        wireMockServer.stubFor(get(urlEqualTo("/product/" + productId))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("{\"error\": \"Product not found\"}")));
    }

    /**
     * Stubs an error response for the product details endpoint.
     *
     * @param productId the product ID
     * @param status    the HTTP status code to return
     */
    public void stubProductError(String productId, int status) {
        wireMockServer.stubFor(get(urlEqualTo("/product/" + productId))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withBody("{\"error\": \"Service error\"}")));
    }

    /**
     * Verifies that the similar IDs endpoint was called exactly once for the given product.
     *
     * @param productId the product ID
     */
    public void verifySimilarIdsCalled(String productId) {
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/product/" + productId + "/similarids")));
    }

    /**
     * Verifies that the product details endpoint was called exactly once for the given product.
     *
     * @param productId the product ID
     */
    public void verifyProductCalled(String productId) {
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/product/" + productId)));
    }

    /**
     * Verifies that the similar IDs endpoint was called a specific number of times.
     *
     * @param productId the product ID
     * @param count     the expected number of calls
     */
    public void verifySimilarIdsCalled(String productId, int count) {
        wireMockServer.verify(count, getRequestedFor(urlEqualTo("/product/" + productId + "/similarids")));
    }

    /**
     * Verifies that the product details endpoint was called a specific number of times.
     *
     * @param productId the product ID
     * @param count     the expected number of calls
     */
    public void verifyProductCalled(String productId, int count) {
        wireMockServer.verify(count, getRequestedFor(urlEqualTo("/product/" + productId)));
    }

    private String similarIdsJson(String... ids) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(ids[i]).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private String productJson(String id, String name, double price, boolean availability) {
        return String.format(
                java.util.Locale.US,
                "{\"id\":\"%s\",\"name\":\"%s\",\"price\":%.2f,\"availability\":%s}",
                id, name, price, availability ? "true" : "false"
        );
    }
}
