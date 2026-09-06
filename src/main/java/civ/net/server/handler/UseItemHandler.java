package civ.net.server.handler;

import civ.model.Game;
import civ.model.Hex;
import civ.model.Player;
import civ.model.Unit;
import civ.model.item.ItemType;
import civ.net.protocol.Message;
import civ.net.protocol.request.UseItemRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;

public class UseItemHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        UseItemRequest request = (UseItemRequest) message;
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

        ItemType type;
        try {
            type = ItemType.valueOf(request.getItemType());
        } catch (RuntimeException ex) {
            client.send(new ErrorResponse("Unknown item type."));
            return;
        }

        Unit target = game.findUnit(request.getUnitId());
        Hex destination = null;
        if (request.getTargetCol() != null && request.getTargetRow() != null) {
            destination = game.getMap().get(request.getTargetCol(), request.getTargetRow());
        }

        String reason = game.useItemRejection(player, type, target, destination);
        if (reason != null) {
            client.send(new ErrorResponse(reason));
            return;
        }

        game.useItem(player, type, target, destination);
        // Deliberately: no AP is spent. Using an item is a free action.
        HandlerSupport.ok(client, request);
        StateFilter.broadcast(session);
    }
}
