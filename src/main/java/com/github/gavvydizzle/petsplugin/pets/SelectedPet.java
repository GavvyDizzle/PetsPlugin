package com.github.gavvydizzle.petsplugin.pets;

import java.util.Comparator;

public class SelectedPet implements Comparator<SelectedPet> {

    private final String petID;
    private double xp;
    private long lastUseTime;

    public SelectedPet(String petID, double xp, long lastUseTime) {
        this.petID = petID;
        this.xp = xp;
        this.lastUseTime = lastUseTime;
    }

    public String getPetID() {
        return petID;
    }

    public double getXp() {
        return xp;
    }

    public void addXP(double amount) {
        this.xp += amount;
    }

    public long getLastUseTime() {
        return lastUseTime;
    }

    public void setLastUseTime() {
        this.lastUseTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "SelectedPet(id=" + petID + " xp=" + xp + " lastUse=" + lastUseTime + ")";
    }

    @Override
    public int compare(SelectedPet o1, SelectedPet o2) {
        if (o1 == null && o2 == null) return 0;
        else if (o1 == null) return -1;
        else if (o2 == null) return 1;

        if (!o1.petID.equals(o2.petID)) {
            return o1.petID.compareTo(o2.petID);
        }
        return Double.compare(o1.xp, o2.xp);
    }
}
