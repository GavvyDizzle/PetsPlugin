package com.github.gavvydizzle.petsplugin.pets.xp;

import com.github.gavvydizzle.petsplugin.pets.Pet;

/**
 * Simple wrapper for when a pet defines an entry that is not in the global space.
 */
public class MissingExperienceEntry {

    private final Pet pet;
    private final ExperienceType experienceType;
    private final Object o;

    public MissingExperienceEntry(Pet pet, ExperienceType experienceType, Object o) {
        this.pet = pet;
        this.experienceType = experienceType;
        this.o = o;
    }

    @Override
    public String toString() {
        return "The pet with id '" + pet.getId() + "' defines the xp entry '" + experienceType.name() + ":" + o.toString() + "' but it does not exist in the global space";
    }
}
