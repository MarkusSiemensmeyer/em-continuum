package io.continuum.slices.blueprint.translation.reportitemtoassetregistry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** HTTP adapter of the {@link AssetRegistry} port. A non-2xx answer throws, so nothing gets recorded. */
@Component
@ConditionalOnProperty(prefix = "slices.blueprint.translation", name = "reportitemtoassetregistry.enabled")
class RestClientAssetRegistry implements AssetRegistry {

    private final RestClient restClient;

    RestClientAssetRegistry(@Value("${blueprint.asset-registry.url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    @Override
    public void report(AssetRegistryNotice notice) {
        restClient.put()
                .uri("/assets/{assetId}/lifecycle", notice.assetId())
                .body(notice)
                .retrieve()
                .toBodilessEntity();
    }
}
