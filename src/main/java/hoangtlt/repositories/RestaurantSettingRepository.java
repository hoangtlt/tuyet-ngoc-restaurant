package hoangtlt.repositories;

import hoangtlt.entities.RestaurantSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantSettingRepository extends JpaRepository<RestaurantSetting, Long> {
}
