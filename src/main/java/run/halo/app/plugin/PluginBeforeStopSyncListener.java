package run.halo.app.plugin;

import org.springframework.context.event.EventListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.plugin.event.HaloPluginBeforeStopEvent;

/**
 * Synchronization listener executed by the plugin before it is stopped.
 *
 * @author guqing
 * @since 2.0.0
 */
@Component
public class PluginBeforeStopSyncListener {

    private final ReactiveExtensionClient client;

    public PluginBeforeStopSyncListener(ReactiveExtensionClient client) {
        this.client = client;
    }

    @EventListener
    public Mono<Void> onApplicationEvent(@NonNull HaloPluginBeforeStopEvent event) {
        var pluginWrapper = event.getPlugin();
        var pluginContext = ExtensionContextRegistry.getInstance()
            .getByPluginId(pluginWrapper.getPluginId());

        return cleanUpPluginExtensionResources(pluginContext);
    }

    private Mono<Void> cleanUpPluginExtensionResources(PluginApplicationContext context) {
        var gvkExtensionNames = context.extensionNamesMapping();
        return Flux.fromIterable(gvkExtensionNames.entrySet())
            .flatMap(entry -> Flux.fromIterable(entry.getValue())
                .flatMap(extensionName -> client.fetch(entry.getKey(), extensionName))
                .flatMap(client::delete))
            .then();
    }
}
