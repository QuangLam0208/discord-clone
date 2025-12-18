package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.jpa.Subscription;
import hcmute.edu.vn.discord.repository.SubscriptionRepository;
import hcmute.edu.vn.discord.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Override
    public Subscription createSubscription(Subscription subscription) {
        subscription.setId(null);
        if (subscription.getStartDate() == null) {
            subscription.setStartDate(new Date());
        }
        return subscriptionRepository.save(subscription);
    }

    @Override
    public Subscription updateSubscription(Long id, Subscription updated) {
        Subscription existing = subscriptionRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Subscription not found with id " + id));

        // update allowed fields
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        existing.setUser(updated.getUser());

        return subscriptionRepository.save(existing);
    }

    @Override
    public void cancelSubscription(Long id) {
        Subscription existing = subscriptionRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Subscription not found with id " + id));

        existing.setEndDate(new Date());
        subscriptionRepository.save(existing);
    }

    @Override
    public Optional<Subscription> getSubscriptionById(Long id) {
        return subscriptionRepository.findById(id);
    }

    @Override
    public Optional<Subscription> getActiveSubscriptionForUser(Long userId) {
        return subscriptionRepository.findByUserIdAndEndDateAfter(userId, new Date());
    }

    @Override
    public List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }
}

