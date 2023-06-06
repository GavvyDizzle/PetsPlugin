package com.github.gavvydizzle.petsplugin.pets;

public class SelectedPet {

    private final String petID;
    private long xp;

    public SelectedPet(String petID, long xp) {
        this.petID = petID;
        this.xp = xp;
    }

    public String getPetID() {
        return petID;
    }

    public long getXp() {
        return xp;
    }

    public void addXP(long amount) {
        this.xp += amount;
    }

    @Override
    public String toString() {
        return "SelectedPet(id=" + petID + " xp=" + xp + ")";
    }
}
