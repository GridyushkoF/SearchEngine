package searchengine.converters;

import searchengine.config.ConfigSite;
import searchengine.model.SiteEntity;
import searchengine.model.SiteStatus;
import searchengine.services.indexing.NodeLink;

import java.time.LocalDateTime;

public class ConfigSiteConverter {
    public static NodeLink getNodeLinkByConfigSite(ConfigSite configSite) {
        return new NodeLink(configSite.getUrl(),configSite.getUrl());
    }
    public static SiteEntity getSiteEntityByConfigSite(ConfigSite configSite) {
        return new SiteEntity(
                SiteStatus.INDEXING,
                LocalDateTime.now(),
                "Ошибок нет",
                configSite.getUrl(),
                configSite.getName());
    }
}
