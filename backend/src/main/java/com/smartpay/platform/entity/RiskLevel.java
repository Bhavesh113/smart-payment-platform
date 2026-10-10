package com.smartpay.platform.entity;

/** Ordered by severity: the ordinal is used to pick the highest level among triggered rules. */
public enum RiskLevel {
    LOW, MEDIUM, HIGH, BLOCKED
}
