package com.ensono.utility;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * Utility class used to set time limits and check if a time limit has been reached
 */
public class TimeLimit {

    private Instant end;
    private final Duration time;

    /**
     * Create a TimeLimit in seconds
     *
     * @param seconds - Amount of seconds in this limit
     */
    public TimeLimit(int seconds) {
        this.time = Duration.ofSeconds(seconds);
        reset();
    }

    /**
     * Create a TimeLimit with a specified {@link Duration}
     *
     * @param time {@link Duration} to set as the limit
     */
    public TimeLimit(Duration time) {
        this.time = time;
        reset();
    }

    public static TimeLimit of(Duration time) {
        return new TimeLimit(time);
    }

    /**
     * Resets the timer
     */
    public void reset() {
        end = Clock.systemDefaultZone().instant().plus(time);
    }

    /**
     * Checks to see whether the time limit has been reached
     *
     * @return true if the limit has not yet been reached
     */
    public boolean timeLeft() {
        return !end.isBefore(Clock.systemDefaultZone().instant());
    }

    /**
     * Checks to see whether the time limit has been reached, if it has been reached, an {@link TimeLimitReachedError} is thrown
     *
     * @return true if the limit has been reached
     * @throws TimeLimitReachedError if the time limit has been reached
     */
    public boolean timeLeftElseThrow() {
        if (!timeLeft())
            throw new TimeLimitReachedError("The specified time limit of %d seconds has been reached", time.toSeconds());
        return true;
    }

    /**
     * Given a {@link Supplier} which supplies a Boolean value, this function will continue to invoke the supplier method until it returns false,
     * or until the timelimit has been reached where an error will be thrown {@link #timeLeftElseThrow()}
     *
     * @param method {@link Supplier} which must return a Boolean value. i.e. return true if the method needs to be called again as X process has not finished or reached the desired result
     */
    public void doWhileTrue(Supplier<Boolean> method) {
        do timeLeftElseThrow();
        while (method.get());
    }

    public static class TimeLimitReachedError extends Error {

        public TimeLimitReachedError(String s, Object... format) {
            super(String.format(s, format));
        }

    }

}