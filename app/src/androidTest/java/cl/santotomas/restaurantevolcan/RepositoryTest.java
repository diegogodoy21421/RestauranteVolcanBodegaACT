package cl.santotomas.restaurantevolcan;

import android.content.Context;
import android.database.sqlite.*;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import cl.santotomas.restaurantevolcan.ui.products.*;

@RunWith(AndroidJUnit4.class)
public class RepositoryTest {
    @Test public void migrationAndMovementsPreserveData() {
        Context context = ApplicationProvider.getApplicationContext();
        ProductRepository repo = ProductRepository.getInstance(context);
        SQLiteDatabase db = repo.getWritableDatabase();
        db.beginTransaction(); // Roll back the entire fixture, including the migration.
        try {
            db.execSQL("DROP TABLE stock_movements");
            db.execSQL("DROP TABLE products");
            db.execSQL("CREATE TABLE products (id INTEGER PRIMARY KEY, code TEXT NOT NULL UNIQUE, name TEXT NOT NULL, description TEXT NOT NULL, category TEXT NOT NULL, unit TEXT NOT NULL, stock INTEGER NOT NULL, available INTEGER NOT NULL)");
            db.execSQL("CREATE TABLE stock_movements (id INTEGER PRIMARY KEY, product_code TEXT NOT NULL REFERENCES products(code), type TEXT NOT NULL, quantity INTEGER NOT NULL CHECK(quantity > 0), registered_at INTEGER NOT NULL, username TEXT NOT NULL)");
            db.execSQL("INSERT INTO products VALUES(1,'TEST','Harina','','Repostería','kg',10,1)");
            db.execSQL("INSERT INTO stock_movements VALUES(1,'TEST','ENTRADA',10,123,'antiguo')");
            repo.onUpgrade(db, 2, 3);
            assertEquals(10, repo.getProducts().get(0).getStock());
            assertEquals(5, repo.getProducts().get(0).getMinimumStock());
            assertEquals(1, repo.getMovements("TEST", "antiguo", false).size());
            ProductItem p = repo.getProducts().get(0);
            p.setBarcode("1234567890128"); p.setMinimumStock(4);
            repo.saveProduct(p, false);
            assertEquals("TEST", repo.findByBarcode("1234567890128").getCode());
            assertEquals(15, repo.registerEntry("TEST", 5, "admin"));
            assertEquals(12, repo.registerExit("TEST", 3, "admin"));
            try { repo.registerExit("TEST", 13, "admin"); fail("Salida inválida aceptada"); }
            catch (IllegalArgumentException expected) {}
            assertEquals(12, repo.getProducts().get(0).getStock());
            assertEquals(2, repo.getMovements("TEST","admin",false).size());
            assertEquals(3, repo.getMovements("TEST","",false).size());
            ProductItem duplicate = new ProductItem("OTHER","Otro","","X","kg",0,true);
            duplicate.setBarcode("1234567890128");
            try { repo.saveProduct(duplicate, true); fail("Código duplicado aceptado"); }
            catch (SQLiteConstraintException expected) {}
            repo.setAvailable("TEST", false);
            assertFalse(repo.findByBarcode("1234567890128").isAvailable());
            assertEquals(3, repo.getMovements("TEST","",false).size());
        } finally { db.endTransaction(); }
    }
}
