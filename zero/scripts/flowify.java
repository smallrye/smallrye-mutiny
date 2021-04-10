///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0
//DEPS org.ow2.asm:asm-commons:9.1
//DEPS commons-io:commons-io:2.8.0

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

// Made for Java 8 compatibility

@Command(name = "flowify", mixinStandardHelpOptions = true, version = "flowify 0.1", description = "Rewrite from reactive streams to JDK Flow")
class flowify implements Callable<Integer> {

    @Parameters(index = "0", description = "Input jar file")
    private String inputFile;

    @Parameters(index = "1", description = "Output jar file")
    private String outputFile;

    public static void main(String... args) {
        int exitCode = new CommandLine(new flowify()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        Map<String, String> mappings = new HashMap<String, String>() {
            {
                put("org/reactivestreams/Publisher", "java/util/concurrent/Flow$Publisher");
                put("org/reactivestreams/Processor", "java/util/concurrent/Flow$Processor");
                put("org/reactivestreams/Subscriber", "java/util/concurrent/Flow$Subscriber");
                put("org/reactivestreams/Subscription", "java/util/concurrent/Flow$Subscription");
            }
        };
        SimpleRemapper remapper = new SimpleRemapper(mappings);

        try (
                ZipInputStream inputStream = new ZipInputStream(new FileInputStream(inputFile));
                ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(outputFile))) {

            ZipEntry entry = inputStream.getNextEntry();
            while (entry != null) {
                System.out.println("👉 Reading " + entry.getName());
                byte[] bytes = IOUtils.toByteArray(inputStream);

                if (isCandidateClassFile(entry.getName())) {
                    System.out.println("  ⚡️ Rewriting " + entry.getName());
                    ClassReader classReader = new ClassReader(bytes);
                    ClassWriter classWriter = new ClassWriter(0);
                    ClassRemapper classRemapper = new ClassRemapper(classWriter, remapper);
                    classReader.accept(classRemapper, 0);
                    bytes = classWriter.toByteArray();
                } else if ("module-info.class".equals(entry.getName())) {
                    System.out.println("  🪛  Generating a new " + entry.getName());
                    ClassWriter classWriter = new ClassWriter(0);
                    classWriter.visit(Opcodes.V9, Opcodes.ACC_MODULE, "module-info", null, null, null);
                    ModuleVisitor moduleVisitor = classWriter.visitModule("mutiny.zero", Opcodes.ACC_SYNTHETIC, null);
                    moduleVisitor.visitRequire("java.base", Opcodes.ACC_MANDATED, null);
                    moduleVisitor.visitExport("mutiny/zero", 0);
                    moduleVisitor.visitEnd();
                    classWriter.visitEnd();
                    bytes = classWriter.toByteArray();
                }

                outputStream.putNextEntry(new ZipEntry(entry.getName()));
                outputStream.write(bytes);

                inputStream.closeEntry();
                outputStream.closeEntry();
                entry = inputStream.getNextEntry();
            }

        }
        return 0;
    }

    private boolean isCandidateClassFile(String name) {
        return name.endsWith(".class") &&
                !name.endsWith("module-info.class") &&
                !name.endsWith("package-info.class");
    }
}
