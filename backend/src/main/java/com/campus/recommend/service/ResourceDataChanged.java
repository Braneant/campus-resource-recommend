package com.campus.recommend.service;

/** Published inside a write transaction; consumers refresh only after it commits. */
public record ResourceDataChanged() {}
