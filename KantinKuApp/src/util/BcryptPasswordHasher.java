package util;

import at.favre.lib.crypto.bcrypt.BCrypt;
import java.io.IOException;

public final class BcryptPasswordHasher {
    private static final int COST = 10;

    private BcryptPasswordHasher() {
    }

    public static boolean verify(char[] password, String encoded) throws IOException {
        if (encoded == null || !encoded.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}")) {
            throw new IOException("Format hash bcrypt akun tidak dikenali.");
        }
        try {
            return BCrypt.verifyer().verify(password, encoded).verified;
        } catch (IllegalArgumentException exception) {
            throw new IOException("Hash bcrypt akun tidak valid.", exception);
        }
    }

    public static String hash(char[] password) throws IOException {
        try {
            return BCrypt.withDefaults().hashToString(COST, password);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Password tidak dapat di-hash dengan bcrypt.", exception);
        }
    }
}
