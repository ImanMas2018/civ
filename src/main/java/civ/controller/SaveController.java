package civ.controller;

import civ.model.Game;
import civ.model.event.GameEvent;
import civ.model.save.SaveGame;
import civ.model.save.SaveService;
import civ.model.save.SaveSlotInfo;
import civ.view.MainWindow;
import java.io.IOException;
import java.util.List;

public class SaveController {

    private final SaveService saveService = new SaveService();
    private final MainWindow window;

    public SaveController(MainWindow window) {
        this.window = window;
    }

    public SaveService getSaveService() {
        return saveService;
    }

    public void wireAutosave(Game game) {
        game.getBus().subscribe(GameEvent.TURN_ENDED, payload -> {
            try {
                saveService.autosave((Game) payload);
            } catch (IOException ex) {
                ((Game) payload).addLog("Autosave failed: " + ex.getMessage());
            }
        });
    }

    public List<SaveSlotInfo> listSlots() {
        return saveService.listSlots();
    }

    public void save(Game game, String slotName, String saveName) throws IOException {
        saveService.save(game, slotName, saveName);
    }

    public Game load(String slotName) throws IOException {
        SaveGame data = saveService.load(slotName);
        Game game = new Game(data.mapSeed, true);
        saveService.apply(game, data);
        wireAutosave(game);
        return game;
    }

    public void deleteSlot(String slotName) throws IOException {
        saveService.deleteSlot(slotName);
    }

    public void openLoadedGame(Game game) {
        window.openGame(game);
    }
}
