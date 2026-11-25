package cl.santotomas.restaurantevolcan.ui.reports;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import cl.santotomas.restaurantevolcan.ui.products.ProductItem;

public class StockReportTest {
    private ProductItem product(String sku, String name, String category, int stock, int minimum, boolean active) {
        ProductItem p = new ProductItem(sku, name, "", category, "kg", stock, active);
        p.setMinimumStock(minimum); return p;
    }
    @Test public void zeroIsAlwaysOutOfStockAndMinimumIsInclusive() {
        assertEquals("Sin stock", product("A","A","X",0,0,true).getStockStatus());
        assertEquals("Crítico", product("A","A","X",5,5,true).getStockStatus());
        assertEquals("Disponible", product("A","A","X",6,5,true).getStockStatus());
    }
    @Test public void combinedFiltersExcludeInactiveProducts() {
        ProductItem match = product("P1","Harina","Repostería",5,5,true);
        List<ProductItem> items = Arrays.asList(match,
                product("P2","Harina integral","Repostería",6,5,true),
                product("P3","Harina","Frutas",2,5,true),
                product("P4","Harina","Repostería",1,5,false));
        assertEquals(Collections.singletonList(match), StockReport.filter(items,"Repostería","HARINA","Crítico"));
        assertEquals(Collections.singletonList(match), StockReport.filter(items,"","p1",""));
        assertTrue(StockReport.filter(items,"","inexistente","").isEmpty());
        assertEquals(3, StockReport.filter(items,"","","").size());
    }
}
