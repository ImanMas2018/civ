package civ.model;

public class Worker extends Unit {

    private ProductionBuilding station;

    public Worker(int col, int row) {
        super("Worker", 3, 1, col, row);
    }

    public Worker(long id, long createdAt, int col, int row) {
        super(id, createdAt, "Worker", 3, 1, col, row);
    }

    public ProductionBuilding getStation() {
        return station;
    }

    public void setStation(ProductionBuilding station) {
        this.station = station;
    }

    @Override
    public boolean isBusy() {
        return station != null;
    }

    @Override
    public String describe() {
        if (station == null) {
            return super.describe() + "  idle";
        }
        return super.describe() + "  working in " + station.getType().getLabel();
    }
}
