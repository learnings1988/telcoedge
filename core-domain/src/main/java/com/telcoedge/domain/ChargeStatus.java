package com.telcoedge.domain;

public enum ChargeStatus {
    CHARGED,
    INSUFFICIENT_BALANCE,
    DUPLICATE,
    SUBSCRIBER_NOT_FOUND,
    NO_ACTIVE_PLAN_FOUND,
    NO_RATE_FOUND_FOR_ACTIVE_PLAN
}
