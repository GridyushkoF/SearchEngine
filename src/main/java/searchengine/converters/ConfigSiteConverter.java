package searchengine.converters;

import searchengine.config.ConfigSite;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.services.indexing.NodeLink;

import java.time.LocalDateTime;

public class ConfigSiteConverter {
    public static NodeLink getNodeLinkByConfigSite(ConfigSite configSite) {
        return new NodeLink(configSite.getUrl(),configSite.getUrl());
    }
    public static Site getSiteEntityByConfigSite(ConfigSite configSite) {
        return new Site(
                SiteStatus.INDEXING,
                LocalDateTime.now(),
                "Ошибок нет",
                configSite.getUrl(),
                configSite.getName());
    }
}
