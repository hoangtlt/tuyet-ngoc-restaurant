package hoangtlt.services;

import hoangtlt.entities.RestaurantSetting;
import hoangtlt.repositories.RestaurantSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantSettingService {

    private final RestaurantSettingRepository repository;

    public RestaurantSettingService(RestaurantSettingRepository repository) {
        this.repository = repository;
    }

    public RestaurantSetting getSettings() {
        return repository.findById(1L).orElseGet(() -> {
            RestaurantSetting defaultSetting = new RestaurantSetting();
            return repository.save(defaultSetting);
        });
    }

    @Transactional
    public void saveSettings(RestaurantSetting settings) {
        settings.setId(1L);
        repository.save(settings);
    }
}
