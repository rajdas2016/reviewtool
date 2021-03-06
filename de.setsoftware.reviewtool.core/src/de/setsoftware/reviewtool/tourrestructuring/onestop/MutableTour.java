package de.setsoftware.reviewtool.tourrestructuring.onestop;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.base.Util;
import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.model.changestructure.Tour;

/**
 * A helper class for restructuring the tours, containing the information like a tour,
 * but being mutable instead.
 */
class MutableTour {

    /**
     * Helper class containing data about a stop and its position in a (mutable) tour.
     */
    private static class StopInTour {

        private final MutableTour tour;
        private final int index;

        public StopInTour(MutableTour tour, int stopIndex) {
            this.tour = tour;
            this.index = stopIndex;
        }

        public void merge(Stop s) {
            this.tour.mergeStop(this.index, s);
        }

    }

    private final Set<String> descriptionParts;
    private final List<Stop> stops;

    public MutableTour(Tour t) {
        this.descriptionParts = new LinkedHashSet<>();
        this.descriptionParts.add(t.getDescription());
        this.stops = new ArrayList<>(t.getStops());
    }

    public static List<Tour> toTours(List<MutableTour> mutableTours) {
        final List<Tour> ret = new ArrayList<>();
        for (final MutableTour t : mutableTours) {
            ret.add(new Tour(
                    Util.implode(t.descriptionParts, " + "),
                    t.stops));
        }
        return ret;
    }

    public boolean canBeResolvedCompletely(List<MutableTour> mutableTours, int excludedIndex) {
        for (final Stop s : this.stops) {
            if (!this.canBeMerged(s, mutableTours, excludedIndex)) {
                return false;
            }
        }
        return true;
    }

    private boolean canBeMerged(Stop s, List<MutableTour> mutableTours, int excludedIndex) {
        return this.getStopToMergeWith(s, mutableTours, excludedIndex) != null;
    }

    public boolean resolve(List<MutableTour> mutableTours, int excludedIndex) {
        boolean didSomething = false;
        final Iterator<Stop> iter = this.stops.iterator();
        while (iter.hasNext()) {
            final Stop s = iter.next();
            final StopInTour toMergeWith = this.getStopToMergeWith(s, mutableTours, excludedIndex);
            if (toMergeWith != null) {
                toMergeWith.merge(s);
                toMergeWith.tour.descriptionParts.addAll(this.descriptionParts);
                iter.remove();
                didSomething = true;
            }
        }
        return didSomething;
    }

    private StopInTour getStopToMergeWith(Stop s, List<MutableTour> mutableTours, int excludedIndex) {
        for (int tourIndex = excludedIndex + 1; tourIndex < mutableTours.size(); tourIndex++) {
            final StopInTour mergeWith = mutableTours.get(tourIndex).getStopToMergeWith(s);
            if (mergeWith != null) {
                return mergeWith;
            }
        }
        for (int tourIndex = excludedIndex - 1; tourIndex >= 0; tourIndex--) {
            final StopInTour mergeWith = mutableTours.get(tourIndex).getStopToMergeWith(s);
            if (mergeWith != null) {
                return mergeWith;
            }
        }
        return null;
    }

    private StopInTour getStopToMergeWith(Stop s) {
        for (int stopIndex = 0; stopIndex < this.stops.size(); stopIndex++) {
            if (this.stops.get(stopIndex).canBeMergedWith(s)) {
                return new StopInTour(this, stopIndex);
            }
        }
        return null;
    }

    public void mergeStop(int index, Stop s) {
        this.stops.set(index, this.stops.get(index).merge(s));
    }

    public boolean isEmpty() {
        return this.stops.isEmpty();
    }
}
