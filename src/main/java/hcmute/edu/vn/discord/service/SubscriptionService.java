package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.entity.jpa.Subscription;

import java.util.List;
import java.util.Optional;

public interface SubscriptionService {
    Subscription createSubscription(Subscription subscription);
    Subscription updateSubscription(Long id, Subscription subscription);
    void cancelSubscription(Long id);
    Optional<Subscription> getSubscriptionById(Long id);
    Optional<Subscription> getActiveSubscriptionForUser(Long userId);
    List<Subscription> getAllSubscriptions();
}

