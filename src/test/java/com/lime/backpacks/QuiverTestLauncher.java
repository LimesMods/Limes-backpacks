package com.lime.backpacks;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;

public final class QuiverTestLauncher {
    public static void main(String[] args) throws Exception {
        // Gradle shortens long Windows classpaths to a manifest-only JAR. Fabric's
        // game discovery reads java.class.path, so expose the original entries.
        var entries = new java.util.ArrayList<String>();
        for (String entry : System.getProperty("java.class.path").split(java.io.File.pathSeparator)) {
            var path = java.nio.file.Path.of(entry);
            if (path.getFileName().toString().startsWith("gradle-javaexec-classpath")) {
                try (var jar = new java.util.jar.JarFile(path.toFile())) {
                    String manifestPath = jar.getManifest().getMainAttributes().getValue("Class-Path");
                    for (String uri : manifestPath.split(" "))
                        entries.add(java.nio.file.Path.of(path.toUri().resolve(uri)).toString());
                }
            } else entries.add(entry);
        }
        System.setProperty("java.class.path", String.join(java.io.File.pathSeparator, entries));
        ClassLoader loader = new Knot(EnvType.CLIENT).init(new String[0]);
        Class.forName(System.getProperty("verificationClass", "com.lime.backpacks.QuiverChecks"), true, loader)
                .getMethod("run").invoke(null);
    }
}
