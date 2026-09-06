package civ.net.server.handler;

import civ.model.Game;
import civ.model.Hex;
import civ.model.Player;
import civ.model.ResourceType;
import civ.model.Stockpile;
import civ.model.Unit;
import civ.net.protocol.Message;
import civ.net.protocol.push.NoticePush;
import civ.net.protocol.request.CheatRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;

public class CheatHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        CheatRequest request = (CheatRequest) message;
        Game game = session.getGame();
        Player player = session.playerOf(client);

        if (!session.getLobby().isCheatsEnabled()) {
            client.send(new ErrorResponse("Cheats are disabled for this match."));
            return;
        }
        if (game == null || player == null) {
            client.send(new ErrorResponse("Cheats only work during a game."));
            return;
        }
        if (session.isGameOver()) {
            client.send(new ErrorResponse("The game is over."));
            return;
        }

        String text = request.getCommand() == null ? "" : request.getCommand().trim();
        String command = text.split("\\s+")[0].toLowerCase();

        switch (command) {
            case "/greedisgood":
                Stockpile stock = player.getEmpire().getStock();
                stock.setCapacity(stock.getCapacity() + 500);
                for (ResourceType type : ResourceType.values()) {
                    stock.add(type, 500);
                }
                announce(session, player, "conjured a fortune");
                break;

            case "/marco":
                for (int col = 0; col < game.getMap().getCols(); col++) {
                    for (int row = 0; row < game.getMap().getRows(); row++) {
                        Hex hex = game.getMap().get(col, row);
                        if (hex != null) {
                            player.getFog().discover(hex);
                        }
                    }
                }
                announce(session, player, "peered through the fog");
                break;

            case "/redbull":
                for (Unit unit : player.getEmpire().getUnits()) {
                    unit.setAp(unit.getMaxAp());
                }
                announce(session, player, "gave their army wings");
                break;

            default:
                client.send(new ErrorResponse("Unknown command: " + command));
                return;
        }

        HandlerSupport.ok(client, request);
        StateFilter.broadcast(session);
    }

    private void announce(ServerSession session, Player player, String what) {
        session.getClients().broadcast(new NoticePush(player.getName() + " " + what + "."));
    }
}
