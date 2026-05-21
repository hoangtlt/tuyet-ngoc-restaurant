package hoangtlt.services;

import hoangtlt.entities.Area;
import hoangtlt.repositories.AreaRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class AreaService {
    private final AreaRepository areaRepository;

    public AreaService(AreaRepository areaRepository) {
        this.areaRepository = areaRepository;
    }

    public List<Area> getAllAreas() {
        return areaRepository.findAll();
    }

    public Optional<Area> getAreaById(Long id) {
        return areaRepository.findById(id);
    }

    public Area saveArea(Area area) {
        return areaRepository.save(area);
    }

    public void deleteArea(Long id) {
        areaRepository.deleteById(id);
    }
}
