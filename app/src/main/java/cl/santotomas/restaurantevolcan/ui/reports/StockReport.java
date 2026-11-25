package cl.santotomas.restaurantevolcan.ui.reports;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import cl.santotomas.restaurantevolcan.ui.products.ProductItem;

/** Same rules for the screen and the exported report. */
public final class StockReport {
    private StockReport() {}
    public static List<ProductItem> filter(List<ProductItem> products, String category,
                                           String query, String status) {
        List<ProductItem> result = new ArrayList<>();
        String search = query.trim().toLowerCase(Locale.ROOT);
        for (ProductItem p : products) {
            if (!p.isAvailable()) continue;
            if (!category.isEmpty() && !category.equals(p.getCategory())) continue;
            if (!search.isEmpty() && !(p.getName().toLowerCase(Locale.ROOT).contains(search)
                    || p.getCode().toLowerCase(Locale.ROOT).contains(search))) continue;
            if (!status.isEmpty() && !status.equals(p.getStockStatus())) continue;
            result.add(p);
        }
        return result;
    }
}
