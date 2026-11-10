package dao.impl;

import config.AppConfig;
import models.Account;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class SellerAccountRepository {
    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 32;
    private final Path accountsFile = AppConfig.dataDirectory().resolve("accounts.tsv");

    public List<Account> load() throws IOException {
        if (!Files.exists(accountsFile)) return new ArrayList<>();
        List<Account> accounts = new ArrayList<>();
        int lineNumber = 0;
        for (String line : Files.readAllLines(accountsFile, StandardCharsets.UTF_8)) {
            lineNumber++;
            if (line.isBlank()) continue;
            String[] fields = line.split("\\t", -1);
            if (fields.length != 4) {
                throw new IOException("Format akun tidak valid pada baris " + lineNumber + ".");
            }
            try {
                byte[] salt = Base64.getDecoder().decode(fields[2]);
                byte[] passwordHash = Base64.getDecoder().decode(fields[3]);
                if (salt.length != SALT_LENGTH || passwordHash.length != HASH_LENGTH) {
                    throw new IllegalArgumentException("Panjang salt atau hash tidak valid.");
                }
                accounts.add(new Account(decodeText(fields[0]), decodeText(fields[1]), salt, passwordHash));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Data akun tidak valid pada baris " + lineNumber + ".", exception);
            }
        }
        return accounts;
    }

    public void save(List<Account> accounts) throws IOException {
        Files.createDirectories(accountsFile.getParent());
        Path temporaryFile = Files.createTempFile(accountsFile.getParent(), "accounts-", ".tmp");
        try {
            List<String> lines = new ArrayList<>();
            for (Account account : accounts) {
                lines.add(String.join("\t",
                        encodeText(account.getUsername()),
                        encodeText(account.getEmail()),
                        Base64.getEncoder().encodeToString(account.getSalt()),
                        Base64.getEncoder().encodeToString(account.getPasswordHash())));
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

    private static String encodeText(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeText(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
