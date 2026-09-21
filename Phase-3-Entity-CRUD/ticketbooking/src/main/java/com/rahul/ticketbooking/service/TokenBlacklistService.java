package com.rahul.ticketbooking.service;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final Set<String> revokedIds = ConcurrentHashMap.newKeySet();

    public void blacklist(String jti) {
        revokedIds.add(jti);
    }

    public boolean isBlacklisted(String jti) {
        return revokedIds.contains(jti);
    }
}