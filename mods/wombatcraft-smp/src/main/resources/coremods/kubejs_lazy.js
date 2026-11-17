// KubeJS 2001.6.5-build.26: get/forget race can return null while parsing generated resources.
// Lock each Lazy instance, not the global reload pipeline. Preserve factories and expiry semantics.
function initializeCoreMod() {
    return { wombatcraft_kubejs_lazy: {
        target: { type: 'CLASS', name: 'dev.latvian.mods.kubejs.util.Lazy' },
        transformer: function(node) {
            var changed = 0;
            for (var i = 0; i < node.methods.size(); i++) {
                var method = node.methods.get(i);
                if ((method.name === 'get' && method.desc === '()Ljava/lang/Object;') ||
                    (method.name === 'forget' && method.desc === '()V')) {
                    method.access = method.access | 32; // ACC_SYNCHRONIZED
                    changed++;
                }
            }
            if (changed !== 2) throw new Error('KubeJS Lazy signature changed; re-audit concurrency patch');
            return node;
        }
    }};
}
