package cl.santotomas.restaurantevolcan.ui.location;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import cl.santotomas.restaurantevolcan.ui.common.Screen;

/** Uses a fixed company address and a one-shot foreground location request. */
public class LocationActivity extends Screen {
    public static final String COMPANY_ADDRESS = "Arauco 126, 1400099 Ovalle, Coquimbo, Chile";
    private TextView current;
    private LocationManager manager;
    private CancellationSignal cancellation;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean routeRequested;
    private final Runnable timeout = () -> {
        cancelRequest();
        current.setText("No se pudo obtener la ubicación. Comprueba el GPS e intenta al aire libre.");
    };
    private final ActivityResultLauncher<String[]> permissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (hasLocationPermission()) locate();
                else {
                    current.setText("Ubicación no autorizada. El mapa de la empresa sigue disponible.");
                    new AlertDialog.Builder(this).setMessage(
                            "Puedes autorizar ubicación en Ajustes de la aplicación o continuar viendo el local.")
                            .setPositiveButton("Ajustes", (d,w) -> startActivity(new Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))))
                            .setNegativeButton("Cerrar", null).show();
                }
            });
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        screen("Ubicación de la empresa");
        text("Volcán Waffles & Café");
        text(COMPANY_ADDRESS);
        text("El mapa y la ruta se abren en Google Maps o en el navegador.");
        button("Ver empresa en Google Maps", v -> open(new Uri.Builder()
                .scheme("https").authority("www.google.com").path("/maps/search/")
                .appendQueryParameter("api", "1").appendQueryParameter("query", COMPANY_ADDRESS).build()));
        current = text("Pulsa un botón para solicitar tu ubicación. No se guarda en la aplicación.");
        button("Obtener mi ubicación", v -> requestLocation(false));
        button("Cómo llegar a la empresa", v -> requestLocation(true));
        button("Volver al menú", v -> finish());
        manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (state != null) routeRequested = state.getBoolean("route");
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putBoolean("route", routeRequested);
    }
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    private void requestLocation(boolean route) {
        routeRequested = route;
        if (hasLocationPermission()) locate();
        else permissions.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
    }
    private void locate() {
        cancelRequest();
        boolean precise = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        String provider = precise && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
        if (!manager.isLocationEnabled() || !manager.isProviderEnabled(provider)) {
            current.setText("Activa los servicios de ubicación para obtener tu posición.");
            new AlertDialog.Builder(this).setMessage("Activa la ubicación y vuelve a pulsar el botón.")
                    .setPositiveButton("Activar ubicación", (d,w) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton("Cerrar", null).show();
            return;
        }
        current.setText("Obteniendo ubicación actual…");
        CancellationSignal signal = new CancellationSignal();
        cancellation = signal;
        handler.postDelayed(timeout, 25000);
        try {
            manager.getCurrentLocation(provider, signal, getMainExecutor(), location -> {
                if (signal != cancellation || signal.isCanceled()) return;
                handler.removeCallbacks(timeout);
                cancellation = null;
                if (location == null) { current.setText("Ubicación no disponible. Intenta nuevamente."); return; }
                String origin = location.getLatitude() + "," + location.getLongitude();
                current.setText("Latitud: " + location.getLatitude() + "\nLongitud: " + location.getLongitude()
                        + "\nPrecisión aproximada: " + Math.round(location.getAccuracy()) + " m");
                if (routeRequested) open(new Uri.Builder().scheme("https").authority("www.google.com")
                        .path("/maps/dir/").appendQueryParameter("api", "1")
                        .appendQueryParameter("origin", origin).appendQueryParameter("destination", COMPANY_ADDRESS)
                        .appendQueryParameter("travelmode", "driving").build());
            });
        } catch (SecurityException | IllegalArgumentException e) {
            cancelRequest(); current.setText("No se pudo acceder a la ubicación. Revisa permisos y servicios.");
        }
    }
    private void open(Uri uri) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
        catch (ActivityNotFoundException e) { message("Instala Google Maps o un navegador para abrir el mapa."); }
    }
    private void cancelRequest() {
        handler.removeCallbacks(timeout);
        if (cancellation != null) { cancellation.cancel(); cancellation = null; }
    }
    @Override protected void onStop() {
        super.onStop();
        if (cancellation != null) {
            cancelRequest();
            current.setText("Consulta interrumpida. Pulsa nuevamente para obtener tu ubicación.");
        }
    }
}
