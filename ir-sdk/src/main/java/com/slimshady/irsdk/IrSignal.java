package com.slimshady.irsdk;

/** A carrier in Hz and alternating on/off durations in microseconds. */
public final class IrSignal {
    public final int frequencyHz;
    private final int[] pattern;
    public IrSignal(int frequencyHz, int[] pattern) {
        if (frequencyHz <= 0) throw new IllegalArgumentException("Frequency must be positive (Hz).");
        if (pattern == null || pattern.length == 0 || pattern.length > 10000)
            throw new IllegalArgumentException("Provide 1–10,000 pulse durations.");
        long total = 0;
        for (int pulse : pattern) {
            if (pulse <= 0) throw new IllegalArgumentException("Every duration must be positive (µs).");
            total += pulse;
        }
        if (total >= 2000000) throw new IllegalArgumentException("Signal must be shorter than two seconds.");
        this.frequencyHz = frequencyHz;
        this.pattern = pattern.clone();
    }
    public int[] getPattern() { return pattern.clone(); }
    public static IrSignal parse(String frequency, String pulses) {
        try {
            String[] parts = pulses.trim().split("[\\s,;]+");
            int[] pattern = new int[parts.length];
            for (int i = 0; i < parts.length; i++) pattern[i] = Integer.parseInt(parts[i]);
            return new IrSignal(Integer.parseInt(frequency.trim()), pattern);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Use whole numbers: frequency in Hz and durations in µs.");
        }
    }
}
