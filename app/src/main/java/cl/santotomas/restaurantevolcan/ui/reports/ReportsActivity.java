package cl.santotomas.restaurantevolcan.ui.reports;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.io.OutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.*;
import cl.santotomas.restaurantevolcan.ui.common.Screen;
import cl.santotomas.restaurantevolcan.ui.products.*;

public class ReportsActivity extends Screen {
    private Spinner category, status;
    private EditText product;
    private LinearLayout results;
    private ArrayList<String> reportLines = new ArrayList<>();
    private ArrayList<String> exportLines;
    private final ActivityResultLauncher<String> export = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/pdf"), this::writePdf);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        screen("Informe de stock");
        List<String> categories = new ArrayList<>();
        categories.add("Todas");
        for (ProductItem p : ProductRepository.getInstance(this).getProducts())
            if (!categories.contains(p.getCategory())) categories.add(p.getCategory());
        category = spinner("Categoría", categories);
        product = field("Producto: nombre o SKU", false);
        status = spinner("Estado del stock", Arrays.asList("Todos", "Sin stock", "Crítico", "Disponible"));
        button("Generar informe", v -> generate());
        button("Exportar informe a PDF", v -> {
            generate();
            exportLines = new ArrayList<>(reportLines);
            export.launch("Informe_stock_" + System.currentTimeMillis() + ".pdf");
        });
        button("Volver al menú", v -> finish());
        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        body.addView(results);
        if (state != null) {
            category.setSelection(state.getInt("category"));
            status.setSelection(state.getInt("status"));
            product.setText(state.getString("query", ""));
            exportLines = state.getStringArrayList("export");
        }
        generate();
    }
    @Override protected void onResume() { super.onResume(); if (results != null) generate(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("category", category.getSelectedItemPosition());
        state.putInt("status", status.getSelectedItemPosition());
        state.putString("query", product.getText().toString());
        state.putStringArrayList("export", exportLines);
    }
    private void generate() {
        String cat = category.getSelectedItemPosition() == 0 ? "" : category.getSelectedItem().toString();
        String stockState = status.getSelectedItemPosition() == 0 ? "" : status.getSelectedItem().toString();
        List<ProductItem> items = StockReport.filter(ProductRepository.getInstance(this).getProducts(),
                cat, product.getText().toString(), stockState);
        reportLines.clear();
        reportLines.add("Volcán Waffles & Café — Informe de stock");
        reportLines.add(DateFormat.getDateTimeInstance().format(new Date()));
        reportLines.add("Categoría: " + (cat.isEmpty() ? "Todas" : cat));
        reportLines.add("Producto: " + product.getText() + " | Estado: " + status.getSelectedItem());
        reportLines.add("Productos activos encontrados: " + items.size());
        for (ProductItem p : items) {
            reportLines.add(p.getCode() + " — " + p.getName());
            reportLines.add("Categoría: " + p.getCategory());
            reportLines.add("Actual: " + p.getStock() + " " + p.getUnit() + " | Mínimo: "
                    + p.getMinimumStock() + " | " + p.getStockStatus());
            reportLines.add(" ");
        }
        if (items.isEmpty()) reportLines.add("No existen productos para los filtros seleccionados.");
        results.removeAllViews();
        for (String line : reportLines) {
            TextView view = new TextView(this);
            view.setText(line);
            view.setTextSize(16);
            view.setPadding(0, 6, 0, 6);
            results.addView(view);
        }
    }
    private void writePdf(Uri uri) {
        if (uri == null) return;
        if (exportLines == null) { message("Genera nuevamente el informe."); return; }
        PdfDocument document = new PdfDocument();
        try {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.BLACK);
            paint.setTextSize(11);
            List<String> wrapped = new ArrayList<>();
            for (String source : exportLines) {
                String rest = source;
                while (paint.measureText(rest) > 510) {
                    int count = paint.breakText(rest, true, 510, null);
                    int space = rest.lastIndexOf(' ', count);
                    int cut = space > 0 ? space : Math.max(1, count);
                    wrapped.add(rest.substring(0, cut));
                    rest = rest.substring(cut).trim();
                }
                wrapped.add(rest);
            }
            int pageNumber = 0;
            for (int start = 0; start < wrapped.size(); start += 42) {
                PdfDocument.Page page = document.startPage(new PdfDocument.PageInfo.Builder(595, 842, ++pageNumber).create());
                Canvas canvas = page.getCanvas();
                int y = 48;
                for (int i = start; i < Math.min(start + 42, wrapped.size()); i++, y += 17)
                    canvas.drawText(wrapped.get(i), 42, y, paint);
                canvas.drawText("Página " + pageNumber, 42, 810, paint);
                document.finishPage(page);
            }
            try (OutputStream stream = getContentResolver().openOutputStream(uri)) {
                if (stream == null) throw new IOException("Destino no disponible");
                document.writeTo(stream);
            }
            message("Informe PDF guardado.");
        } catch (IOException | RuntimeException e) { message("No se pudo guardar el PDF. Intenta en otra carpeta."); }
        finally { document.close(); }
    }
}
