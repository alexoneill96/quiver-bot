package com.quiverbot.domain.enums

/**
 * Categories for classifying trading signals from QuiverQuant tweets.
 */
enum class SignalCategory {
    /** Individual politician/insider trade disclosure */
    DIRECT_TRADE,

    /** Market-wide or sector trends in trading activity */
    AGGREGATE_TREND,

    /** Legislative/regulatory signals that may impact markets */
    POLICY_SIGNAL,

    /** General updates, promotions, non-actionable content */
    LOW_SIGNAL
}
