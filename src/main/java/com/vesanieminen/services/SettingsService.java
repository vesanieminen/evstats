package com.vesanieminen.services;

import com.vesanieminen.model.Setting;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

    static final String KEY_SCHEDULED_FETCH_ENABLED = "usedEvs.scheduledFetchEnabled";

    private final SettingRepository repository;

    public SettingsService(SettingRepository repository) {
        this.repository = repository;
    }

    public boolean isScheduledFetchEnabled() {
        return repository.findById(KEY_SCHEDULED_FETCH_ENABLED)
                .map(s -> Boolean.parseBoolean(s.getValue()))
                .orElse(true);
    }

    @Transactional
    public void setScheduledFetchEnabled(boolean enabled) {
        Setting s = repository.findById(KEY_SCHEDULED_FETCH_ENABLED)
                .orElseGet(() -> new Setting(KEY_SCHEDULED_FETCH_ENABLED, ""));
        s.setValue(Boolean.toString(enabled));
        repository.save(s);
    }
}
