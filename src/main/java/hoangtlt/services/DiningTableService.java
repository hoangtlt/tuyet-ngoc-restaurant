package hoangtlt.services;

import hoangtlt.entities.DiningTable;
import hoangtlt.repositories.DiningTableRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class DiningTableService {
    private final DiningTableRepository diningTableRepository;

    public DiningTableService(DiningTableRepository diningTableRepository) {
        this.diningTableRepository = diningTableRepository;
    }

    public List<DiningTable> getAllTables() {
        return diningTableRepository.findAll();
    }

    public List<DiningTable> getTablesByArea(Long areaId) {
        return diningTableRepository.findByAreaId(areaId);
    }

    public Optional<DiningTable> getTableById(Long id) {
        return diningTableRepository.findById(id);
    }

    public DiningTable saveTable(DiningTable table) {
        return diningTableRepository.save(table);
    }

    public void deleteTable(Long id) {
        diningTableRepository.deleteById(id);
    }

    public void updateStatus(Long tableId, DiningTable.TableStatus status) {
        diningTableRepository.findById(tableId).ifPresent(table -> {
            table.setStatus(status);
            diningTableRepository.save(table);
        });
    }
}
