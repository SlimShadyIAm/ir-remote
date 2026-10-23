package com.slimshady.irsdk;

import android.content.Context;
import android.hardware.ConsumerIrManager;

/** Uses Android's public Consumer IR API; no root or vendor APIs required. */
public final class IrTransmitter {
    private final ConsumerIrManager manager;
    public IrTransmitter(Context context) {
        manager = (ConsumerIrManager) context.getSystemService(Context.CONSUMER_IR_SERVICE);
    }
    public boolean isAvailable() { return manager != null && manager.hasIrEmitter(); }
    public ConsumerIrManager.CarrierFrequencyRange[] getSupportedRanges() {
        return isAvailable() ? manager.getCarrierFrequencies() : null;
    }
    public void transmit(IrSignal signal) {
        if (!isAvailable()) throw new IllegalStateException("This device does not expose an IR emitter to Android apps.");
        ConsumerIrManager.CarrierFrequencyRange[] ranges = getSupportedRanges();
        if (ranges != null && ranges.length > 0) {
            boolean supported = false;
            for (ConsumerIrManager.CarrierFrequencyRange range : ranges)
                supported |= signal.frequencyHz >= range.getMinFrequency() && signal.frequencyHz <= range.getMaxFrequency();
            if (!supported) throw new IllegalArgumentException("Carrier frequency is outside this device's supported ranges.");
        }
        manager.transmit(signal.frequencyHz, signal.getPattern());
    }
}
