package cl.santotomas.restaurantevolcan.ui.products;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class ProductRepository extends SQLiteOpenHelper {

    private static final String[] PRODUCT_COLUMNS = {
            "code", "name", "description", "category", "unit", "stock", "available", "barcode", "minimum_stock", "physical_location"
    };

    private static ProductRepository instance;

    private ProductRepository(Context context) {
        super(context, "volcan_bodega.db", null, 3);
    }

    public static synchronized ProductRepository getInstance(Context context) {
        if (instance == null) {
            instance = new ProductRepository(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onConfigure(SQLiteDatabase database) {
        database.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE products ("
                + "id INTEGER PRIMARY KEY, code TEXT NOT NULL UNIQUE, name TEXT NOT NULL, "
                + "description TEXT NOT NULL, category TEXT NOT NULL, unit TEXT NOT NULL, "
                + "stock INTEGER NOT NULL, available INTEGER NOT NULL, barcode TEXT UNIQUE, "
                + "minimum_stock INTEGER NOT NULL DEFAULT 5, physical_location TEXT NOT NULL DEFAULT '')");

        database.execSQL("CREATE TABLE stock_movements ("
                + "id INTEGER PRIMARY KEY, product_code TEXT NOT NULL REFERENCES products(code), "
                + "type TEXT NOT NULL, quantity INTEGER NOT NULL CHECK(quantity > 0), "
                + "registered_at INTEGER NOT NULL, username TEXT NOT NULL)");

        seedProduct(database, new ProductItem(
                "P001", "Harina pastelera", "Harina fina para waffles y masas",
                "Repostería", "kg", 18, true));
        seedProduct(database, new ProductItem(
                "P002", "Frutilla", "Frutas frescas seleccionadas",
                "Frutas", "kg", 12, true));
        seedProduct(database, new ProductItem(
                "P003", "Salsa de chocolate", "Salsa especial para decoración",
                "Toppings y salsas", "botellas", 4, true));
        seedProduct(database, new ProductItem(
                "P004", "Helado de vainilla", "Helado artesanal premium",
                "Lácteos y refrigerados", "baldes", 2, false));
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            database.execSQL("CREATE TABLE IF NOT EXISTS stock_movements ("
                    + "id INTEGER PRIMARY KEY, product_code TEXT NOT NULL REFERENCES products(code), "
                    + "type TEXT NOT NULL, quantity INTEGER NOT NULL CHECK(quantity > 0), "
                    + "registered_at INTEGER NOT NULL, username TEXT NOT NULL)");

            database.execSQL("INSERT INTO stock_movements (product_code, type, quantity, registered_at, username) "
                    + "SELECT product_code, 'ENTRADA', quantity, registered_at, username FROM stock_entries");

            database.execSQL("DROP TABLE IF EXISTS stock_entries");
        }
        if (oldVersion < 3) {
            database.execSQL("ALTER TABLE products ADD COLUMN barcode TEXT");
            database.execSQL("ALTER TABLE products ADD COLUMN minimum_stock INTEGER NOT NULL DEFAULT 5");
            database.execSQL("ALTER TABLE products ADD COLUMN physical_location TEXT NOT NULL DEFAULT ''");
            database.execSQL("CREATE UNIQUE INDEX index_product_barcode ON products(barcode)");
        }
    }

    public void saveProduct(ProductItem item, boolean create) {
        if (item.getStock() < 0 || item.getMinimumStock() < 0)
            throw new IllegalArgumentException("El stock no puede ser negativo.");
        ContentValues values = valuesForProduct(item);
        if (create) getWritableDatabase().insertOrThrow("products", null, values);
        else if (getWritableDatabase().update("products", values, "code = ?",
                new String[]{item.getCode()}) != 1) throw new IllegalStateException("Producto no encontrado.");
    }

    public ProductItem findByBarcode(String barcode) {
        for (ProductItem item : getProducts()) if (item.getBarcode().equals(barcode)) return item;
        return null;
    }

    public List<ProductItem> getProducts() {
        List<ProductItem> products = new ArrayList<>();

        try (Cursor cursor = getReadableDatabase().query(
                "products", PRODUCT_COLUMNS, null, null, null, null,
                "CASE WHEN id <= 4 THEN 1 ELSE 0 END, "
                        + "CASE WHEN id <= 4 THEN id ELSE -id END")) {
            while (cursor.moveToNext()) {
                ProductItem item = new ProductItem(
                        cursor.getString(0), cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4), cursor.getInt(5),
                        cursor.getInt(6) != 0);
                item.setBarcode(cursor.getString(7));
                item.setMinimumStock(cursor.getInt(8));
                item.setLocation(cursor.getString(9));
                products.add(item);
            }
        }

        return products;
    }

    public boolean addProduct(ProductItem item) {
        return getWritableDatabase().insertWithOnConflict(
                "products", null, valuesForProduct(item),
                SQLiteDatabase.CONFLICT_IGNORE) != -1;
    }

    public void updateProduct(String code, String name, String description,
                              String category, String unit, int stock, boolean available) {
        updateByCode(code, productValues(name, description, category, unit, stock, available));
    }

    public void updateStock(String code, int newStock) {
        ContentValues values = new ContentValues();
        values.put("stock", newStock);
        updateByCode(code, values);
    }

    public void setAvailable(String code, boolean available) {
        ContentValues values = new ContentValues();
        values.put("available", available);
        updateByCode(code, values);
    }

    public int registerEntry(String productCode, int quantity, String username) {
        if (quantity <= 0 || username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("La entrada requiere cantidad y usuario.");
        }

        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();

        try {
            int currentStock;
            try (Cursor cursor = database.query(
                    "products", new String[]{"stock"},
                    "code = ? AND available = 1", new String[]{productCode},
                    null, null, null)) {
                if (!cursor.moveToFirst()) {
                    throw new IllegalStateException("El producto ya no está disponible.");
                }
                currentStock = cursor.getInt(0);
            }

            int newStock = Math.addExact(currentStock, quantity);

            ContentValues movement = new ContentValues();
            movement.put("product_code", productCode);
            movement.put("type", "ENTRADA");
            movement.put("quantity", quantity);
            movement.put("registered_at", System.currentTimeMillis());
            movement.put("username", username);
            database.insertOrThrow("stock_movements", null, movement);

            ContentValues stock = new ContentValues();
            stock.put("stock", newStock);
            if (database.update("products", stock, "code = ?",
                    new String[]{productCode}) != 1) {
                throw new SQLiteException("No se pudo actualizar el stock.");
            }

            database.setTransactionSuccessful();
            return newStock;
        } finally {
            database.endTransaction();
        }
    }

    public int registerExit(String productCode, int quantity, String username) {
        if (quantity <= 0 || username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("La salida requiere cantidad y usuario.");
        }

        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();

        try {
            int currentStock;
            try (Cursor cursor = database.query(
                    "products", new String[]{"stock"},
                    "code = ? AND available = 1", new String[]{productCode},
                    null, null, null)) {
                if (!cursor.moveToFirst()) {
                    throw new IllegalStateException("El producto ya no está disponible.");
                }
                currentStock = cursor.getInt(0);
            }

            if (quantity > currentStock) {
                throw new IllegalArgumentException("Stock insuficiente. Stock actual: " + currentStock);
            }

            int newStock = currentStock - quantity;

            ContentValues movement = new ContentValues();
            movement.put("product_code", productCode);
            movement.put("type", "SALIDA");
            movement.put("quantity", quantity);
            movement.put("registered_at", System.currentTimeMillis());
            movement.put("username", username);
            database.insertOrThrow("stock_movements", null, movement);

            ContentValues stock = new ContentValues();
            stock.put("stock", newStock);
            if (database.update("products", stock, "code = ?",
                    new String[]{productCode}) != 1) {
                throw new SQLiteException("No se pudo actualizar el stock.");
            }

            database.setTransactionSuccessful();
            return newStock;
        } finally {
            database.endTransaction();
        }
    }

    public List<StockMovement> getRecentMovements() { return getMovements("", "", true); }

    public List<StockMovement> getMovements(String productCode, String username, boolean recent) {
        List<StockMovement> movements = new ArrayList<>();
        String sql = "SELECT p.name, m.type, p.unit, m.quantity, m.registered_at, m.username "
                + "FROM stock_movements m JOIN products p ON p.code = m.product_code "
                + "WHERE (? = '' OR m.product_code = ?) AND (? = '' OR m.username = ?) "
                + "ORDER BY m.id DESC" + (recent ? " LIMIT 20" : "");

        try (Cursor cursor = getReadableDatabase().rawQuery(sql, new String[]{productCode, productCode, username, username})) {
            while (cursor.moveToNext()) {
                movements.add(new StockMovement(
                        cursor.getString(0), cursor.getString(1), cursor.getString(2),
                        cursor.getInt(3), cursor.getLong(4), cursor.getString(5)));
            }
        }

        return movements;
    }

    public List<StockEntry> getRecentEntries() {
        List<StockEntry> entries = new ArrayList<>();
        for (StockMovement m : getRecentMovements()) {
            if ("ENTRADA".equalsIgnoreCase(m.type)) {
                entries.add(new StockEntry(m.productName, m.unit, m.quantity, m.registeredAt, m.username));
            }
        }
        return entries;
    }

    private void updateByCode(String code, ContentValues values) {
        getWritableDatabase().update("products", values, "code = ?", new String[]{code});
    }

    private static void seedProduct(SQLiteDatabase database, ProductItem item) {
        database.insertOrThrow("products", null, valuesForProduct(item));
    }

    private static ContentValues valuesForProduct(ProductItem item) {
        ContentValues values = productValues(
                item.getName(), item.getDescription(), item.getCategory(),
                item.getUnit(), item.getStock(), item.isAvailable());
        values.put("code", item.getCode());
        if (item.getBarcode().isEmpty()) values.putNull("barcode");
        else values.put("barcode", item.getBarcode());
        values.put("minimum_stock", item.getMinimumStock());
        values.put("physical_location", item.getLocation());
        return values;
    }

    private static ContentValues productValues(String name, String description,
                                               String category, String unit, int stock, boolean available) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("description", description);
        values.put("category", category);
        values.put("unit", unit);
        values.put("stock", stock);
        values.put("available", available);
        return values;
    }

    public static final class StockMovement {
        public final String productName;
        public final String type;
        public final String unit;
        public final int quantity;
        public final long registeredAt;
        public final String username;

        public StockMovement(String productName, String type, String unit, int quantity,
                             long registeredAt, String username) {
            this.productName = productName;
            this.type = type;
            this.unit = unit;
            this.quantity = quantity;
            this.registeredAt = registeredAt;
            this.username = username;
        }
    }

    public static final class StockEntry {
        public final String productName;
        public final String unit;
        public final int quantity;
        public final long registeredAt;
        public final String username;

        private StockEntry(String productName, String unit, int quantity,
                           long registeredAt, String username) {
            this.productName = productName;
            this.unit = unit;
            this.quantity = quantity;
            this.registeredAt = registeredAt;
            this.username = username;
        }
    }
}