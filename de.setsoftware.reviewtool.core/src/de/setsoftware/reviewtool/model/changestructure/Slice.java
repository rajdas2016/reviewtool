package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.List;

/**
 * A slice of a unit of work that is in some way cohesive, for example all changes that belong to a certain
 * sub task.
 */
public class Slice {

    private final String description;
    private final List<Fragment> fragments = new ArrayList<>();

    public Slice(String description, List<Fragment> list) {
        this.description = description;
        this.fragments.addAll(list);
    }

    @Override
    public String toString() {
        return "Slice: " + this.description + ", " + this.fragments;
    }

    public List<Fragment> getFragments() {
        return this.fragments;
    }

    public String getDescription() {
        return this.description;
    }

}