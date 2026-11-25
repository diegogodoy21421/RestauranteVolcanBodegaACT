package cl.santotomas.restaurantevolcan.ui.common;

import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import cl.santotomas.restaurantevolcan.R;

/** Small shared layout for the new modules, using the existing app palette. */
public abstract class Screen extends AppCompatActivity {
    protected LinearLayout body;
    protected void screen(String title) {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(getColor(R.color.volcan_background));
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        body.setPadding(pad, pad, pad, pad);
        scroll.addView(body);
        setContentView(scroll);
        TextView heading = text(title);
        heading.setTextSize(26);
        heading.setTextColor(getColor(R.color.volcan_primary));
    }
    protected TextView text(String value) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(getColor(R.color.volcan_text_primary));
        view.setTextSize(16);
        view.setPadding(0, 12, 0, 12);
        body.addView(view);
        return view;
    }
    protected EditText field(String hint, boolean number) {
        EditText view = new EditText(this);
        view.setHint(hint);
        view.setSingleLine(true);
        view.setInputType(number ? InputType.TYPE_CLASS_NUMBER : InputType.TYPE_CLASS_TEXT);
        body.addView(view, new LinearLayout.LayoutParams(-1, -2));
        return view;
    }
    protected Spinner spinner(String label, java.util.List<String> values) {
        text(label);
        Spinner view = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        view.setAdapter(adapter);
        body.addView(view, new LinearLayout.LayoutParams(-1, Math.round(52 * getResources().getDisplayMetrics().density)));
        return view;
    }
    protected Button button(String label, View.OnClickListener listener) {
        Button view = new Button(this);
        view.setText(label);
        view.setOnClickListener(listener);
        body.addView(view, new LinearLayout.LayoutParams(-1, -2));
        return view;
    }
    protected void message(String value) { Toast.makeText(this, value, Toast.LENGTH_LONG).show(); }
}
