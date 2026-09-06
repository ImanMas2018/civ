package civ.net.server.handler;

import civ.model.Apothecary;
import civ.model.Building;
import civ.model.Game;
import civ.model.Player;
import civ.model.item.ItemType;
import civ.net.protocol.Message;
import civ.net.protocol.request.CraftItemRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;

public class CraftItemHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        CraftItemRequest request = (CraftItemRequest) message;
        if (HandlerSupport.rejectIfGameOver(session, client)) {
            return;
        }
        Game game = HandlerSupport.requireGame(session, client);
        if (game == null) {
            return;
        }
        Player player = HandlerSupport.requireTurn(session, client, game);
        if (player == null) {
            return;
        }

        Building building = game.findBuilding(request.getApothecaryId());
        if (!(building instanceof Apothecary)) {
            client.send(new ErrorResponse("That is not an Apothecary."));
            return;
        }
        Apothecary apothecary = (Apothecary) building;
        if (!game.owns(player, apothecary)) {
            client.send(new ErrorResponse("That Apothecary does not belong to you."));
            return;
        }

        ItemType type;
        try {
            type = ItemType.valueOf(request.getItemType());
        } catch (RuntimeException ex) {
            client.send(new ErrorResponse("Unknown item type."));
            return;
        }

        String reason = game.craftItemRejection(apothecary, type);
        if (reason != null) {
            client.send(new ErrorResponse(reason));
            return;
        }

        // craftItem uses getCurrentPlayer(), which is this player after requireTurn.
        game.craftItem(apothecary, type);
        HandlerSupport.ok(client, request);
        StateFilter.broadcast(session);
    }
}
