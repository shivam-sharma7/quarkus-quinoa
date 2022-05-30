package io.quarkiverse.quinoa;

import static io.quarkus.vertx.http.runtime.VertxHttpRecorder.DEFAULT_ROUTE_ORDER;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class QuinoaRecorder {
    private static final Logger LOG = Logger.getLogger(QuinoaRecorder.class);
    public static final String META_INF_WEB_UI = "META-INF/webui";
    public static final int QUINOA_ROUTE_ORDER = 1100;
    public static final int QUINOA_SPA_ROUTE_ORDER = DEFAULT_ROUTE_ORDER + 10_100;
    public static final Set<HttpMethod> HANDLED_METHODS = Set.of(HttpMethod.HEAD, HttpMethod.OPTIONS, HttpMethod.GET);

    public Handler<RoutingContext> quinoaProxyDevHandler(Supplier<Vertx> vertx, int port,
            final List<String> ignoredPathPrefixes) {
        logIgnoredPathPrefixes(ignoredPathPrefixes);
        return new QuinoaDevProxyHandler(vertx.get(), port, ignoredPathPrefixes);
    }

    public Handler<RoutingContext> quinoaSPARoutingHandler(final List<String> ignoredPathPrefixes) throws IOException {
        return new QuinoaSPARoutingHandler(ignoredPathPrefixes);
    }

    public Handler<RoutingContext> quinoaHandler(final String directory, final Set<String> uiResources,
            final List<String> ignoredPathPrefixes) throws IOException {
        logIgnoredPathPrefixes(ignoredPathPrefixes);
        return new QuinoaUIResourceHandler(directory, uiResources, ignoredPathPrefixes);
    }

    static String resolvePath(RoutingContext ctx) {
        return (ctx.mountPoint() == null) ? ctx.normalizedPath()
                : ctx.normalizedPath().substring(
                        // let's be extra careful here in case Vert.x normalizes the mount points at some point
                        ctx.mountPoint().endsWith("/") ? ctx.mountPoint().length() - 1 : ctx.mountPoint().length());
    }

    static boolean isIgnored(final String path, final List<String> ignoredPathPrefixes) {
        if (ignoredPathPrefixes.stream().anyMatch(path::startsWith)) {
            if (LOG.isDebugEnabled()) {
                LOG.debugf("Quinoa is ignoring path (quarkus.quinoa.ignored-path-prefixes): " + path);
            }
            return true;
        }
        return false;
    }

    static void logIgnoredPathPrefixes(final List<String> ignoredPathPrefixes) {
        if (LOG.isDebugEnabled()) {
            LOG.debugf("Quinoa is ignoring paths starting with: " + String.join(", ", ignoredPathPrefixes));
        }
    }

    static boolean shouldHandleMethod(RoutingContext ctx) {
        return HANDLED_METHODS.contains(ctx.request().method());
    }

    static void next(ClassLoader cl, RoutingContext ctx) {
        // make sure we don't lose the correct TCCL to Vert.x...
        Thread.currentThread().setContextClassLoader(cl);
        ctx.next();
    }

}
