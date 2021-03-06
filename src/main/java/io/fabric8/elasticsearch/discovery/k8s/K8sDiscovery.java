package io.fabric8.elasticsearch.discovery.k8s;

import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.node.DiscoveryNodeService;
import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.discovery.DiscoverySettings;
import org.elasticsearch.discovery.zen.ZenDiscovery;
import org.elasticsearch.discovery.zen.elect.ElectMasterService;
import org.elasticsearch.discovery.zen.ping.ZenPing;
import org.elasticsearch.discovery.zen.ping.ZenPingService;
import org.elasticsearch.discovery.zen.ping.unicast.UnicastZenPing;
import org.elasticsearch.node.settings.NodeSettingsService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

/**
 *
 */
public class K8sDiscovery extends ZenDiscovery {

  @Inject
  public K8sDiscovery(Settings settings, ClusterName clusterName, ThreadPool threadPool, TransportService transportService,
                      ClusterService clusterService, NodeSettingsService nodeSettingsService, ZenPingService pingService,
                      DiscoveryNodeService discoveryNodeService,
                      NetworkService networkService, DiscoverySettings discoverySettings,
                      ElectMasterService electMasterService) {
    super(settings, clusterName, threadPool, transportService, clusterService, nodeSettingsService,
        discoveryNodeService, pingService, electMasterService, discoverySettings);
    if (settings.getAsBoolean("cloud.enabled", true)) {
      ImmutableList<? extends ZenPing> zenPings = pingService.zenPings();
      UnicastZenPing unicastZenPing = null;
      for (ZenPing zenPing : zenPings) {
        if (zenPing instanceof UnicastZenPing) {
          unicastZenPing = (UnicastZenPing) zenPing;
          break;
        }
      }

      if (unicastZenPing != null) {
        // update the unicast zen ping to add cloud hosts provider
        // and, while we are at it, use only it and not the multicast for example
        unicastZenPing.addHostsProvider(new K8sUnicastHostsProvider(settings, transportService, networkService));
        pingService.zenPings(ImmutableList.of(unicastZenPing));
      } else {
        logger.warn("failed to apply gce unicast discovery, no unicast ping found");
      }
    }
  }
}