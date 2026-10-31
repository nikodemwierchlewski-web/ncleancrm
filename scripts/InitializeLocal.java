import java.io.Console;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Run from the repository root with Java 17 and the project's runtime classpath. */
class InitializeLocal {
    private static final Path LOCAL = Path.of(".local");
    private static final List<Path> TARGETS = List.of(
        LOCAL.resolve("postgres-admin-password"),
        LOCAL.resolve("postgres-password"),
        LOCAL.resolve("application-local.properties")
    );

    public static void main(String[] args) throws Exception {
        if (args.length != 0) {
            throw new IllegalArgumentException("This initializer does not accept passwords or other arguments.");
        }
        if (!Files.isRegularFile(Path.of("pom.xml"))) {
            throw new IllegalStateException("Run this initializer from the repository root.");
        }
        if (Files.isSymbolicLink(LOCAL)) {
            throw new IllegalStateException("The .local directory must not be a symbolic link.");
        }
        for (Path target : TARGETS) {
            if (Files.exists(target) || Files.isSymbolicLink(target)) {
                throw new IllegalStateException("Local configuration already exists; no files were overwritten.");
            }
        }
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("An interactive terminal is required to enter the owner password safely.");
        }

        char[] password = console.readPassword("Password for local-owner (12 or more characters): ");
        char[] confirmation = null;
        String hash;
        try {
            if (password == null || password.length < 12
                    || new String(password).getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new IllegalArgumentException("Use at least 12 characters and at most 72 UTF-8 bytes.");
            }
            confirmation = console.readPassword("Repeat password: ");
            if (confirmation == null || !Arrays.equals(password, confirmation)) {
                throw new IllegalArgumentException("Passwords do not match; no files were written.");
            }
            hash = new BCryptPasswordEncoder(12).encode(new String(password));
        } finally {
            if (password != null) Arrays.fill(password, '\0');
            if (confirmation != null) Arrays.fill(confirmation, '\0');
        }

        Files.createDirectories(LOCAL);
        boolean posix = Files.getFileAttributeView(LOCAL, PosixFileAttributeView.class) != null;
        if (posix) Files.setPosixFilePermissions(LOCAL, PosixFilePermissions.fromString("rwx------"));

        String administratorPassword = randomPassword();
        String applicationPassword = randomPassword();
        Properties properties = new Properties();
        properties.setProperty("CRM_DB_URL", "jdbc:postgresql://127.0.0.1:55433/nclean_crm");
        properties.setProperty("CRM_DB_USERNAME", "crm_dev");
        properties.setProperty("CRM_DB_PASSWORD", applicationPassword);
        properties.setProperty("CRM_ADMIN_USERNAME", "local-owner");
        properties.setProperty("CRM_ADMIN_PASSWORD_HASH", hash);

        List<Path> created = new ArrayList<>();
        try {
            writeSecret(TARGETS.get(0), administratorPassword + "\n", posix, created);
            writeSecret(TARGETS.get(1), applicationPassword + "\n", posix, created);
            createSecret(TARGETS.get(2), posix, created);
            try (Writer writer = Files.newBufferedWriter(TARGETS.get(2), StandardCharsets.UTF_8,
                    StandardOpenOption.WRITE)) {
                properties.store(writer, "Local ncLEAN CRM only. Never commit this file.");
            }
        } catch (Exception failure) {
            for (Path path : created) Files.deleteIfExists(path);
            throw failure;
        }
        console.printf("Local configuration created in .local. Owner login: local-owner.%n");
        console.printf("Database passwords were generated locally. The owner password was not stored.%n");
    }

    private static String randomPassword() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static void createSecret(Path path, boolean posix, List<Path> created) throws IOException {
        if (posix) {
            Files.createFile(path, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        } else {
            Files.createFile(path);
        }
        created.add(path);
    }

    private static void writeSecret(Path path, String value, boolean posix, List<Path> created) throws IOException {
        createSecret(path, posix, created);
        Files.writeString(path, value, StandardCharsets.UTF_8, StandardOpenOption.WRITE);
    }
}
