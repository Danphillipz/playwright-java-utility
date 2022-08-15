package com.ensono.utility;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Utility class used to set time limits and check if a time limit has been reached
 * @author Daniel Phillips
 *
 */
public class TimeLimit {

    private Instant end;
    private Duration time;

    /**
     * Create a TimeLimit in seconds
     * @param seconds - Amount of seconds in this limit
     */
    public TimeLimit(int seconds) {
        time = Duration.ofSeconds(seconds);
        reset();
    }

    /**
     * Create a TimeLimit with a specified {@link Duration}
     * @param time {@link Duration} to set as the limit
     */
    public TimeLimit(Duration time) {
        end = Clock.systemDefaultZone().instant().plus(time);
    }

    /**
     * Resets the timer
     */
    public void reset() {
        end = Clock.systemDefaultZone().instant().plus(time);
    }

    /**
     * Checks to see whether the time limit has been reached
     * @return true if the limit has been reached
     */
    public boolean timeLeft() {
        return !end.isBefore(Clock.systemDefaultZone().instant());
    }

    /**
     * Checks to see whether the time limit has been reached, if it has been reached, an {@link Error} is thrown
     * @return true if the limit has been reached
     * @throws {@link TimeLimitReachedError} if the time limit has been reached
     */
    public boolean timeLeftElseThrow() {
        if(!timeLeft()) throw new TimeLimitReachedError("The specified time limit has been reached");
        return true;
    }

    public class TimeLimitReachedError extends Error {

        public TimeLimitReachedError(String s) {
            super(s);
        }

    }

}