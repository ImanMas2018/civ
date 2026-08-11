package civ.model;

import java.util.ArrayList;
import java.util.List;

/** Mill, mine, farm, stable, settlement — output scales with stationed workers. */
public class ProductionBuilding extends Building {

    private final List<Worker> workers = new ArrayList<>();

    public ProductionBuilding(BuildingType type, Hex hex) {
        super(type, hex);
    }

    public List<Worker> getWorkers() {
        return workers;
    }

    public boolean hasRoom() {
        return workers.size() < getType().getWorkerCapacity();
    }

    public void addWorker(Worker worker) {
        workers.add(worker);
        worker.setStation(this);
    }

    public void removeWorker(Worker worker) {
        workers.remove(worker);
        worker.setStation(null);
    }

    public void releaseAllWorkers() {
        for (Worker worker : new ArrayList<>(workers)) {
            removeWorker(worker);
        }
    }

    @Override
    public int outputPerTurn(Empire empire) {
        ResourceType output = getType().getProduces();
        if (output == null) {
            return 0;
        }
        if (!getHex().hasResource()) {
            return 0;
        }

        int total = workers.size() * getType().getRatePerWorker();

        boolean isMine = getType() == BuildingType.STONE_MINE
                || getType() == BuildingType.IRON_MINE;
        if (isMine && empire.hasTech(Tech.PRO_TOOLS)) {
            total = (int) (total * 1.5);
        }

        return Math.min(total, getHex().getDepositAmount());
    }
}
