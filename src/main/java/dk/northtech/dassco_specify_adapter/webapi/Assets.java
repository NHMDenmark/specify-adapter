package dk.northtech.dassco_specify_adapter.webapi;

import dk.northtech.dassco_specify_adapter.domain.Acknowledge;
import dk.northtech.dassco_specify_adapter.domain.AcknowledgeStatus;
import dk.northtech.dassco_specify_adapter.domain.Asset;
import dk.northtech.dassco_specify_adapter.services.AssetService;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Path("assets")
public class Assets {
    private AssetService assetService;

    @Inject
    public Assets(AssetService assetService) {
        this.assetService = assetService;
    }

    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Acknowledge syncAssets(Asset[] assets){
        List<String> guids = Arrays.stream(assets).map(asset -> asset.asset_guid).toList();
        System.out.println(assets);
        System.out.println(guids);
        this.assetService.convertAssetToAttachment();
        return new Acknowledge(guids, AcknowledgeStatus.SUCCESS, "body", Instant.now());
    }
}
