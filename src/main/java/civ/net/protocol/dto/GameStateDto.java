package civ.net.protocol.dto;

/** Flat snapshot for one player. Filled out in Step 3. */
public class GameStateDto {

    public int turn;
    public long currentPlayerId;
    public long yourPlayerId;
    public String season;
}
