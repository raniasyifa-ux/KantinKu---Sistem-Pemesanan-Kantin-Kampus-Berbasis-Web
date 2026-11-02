package id.kantinku.desktop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class SellerAuthService {
    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 256;
    private static final int ITERATIONS = 120_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Path accountsFile = Path.of(
            System.getProperty("user.home"), ".kantinku-desktop", "accounts.tsv");

    String authenticate(String usernameOrEmail, char[] password) throws IOException {
        String identity = usernameOrEmail.trim().toLowerCase(Locale.ROOT);
        for (Account account : loadAccounts()) {
            if (account.username.toLowerCase(Locale.ROOT).equals(identity)
                    || account.email.equals(identity)) {
                return MessageDigest.isEqual(account.passwordHash, hash(password, account.salt))
                        ? account.username
                        : null;
            }
        }
        return null;
    }

    void register(String username, String email, char[] password) throws IOException {
        String cleanUsername = username.strip();
        String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
        validateUsername(cleanUsername);
        validateEmail(cleanEmail);
        validatePassword(password);

        List<Account> accounts = loadAccounts();
        for (Account account : accounts) {
            if (account.username.equalsIgnoreCase(cleanUsername) || account.email.equals(cleanEmail)) {
                throw new IllegalArgumentException("Username atau email sudah terdaftar. Gunakan data lain.");
            }
        }

        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        accounts.add(new Account(cleanUsername, cleanEmail, salt, hash(password, salt)));
        saveAccounts(accounts);
    }

    void resetPassword(String email, char[] newPassword) throws IOException {
        String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
        validateEmail(cleanEmail);
        validatePassword(newPassword);

        List<Account> accounts = loadAccounts();
        for (Account account : accounts) {
            if (account.email.equals(cleanEmail)) {
                byte[] salt = new byte[SALT_LENGTH];
                RANDOM.nextBytes(salt);
                account.salt = salt;
                account.passwordHash = hash(newPassword, salt);
                saveAccounts(accounts);
                return;
            }
        }
        throw new IllegalArgumentException("Email tidak ditemukan pada akun penjual desktop.");
    }

    private static void validateUsername(String username) {
        int characterCount = username.codePointCount(0, username.length());
        if (characterCount < 4 || characterCount > 12) {
            throw new IllegalArgumentException("Username harus sepanjang 4-12 karakter.");
        }
        for (int index = 0; index < username.length();) {
            int codePoint = username.codePointAt(index);
            if (!Character.isLetterOrDigit(codePoint)) {
                throw new IllegalArgumentException(
                        "Username hanya boleh berisi huruf atau angka, tanpa spasi atau tanda baca.");
            }
            index += Character.charCount(codePoint);
        }
    }

    private static void validateEmail(String email) {
        int at = email.indexOf('@');
        int dot = email.lastIndexOf('.');
        if (at <= 0 || dot <= at + 1 || dot == email.length() - 1 || email.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Masukkan alamat email yang valid.");
        }
    }

    private static void validatePassword(char[] password) {
        boolean hasLetter = false;
        boolean hasDigit = false;
        boolean valid = password.length >= 8;
        for (char character : password) {
            boolean letter = isAsciiLetter(character);
            boolean digit = isAsciiDigit(character);
            hasLetter |= letter;
            hasDigit |= digit;
            valid &= letter || digit;
        }
        if (!valid || !hasLetter || !hasDigit) {
            throw new IllegalArgumentException(
                    "Password minimal 8 karakter dan harus mengandung huruf serta angka.");
        }
    }

    private static boolean isAsciiLetter(char character) {
        return (character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z');
    }

    private static boolean isAsciiDigit(char character) {
        return character >= '0' && character <= '9';
    }

    private List<Account> loadAccounts() throws IOException {
        if (!Files.exists(accountsFile)) {
            return new ArrayList<>();
        }
        List<Account> accounts = new ArrayList<>();
        int lineNumber = 0;
        for (String line : Files.readAllLines(accountsFile, StandardCharsets.UTF_8)) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split("\\t", -1);
            if (fields.length != 4) {
                throw new IOException("Format akun tidak valid pada baris " + lineNumber + ".");
            }
            try {
                byte[] salt = Base64.getDecoder().decode(fields[2]);
                byte[] passwordHash = Base64.getDecoder().decode(fields[3]);
                if (salt.length != SALT_LENGTH || passwordHash.length != HASH_LENGTH / 8) {
                    throw new IllegalArgumentException("Panjang salt atau hash tidak valid.");
                }
                accounts.add(new Account(decodeText(fields[0]), decodeText(fields[1]), salt, passwordHash));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Data akun tidak valid pada baris " + lineNumber + ".", exception);
            }
        }
        return accounts;
    }

    private void saveAccounts(List<Account> accounts) throws IOException {
        Files.createDirectories(accountsFile.getParent());
        Path temporaryFile = Files.createTempFile(accountsFile.getParent(), "accounts-", ".tmp");
        try {
            List<String> lines = new ArrayList<>();
            for (Account account : accounts) {
                lines.add(String.join("\t",
                        encodeText(account.username),
                        encodeText(account.email),
                        Base64.getEncoder().encodeToString(account.salt),
                        Base64.getEncoder().encodeToString(account.passwordHash)));
            }
            Files.write(temporaryFile, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temporaryFile, accountsFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, accountsFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static byte[] hash(char[] password, byte[] salt) throws IOException {
        PBEKeySpec keySpec = new PBEKeySpec(password, salt, ITERATIONS, HASH_LENGTH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(keySpec)
                    .getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IOException("Tidak dapat memverifikasi password.", exception);
        } finally {
            keySpec.clearPassword();
        }
    }

    private static String encodeText(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeText(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static final class Account {
        private final String username;
        private final String email;
        private byte[] salt;
        private byte[] passwordHash;

        private Account(String username, String email, byte[] salt, byte[] passwordHash) {
            this.username = username;
            this.email = email;
            this.salt = salt;
            this.passwordHash = passwordHash;
        }
    }
}
