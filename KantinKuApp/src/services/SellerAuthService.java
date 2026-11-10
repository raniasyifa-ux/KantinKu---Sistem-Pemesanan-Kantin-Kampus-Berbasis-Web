package services;

import java.io.IOException;
import java.util.Locale;
import models.Account;
import dao.impl.UserDAOImpl;
import util.BcryptPasswordHasher;

public final class SellerAuthService {
    private final UserDAOImpl accountRepository = new UserDAOImpl();

    public String authenticate(String usernameOrEmail, char[] password) throws IOException {
        if (usernameOrEmail == null || usernameOrEmail.isBlank() || password == null) {
            return null;
        }
        String identity = usernameOrEmail.trim().toLowerCase(Locale.ROOT);
        Account account = accountRepository.findSeller(identity);
        if (account == null) {
            return null;
        }
        return BcryptPasswordHasher.verify(password, account.getPasswordHashText())
                ? account.getUsername() : null;
    }

    public void register(String username, String email, char[] password) throws IOException {
        String cleanUsername = username.strip();
        String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
        validateUsername(cleanUsername);
        validateEmail(cleanEmail);
        validatePassword(password);

        if (accountRepository.exists(cleanUsername, cleanEmail)) {
            throw new IllegalArgumentException("Username atau email sudah terdaftar. Gunakan data lain.");
        }
        accountRepository.addSeller(cleanUsername, cleanEmail, BcryptPasswordHasher.hash(password));
    }

    public void resetPassword(String email, char[] newPassword) throws IOException {
        String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
        validateEmail(cleanEmail);
        validatePassword(newPassword);

        accountRepository.updateSellerPassword(cleanEmail, BcryptPasswordHasher.hash(newPassword));
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

}
