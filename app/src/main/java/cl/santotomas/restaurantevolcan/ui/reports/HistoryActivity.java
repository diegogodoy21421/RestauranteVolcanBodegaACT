package cl.santotomas.restaurantevolcan.ui.reports;

import android.os.Bundle;
import android.widget.*;
import java.text.DateFormat;
import java.util.*;
import cl.santotomas.restaurantevolcan.ui.common.Screen;
import cl.santotomas.restaurantevolcan.ui.products.*;

public class HistoryActivity extends Screen {
    private Spinner product, user;
    private final List<String> codes = new ArrayList<>(), users = new ArrayList<>();
    private LinearLayout results;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        screen("Historial de movimientos");
        ProductRepository repo = ProductRepository.getInstance(this);
        List<String> labels = new ArrayList<>();
        labels.add("Todos"); codes.add("");
        for (ProductItem p : repo.getProducts()) {
            labels.add(p.getCode() + " — " + p.getName() + (p.isAvailable() ? "" : " (inactivo)"));
            codes.add(p.getCode());
        }
        product = spinner("Producto", labels);
        users.add("Todos");
        for (ProductRepository.StockMovement m : repo.getMovements("", "", false))
            if (!users.contains(m.username)) users.add(m.username);
        user = spinner("Usuario", users);
        button("Consultar historial", v -> load());
        button("Volver al menú", v -> finish());
        results = new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL); body.addView(results);
        if (state != null) {
            product.setSelection(state.getInt("product"));
            user.setSelection(state.getInt("user"));
        }
        load();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("product", product.getSelectedItemPosition());
        state.putInt("user", user.getSelectedItemPosition());
    }
    private void load() {
        results.removeAllViews();
        List<ProductRepository.StockMovement> items = ProductRepository.getInstance(this).getMovements(
                codes.get(product.getSelectedItemPosition()),
                user.getSelectedItemPosition() == 0 ? "" : user.getSelectedItem().toString(), false);
        TextView count = new TextView(this);
        count.setText(items.isEmpty() ? "No hay movimientos para estos filtros." : items.size() + " movimientos");
        results.addView(count);
        for (ProductRepository.StockMovement m : items) {
            TextView row = new TextView(this);
            row.setPadding(0, 18, 0, 18);
            row.setText(m.productName + "\n" + m.type + " · " + m.quantity + " " + m.unit
                    + "\n" + m.username + " · " + DateFormat.getDateTimeInstance().format(new Date(m.registeredAt)));
            results.addView(row);
        }
    }
}
