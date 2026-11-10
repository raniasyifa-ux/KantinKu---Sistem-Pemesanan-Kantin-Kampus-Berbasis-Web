package services;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import models.Menu;
import dao.impl.MenuDAOImpl;

/** Business API for the catalog shared by the buyer and seller applications. */
public final class KatalogBersama {
    private static final MenuDAOImpl REPOSITORY = new MenuDAOImpl();

    private KatalogBersama() {
    }

    public static long penanda() {
        try {
            return REPOSITORY.changeMarker();
        } catch (IOException exception) {
            throw new IllegalStateException("Perubahan katalog MySQL tidak dapat diperiksa.", exception);
        }
    }

    public static Optional<List<Menu>> bacaMenuAktif() throws IOException {
        return REPOSITORY.readActiveBuyerCatalog();
    }
}
