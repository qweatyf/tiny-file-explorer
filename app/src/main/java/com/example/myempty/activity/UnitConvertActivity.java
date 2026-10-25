package com.example.myempty.activity2;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class UnitConvertActivity extends AppCompatActivity {

    private Spinner spType;
    private Spinner spFrom;
    private Spinner spTo;
    private EditText etValue;
    private TextView tvResult;

    private String[] types;
    private String[][] units;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_unit_convert);

        types = getResources().getStringArray(R.array.unit_types);
        units = new String[][] {
            getResources().getStringArray(R.array.unit_length),
            getResources().getStringArray(R.array.unit_area),
            getResources().getStringArray(R.array.unit_volume),
            getResources().getStringArray(R.array.unit_weight),
            getResources().getStringArray(R.array.unit_temp)
        };

        spType = findViewById(R.id.sp_unit_type);
        spFrom = findViewById(R.id.sp_unit_from);
        spTo = findViewById(R.id.sp_unit_to);
        etValue = findViewById(R.id.et_unit_value);
        tvResult = findViewById(R.id.tv_unit_result);

        spType.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, types));

        spType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                setUnits(pos);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        setUnits(0);
        etValue.setText("1");

        findViewById(R.id.btn_unit_convert).setOnClickListener(v -> convert());
    }

    private void setUnits(int typeIdx) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, units[typeIdx]);
        spFrom.setAdapter(a);
        spTo.setAdapter(a);
        spFrom.setSelection(0);
        spTo.setSelection(Math.min(1, units[typeIdx].length - 1));
    }

    private void convert() {
        double v;
        try { v = Double.parseDouble(etValue.getText().toString().trim()); }
        catch (Exception e) {
            Toast.makeText(this, getString(R.string.file_enc_none),
                Toast.LENGTH_SHORT).show();
            return;
        }

        int type = spType.getSelectedItemPosition();
        String from = spFrom.getSelectedItem().toString();
        String to = spTo.getSelectedItem().toString();

        try {
            double base = toBase(v, type, from);
            double result = fromBase(base, type, to);
            tvResult.setText(v + " " + from + " = " + format(result) + " " + to);
        } catch (Exception e) {
            tvResult.setText(getString(R.string.file_fail) + e.getMessage());
        }
    }

    private double toBase(double v, int type, String unit) {
        switch (type) {
            case 0:
                switch (unit) {
                    case "mm": return v / 1000;
                    case "cm": return v / 100;
                    case "dm": return v / 10;
                    case "m": return v;
                    case "km": return v * 1000;
                    case "英寸": case "in": return v * 0.0254;
                    case "英尺": case "ft": return v * 0.3048;
                    case "英里": case "mi": return v * 1609.344;
                }
                break;
            case 1:
                switch (unit) {
                    case "mm²": return v / 1000000;
                    case "cm²": return v / 10000;
                    case "m²": return v;
                    case "km²": return v * 1000000;
                    case "公顷": case "ha": return v * 10000;
                    case "亩": return v * 666.667;
                    case "平方英尺": case "ft²": return v * 0.092903;
                }
                break;
            case 2:
                switch (unit) {
                    case "mL": return v / 1000;
                    case "L": return v;
                    case "m³": return v * 1000;
                    case "加仑(US)": return v * 3.78541;
                    case "加仑(UK)": return v * 4.54609;
                }
                break;
            case 3:
                switch (unit) {
                    case "mg": return v / 1000000;
                    case "g": return v / 1000;
                    case "kg": return v;
                    case "吨": case "t": return v * 1000;
                    case "磅": case "lb": return v * 0.453592;
                    case "盎司": case "oz": return v * 0.0283495;
                }
                break;
            case 4:
                switch (unit) {
                    case "°C": return v;
                    case "°F": return (v - 32) * 5 / 9;
                    case "K": return v - 273.15;
                }
                break;
        }
        throw new RuntimeException(unit);
    }

    private double fromBase(double base, int type, String unit) {
        switch (type) {
            case 0:
                switch (unit) {
                    case "mm": return base * 1000;
                    case "cm": return base * 100;
                    case "dm": return base * 10;
                    case "m": return base;
                    case "km": return base / 1000;
                    case "英寸": case "in": return base / 0.0254;
                    case "英尺": case "ft": return base / 0.3048;
                    case "英里": case "mi": return base / 1609.344;
                }
                break;
            case 1:
                switch (unit) {
                    case "mm²": return base * 1000000;
                    case "cm²": return base * 10000;
                    case "m²": return base;
                    case "km²": return base / 1000000;
                    case "公顷": case "ha": return base / 10000;
                    case "亩": return base / 666.667;
                    case "平方英尺": case "ft²": return base / 0.092903;
                }
                break;
            case 2:
                switch (unit) {
                    case "mL": return base * 1000;
                    case "L": return base;
                    case "m³": return base / 1000;
                    case "加仑(US)": return base / 3.78541;
                    case "加仑(UK)": return base / 4.54609;
                }
                break;
            case 3:
                switch (unit) {
                    case "mg": return base * 1000000;
                    case "g": return base * 1000;
                    case "kg": return base;
                    case "吨": case "t": return base / 1000;
                    case "磅": case "lb": return base / 0.453592;
                    case "盎司": case "oz": return base / 0.0283495;
                }
                break;
            case 4:
                switch (unit) {
                    case "°C": return base;
                    case "°F": return base * 9 / 5 + 32;
                    case "K": return base + 273.15;
                }
                break;
        }
        throw new RuntimeException(unit);
    }

    private String format(double v) {
        if (v == Math.floor(v) && Math.abs(v) < 1e15) {
            return String.valueOf((long) v);
        }
        return String.format("%.6f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}