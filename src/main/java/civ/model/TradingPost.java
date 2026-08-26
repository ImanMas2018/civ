package civ.model;

public class TradingPost extends Building {

    public TradingPost(Hex hex) {
        super(BuildingType.TRADING_POST, hex);
        setHealth(60, 60);
    }

    @Override
    public int outputPerTurn(Empire empire, GameMap map) {
        return 0;
    }
}
