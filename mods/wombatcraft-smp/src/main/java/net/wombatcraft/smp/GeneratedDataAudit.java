package net.wombatcraft.smp;

/** Regression for the observed KubeJS GeneratedData null-byte-array startup failure. */
final class GeneratedDataAudit {
    static void run() throws Exception {
        if (!Boolean.getBoolean("wombatcraft.qa")) throw new IllegalStateException("Disposable QA only");
        Class<?> lazyType=Class.forName("dev.latvian.mods.kubejs.util.Lazy");
        for (String name:new String[]{"get","forget"}) {
            if (!java.lang.reflect.Modifier.isSynchronized(lazyType.getMethod(name).getModifiers()))
                throw new IllegalStateException("KubeJS Lazy synchronization patch missing: "+name);
        }
        byte[] expected="{\"valid\":true}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Object lazy=lazyType.getMethod("of",java.util.function.Supplier.class).invoke(null,(java.util.function.Supplier<byte[]>)()->expected);
        Class<?> generated=Class.forName("dev.latvian.mods.kubejs.script.data.GeneratedData");
        Object data=generated.getConstructor(net.minecraft.resources.ResourceLocation.class,lazyType,boolean.class)
            .newInstance(new net.minecraft.resources.ResourceLocation("wombatcraft_smp","qa.json"),lazy,true);
        var get=generated.getMethod("get");
        var workers=java.util.concurrent.Executors.newFixedThreadPool(8);
        try {
            var calls=new java.util.ArrayList<java.util.concurrent.Callable<Void>>();
            for (int thread=0;thread<8;thread++) calls.add(()->{
                for (int i=0;i<2000;i++) try (var stream=(java.io.InputStream)get.invoke(data)) {
                    if (!java.util.Arrays.equals(stream.readAllBytes(),expected)) throw new IllegalStateException("Generated resource corrupted");
                }
                return null;
            });
            for (var result:workers.invokeAll(calls,30,java.util.concurrent.TimeUnit.SECONDS)) result.get();
        } finally { workers.shutdownNow(); }
        System.out.println("KUBEJS_GENERATED_DATA_AUDIT_COMPLETE parallel_reads=16000 forget=true");
    }
}
