package com.slimshady.irremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.hardware.ConsumerIrManager;
import android.text.InputType;
import android.view.View;
import android.view.WindowInsets;
import android.widget.*;
import com.slimshady.irsdk.*;
import org.json.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private IrTransmitter transmitter;
    private JSONArray buttons;
    private LinearLayout content;
    private boolean busy;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private static final String STORE = "buttons";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        transmitter = new IrTransmitter(this);
        try { buttons = new JSONArray(getPreferences(0).getString(STORE, "[]")); }
        catch (JSONException e) { buttons = new JSONArray(); toast("Saved button data could not be read."); }
        render();
    }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density); }
    private TextView text(String value, int size) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size);
        view.setTextColor(Color.WHITE); view.setPadding(0, dp(8), 0, dp(8)); return view;
    }
    private Button button(String label, View.OnClickListener action) {
        Button view = new Button(this); view.setText(label); view.setAllCaps(false); view.setOnClickListener(action); return view;
    }
    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true); scroll.setBackgroundColor(Color.rgb(17, 23, 34));
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(20), dp(24), dp(24)); scroll.addView(content); setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()); return insets;
        });
        content.addView(text("IR Remote", 32));
        content.addView(text("Your controls. Your signals.", 16));
        String status;
        try {
            status = transmitter.isAvailable() ? "IR emitter available" : "No Android-accessible IR emitter detected. You can still create and edit buttons.";
            ConsumerIrManager.CarrierFrequencyRange[] ranges = transmitter.getSupportedRanges();
            if (ranges != null && ranges.length > 0) {
                status += "\nSupported carriers (Hz):";
                for (ConsumerIrManager.CarrierFrequencyRange r : ranges) status += "\n" + r.getMinFrequency() + " – " + r.getMaxFrequency();
            }
        } catch (RuntimeException e) { status = "Unable to query IR hardware: " + e.getMessage(); }
        content.addView(text(status, 14));
        content.addView(button("+ Add button", v -> edit(-1)));
        content.addView(text("Saved buttons", 22));
        if (buttons.length() == 0) content.addView(text("Add a control using your device's raw IR timings. Durations alternate ON, OFF in microseconds, starting with ON. This app cannot learn signals through the IR blaster.", 16));
        for (int i = 0; i < buttons.length(); i++) {
            final int index = i; JSONObject entry = buttons.optJSONObject(i); if (entry == null) continue;
            content.addView(text(entry.optString("name"), 20));
            content.addView(text(entry.optString("frequency") + " Hz · " + entry.optString("pattern").split("[\\s,;]+").length + " durations", 14));
            LinearLayout actions = new LinearLayout(this);
            Button send = button("Send", v -> send(index));
            try { send.setEnabled(transmitter.isAvailable()); } catch (RuntimeException e) { send.setEnabled(false); }
            actions.addView(send); actions.addView(button("Edit", v -> edit(index)));
            actions.addView(button("Delete", v -> new AlertDialog.Builder(this).setTitle("Delete button?")
                .setMessage("Remove " + entry.optString("name") + "?").setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) -> { buttons.remove(index); persist(); }).show()));
            content.addView(actions);
        }
    }
    private EditText field(LinearLayout form, String label, String value, boolean numeric) {
        form.addView(text(label, 15)); EditText input = new EditText(this); input.setText(value);
        input.setTextColor(Color.WHITE); input.setSingleLine(numeric);
        input.setInputType(numeric ? InputType.TYPE_CLASS_NUMBER : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        form.addView(input); return input;
    }
    private void edit(int index) {
        JSONObject old = index < 0 ? new JSONObject() : buttons.optJSONObject(index);
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(20), dp(8), dp(20), dp(8));
        EditText name = field(form, "Button name", old.optString("name"), false);
        EditText frequency = field(form, "Carrier frequency (Hz)", old.optString("frequency", "38000"), true);
        EditText pattern = field(form, "Raw timings (µs): ON, OFF, ON, OFF…", old.optString("pattern"), false);
        pattern.setMinLines(3); pattern.setHint("9000, 4500, 560, 560, …"); pattern.setHintTextColor(Color.LTGRAY);
        form.addView(text("Separate positive whole-number timings with spaces or commas. Total duration must be less than 2 seconds. Use timings for your target appliance.", 14));
        TextView error = text("", 14); error.setTextColor(Color.rgb(255, 160, 150)); form.addView(error);
        ScrollView scroll = new ScrollView(this); scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(index < 0 ? "New IR button" : "Edit IR button")
            .setView(scroll).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                String label = name.getText().toString().trim();
                if (label.isEmpty() || label.length() > 80) throw new IllegalArgumentException("Use a name between 1 and 80 characters.");
                IrSignal signal = IrSignal.parse(frequency.getText().toString(), pattern.getText().toString());
                StringBuilder normalized = new StringBuilder();
                for (int pulse : signal.getPattern()) { if (normalized.length() > 0) normalized.append(", "); normalized.append(pulse); }
                JSONObject entry = new JSONObject().put("name", label).put("frequency", signal.frequencyHz).put("pattern", normalized.toString());
                if (index < 0) buttons.put(entry); else buttons.put(index, entry);
                persist(); dialog.dismiss();
            } catch (IllegalArgumentException | JSONException e) { error.setText(e.getMessage()); }
        }));
        dialog.show();
    }
    private void send(int index) {
        if (busy) { toast("A signal is already being sent."); return; }
        JSONObject entry = buttons.optJSONObject(index);
        try {
            IrSignal signal = IrSignal.parse(entry.optString("frequency"), entry.optString("pattern"));
            busy = true;
            worker.execute(() -> {
                String result;
                try { transmitter.transmit(signal); result = "Signal submitted to IR hardware."; }
                catch (RuntimeException e) { result = "Could not send: " + e.getMessage(); }
                String message = result;
                runOnUiThread(() -> { busy = false; if (!isFinishing() && !isDestroyed()) toast(message); });
            });
        } catch (IllegalArgumentException e) { toast(e.getMessage()); }
    }
    private void persist() { getPreferences(0).edit().putString(STORE, buttons.toString()).apply(); render(); }
    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_LONG).show(); }
    @Override protected void onDestroy() { worker.shutdown(); super.onDestroy(); }
}
