package civ.net.server.handler;

import civ.model.BuildingType;
import civ.model.Game;
import civ.model.Player;
import civ.model.ResourceType;
import civ.model.Stockpile;
import civ.model.Unit;
import civ.net.protocol.Errors;
import civ.net.protocol.Message;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;

/** Shared preamble for every authoritative action handler. */
final class HandlerSupport {

    private HandlerSupport() {
    }

    static Game requireGame(ServerSession session, ClientHandler client) {
        if (session.isGameOver()) {
            client.send(new ErrorResponse("The game is over."));
            return null;
        }
        Game game = session.getGame();
        if (game == null) {
            client.send(new ErrorResponse("The game has not started yet."));
        }
        return game;
    }

    /** Returns true if the request was rejected because the match has ended. */
    static boolean rejectIfGameOver(ServerSession session, ClientHandler client) {
        if (!session.isGameOver()) {
            return false;
        }
        client.send(new ErrorResponse("The game is over."));
        return true;
    }

    static Player requireTurn(ServerSession session, ClientHandler client, Game game) {
        Player player = session.playerOf(client);
        if (player == null || !player.isAlive()) {
            client.send(new ErrorResponse("You are not in this game."));
            return null;
        }
        if (!game.isTurnOf(player)) {
            client.send(new ErrorResponse(Errors.NOT_YOUR_TURN));
            return null;
        }
        return player;
    }

    static Unit requireOwnUnit(Game game, Player player, long unitId, ClientHandler client) {
        Unit unit = game.findUnit(unitId);
        if (!game.owns(player, unit)) {
            client.send(new ErrorResponse(Errors.NOT_YOUR_UNIT));
            return null;
        }
        return unit;
    }

    static String missingResource(Stockpile stock, int wood, int stone, int iron) {
        if (!stock.canPay(ResourceType.WOOD, wood)) {
            return Errors.notEnough("wood");
        }
        if (!stock.canPay(ResourceType.STONE, stone)) {
            return Errors.notEnough("stone");
        }
        if (!stock.canPay(ResourceType.IRON, iron)) {
            return Errors.notEnough("iron");
        }
        return null;
    }

    static String missingResource(Stockpile stock, ResourceType type, int amount) {
        if (!stock.canPay(type, amount)) {
            return Errors.notEnough(type.getLabel().toLowerCase());
        }
        return null;
    }

    static String missingForBuilding(Stockpile stock, BuildingType type, int woodCost) {
        return missingResource(stock, woodCost, type.getStoneCost(), type.getIronCost());
    }

    static void ok(ClientHandler client, Message request) {
        client.send(new civ.net.protocol.response.OkResponse().withRequestId(request.getRequestId()));
    }
}
