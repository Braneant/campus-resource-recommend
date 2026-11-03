package com.campus.recommend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RecommendationInvalidator {
    @Autowired private RecommendService recommendations;

    @TransactionalEventListener(fallbackExecution = true)
    public void refresh(ResourceDataChanged event) {
        recommendations.invalidateSnapshot();
    }
}
