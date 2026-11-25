package cl.santotomas.restaurantevolcan.ui.products;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.journeyapps.barcodescanner.*;
import java.util.*;
import cl.santotomas.restaurantevolcan.R;
import cl.santotomas.restaurantevolcan.databinding.ActivityProductsBinding;
import cl.santotomas.restaurantevolcan.ui.dashboard.DashboardActivity;
import cl.santotomas.restaurantevolcan.ui.movements.MovementsActivity;

public class ProductsActivity extends AppCompatActivity {
    private ActivityProductsBinding binding;
    private ProductRepository repository;
    private ProductAdapter adapter;
    private final List<ProductItem> displayList = new ArrayList<>();
    private EditText search;
    private final ActivityResultLauncher<ScanOptions> scanner = registerForActivityResult(
            new ScanContract(), result -> {
                if (result.getContents() != null) showBarcode(result.getContents());
            });
    private final ActivityResultLauncher<String> cameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) scan();
                else new AlertDialog.Builder(this).setTitle("Cámara sin autorización")
                        .setMessage("Puedes buscar ingresando el código de barras manualmente.")
                        .setPositiveButton("Búsqueda manual", (d, w) -> manualBarcode())
                        .setNegativeButton("Cerrar", null).show();
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        binding = ActivityProductsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = ProductRepository.getInstance(this);
        ArrayAdapter<CharSequence> categories = ArrayAdapter.createFromResource(
                this, R.array.product_categories, android.R.layout.simple_spinner_item);
        categories.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategory.setAdapter(categories);
        adapter = new ProductAdapter(displayList, this::showProductOptionsDialog);
        binding.recyclerProducts.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerProducts.setAdapter(adapter);
        LinearLayout filters = (LinearLayout) binding.buttonApplyFilter.getParent();
        search = new EditText(this);
        search.setHint("Buscar por nombre, SKU o código de barras");
        search.setSingleLine(true);
        filters.addView(search, 0);
        Button scan = new Button(this); scan.setText("Escanear código de barras");
        LinearLayout scanRow = new LinearLayout(this);
        filters.addView(scanRow);
        scanRow.addView(scan, new LinearLayout.LayoutParams(0, -2, 1));
        scan.setText("Escanear");
        scan.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) scan();
            else cameraPermission.launch(Manifest.permission.CAMERA);
        });
        Button manual = new Button(this); manual.setText("Ingresar código manualmente");
        scanRow.addView(manual, new LinearLayout.LayoutParams(0, -2, 1)); manual.setText("Código manual"); manual.setOnClickListener(v -> manualBarcode());
        binding.buttonApplyFilter.setOnClickListener(v -> loadProducts());
        binding.buttonAddProduct.setOnClickListener(v -> editProduct(null));
        binding.buttonBack.setOnClickListener(v -> finish());
        if (state != null) {
            search.setText(state.getString("query", ""));
            binding.spinnerCategory.setSelection(state.getInt("category"));
        }
        loadProducts();
    }
    @Override protected void onResume() { super.onResume(); if (repository != null) loadProducts(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("query", search.getText().toString());
        state.putInt("category", binding.spinnerCategory.getSelectedItemPosition());
    }
    private void loadProducts() {
        String category = binding.spinnerCategory.getSelectedItem().toString();
        String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        displayList.clear();
        for (ProductItem p : repository.getProducts()) {
            if (binding.spinnerCategory.getSelectedItemPosition() != 0 && !category.equals(p.getCategory())) continue;
            if (!(p.getName().toLowerCase(Locale.ROOT).contains(query)
                    || p.getCode().toLowerCase(Locale.ROOT).contains(query)
                    || p.getBarcode().toLowerCase(Locale.ROOT).contains(query))) continue;
            displayList.add(p);
        }
        adapter.notifyDataSetChanged();
    }
    private void scan() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.ONE_D_CODE_TYPES);
        options.setPrompt("Enfoca el código de barras");
        options.setBeepEnabled(false);
        options.setOrientationLocked(false);
        scanner.launch(options);
    }
    private void manualBarcode() {
        EditText input = new EditText(this);
        input.setHint("Código de barras");
        input.setSingleLine(true);
        new AlertDialog.Builder(this).setTitle("Buscar código de barras").setView(input)
                .setPositiveButton("Buscar", (d,w) -> showBarcode(input.getText().toString().trim()))
                .setNegativeButton("Cancelar", null).show();
    }
    private void showBarcode(String value) {
        if (value.isEmpty()) { message("Ingresa un código de barras."); return; }
        ProductItem p = repository.findByBarcode(value);
        if (p == null) { message("Producto no encontrado."); return; }
        AlertDialog.Builder dialog = new AlertDialog.Builder(this).setTitle(p.getName()).setMessage(
                "SKU: " + p.getCode() + "\nCategoría: " + p.getCategory()
                + "\nStock actual: " + p.getStock() + " " + p.getUnit()
                + "\nStock mínimo: " + p.getMinimumStock() + "\nEstado: " + p.getStockStatus()
                + "\nUbicación física: " + (p.getLocation().isEmpty() ? "Sin registrar" : p.getLocation())
                + (p.isAvailable() ? "" : "\nPRODUCTO DESHABILITADO"))
                .setNegativeButton("Cerrar", null);
        if (p.isAvailable() && getIntent().getStringExtra(DashboardActivity.EXTRA_USERNAME) != null) {
            dialog.setPositiveButton("Registrar entrada", (d,w) -> movement(p, false));
            dialog.setNeutralButton("Registrar salida", (d,w) -> movement(p, true));
        }
        dialog.show();
    }
    private void movement(ProductItem p, boolean exit) {
        Intent intent = new Intent(this, MovementsActivity.class);
        intent.putExtra(DashboardActivity.EXTRA_USERNAME, getIntent().getStringExtra(DashboardActivity.EXTRA_USERNAME));
        intent.putExtra("product_code", p.getCode()); intent.putExtra("exit", exit);
        startActivity(intent);
    }
    private void showProductOptionsDialog(ProductItem p) {
        new AlertDialog.Builder(this).setTitle(p.getName() + " (" + p.getCode() + ")")
                .setItems(new String[]{"Editar producto", "Registrar entrada", "Registrar salida",
                        p.isAvailable() ? "Deshabilitar" : "Habilitar"}, (d,w) -> {
                    if (w == 0) editProduct(p);
                    else if (w == 1 || w == 2) {
                        if (!p.isAvailable()) message("El producto está deshabilitado.");
                        else movement(p, w == 2);
                    } else new AlertDialog.Builder(this).setMessage("¿Confirmas el cambio de estado?")
                            .setPositiveButton("Confirmar", (a,b) -> {
                                repository.setAvailable(p.getCode(), !p.isAvailable()); loadProducts();
                            }).setNegativeButton("Cancelar", null).show();
                }).show();
    }
    private EditText field(LinearLayout form, String label, String value, boolean number) {
        TextView caption = new TextView(this); caption.setText(label); form.addView(caption);
        EditText field = new EditText(this); field.setText(value); field.setSingleLine(true);
        field.setInputType(number ? InputType.TYPE_CLASS_NUMBER : InputType.TYPE_CLASS_TEXT);
        form.addView(field); return field;
    }
    private void editProduct(ProductItem item) {
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(28, 12, 28, 12);
        ScrollView scroll = new ScrollView(this); scroll.addView(form);
        EditText code = field(form, "SKU *", item == null ? "" : item.getCode(), false);
        code.setEnabled(item == null);
        EditText name = field(form, "Nombre *", item == null ? "" : item.getName(), false);
        EditText desc = field(form, "Descripción", item == null ? "" : item.getDescription(), false);
        TextView categoryLabel = new TextView(this);
        categoryLabel.setText("Categoría *"); form.addView(categoryLabel);
        Spinner category = new Spinner(this);
        List<String> cats = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.product_categories)));
        cats.remove(0);
        if (item != null && !cats.contains(item.getCategory())) cats.add(item.getCategory());
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cats);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        category.setAdapter(catAdapter); form.addView(category);
        if (item != null) category.setSelection(cats.indexOf(item.getCategory()));
        EditText unit = field(form, "Unidad de medida *", item == null ? "" : item.getUnit(), false);
        EditText stock = field(form, item == null ? "Stock inicial *" : "Stock actual (se cambia con movimientos)", item == null ? "0" : "" + item.getStock(), true);
        stock.setEnabled(item == null); // Later stock changes must leave a movement.
        EditText minimum = field(form, "Stock mínimo *", item == null ? "5" : "" + item.getMinimumStock(), true);
        EditText barcode = field(form, "Código de barras (opcional, único)", item == null ? "" : item.getBarcode(), false);
        EditText location = field(form, "Ubicación física (opcional)", item == null ? "" : item.getLocation(), false);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(item == null ? "Nuevo producto" : "Editar producto")
                .setView(scroll).setPositiveButton("Guardar", null).setNegativeButton("Cancelar", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (code.getText().toString().trim().isEmpty() || name.getText().toString().trim().isEmpty()
                    || unit.getText().toString().trim().isEmpty()) { message("Completa SKU, nombre y unidad."); return; }
            try {
                int count = item == null ? Integer.parseInt(stock.getText().toString()) : item.getStock();
                int min = Integer.parseInt(minimum.getText().toString());
                if (count < 0 || min < 0) throw new NumberFormatException();
                ProductItem p = new ProductItem(code.getText().toString().trim(), name.getText().toString().trim(),
                        desc.getText().toString().trim(), category.getSelectedItem().toString(),
                        unit.getText().toString().trim(), count, item == null || item.isAvailable());
                p.setMinimumStock(min); p.setBarcode(barcode.getText().toString()); p.setLocation(location.getText().toString());
                repository.saveProduct(p, item == null);
                dialog.dismiss(); loadProducts(); message("Producto guardado.");
            } catch (NumberFormatException e) { message("Stock y mínimo deben ser enteros entre 0 y 2147483647."); }
            catch (SQLiteException e) { message("No se pudo guardar: verifica que SKU y código de barras sean únicos."); }
        }));
        dialog.show();
    }
    private void message(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }
}
