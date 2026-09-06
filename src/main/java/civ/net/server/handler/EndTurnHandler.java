package civ.net.server.handler;

import civ.model.Game;
import civ.model.Player;
import civ.model.TurnEngine;
import civ.model.combat.BattleReport;
import civ.net.protocol.Message;
import civ.net.protocol.push.BattleReportPush;
import civ.net.protocol.request.EndTurnRequest;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;

public class EndTurnHandler implements RequestHandler {

    private static final TurnEngine ENGINE = new TurnEngine();

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        EndTurnRequest request = (EndTurnRequest) message;
        Game game = HandlerSupport.requireGame(session, client);
        if (game == null) {
            return;
        }
        Player player = HandlerSupport.requireTurn(session, client, game);
        if (player == null) {
            return;
        }
        HandlerSupport.ok(client, request);
        endTurnFor(session, game, player);
    }

    /** Also called by the disconnect / heartbeat sweeper. */
    public static void endTurnFor(ServerSession session, Game game, Player player) {
        if (player == null || !game.isTurnOf(player)) {
            StateFilter.broadcast(session);
            return;
        }

        game.setProcessingTurn(true);
        try {
            ENGINE.endTurn(game);
        } finally {
            game.setProcessingTurn(false);
        }

        // Keep skipping if the next seat is also silent/dead (advanceTurn should
        // already skip them; this is a safety net for edge cases).
        int guard = 0;
        while (guard++ < game.getPlayers().size()) {
            Player current = game.getCurrentPlayer();
            if (current == null || (current.isAlive() && current.isConnected())) {
                break;
            }
            if (!current.isAlive()) {
                ENGINE.endTurn(game);
                continue;
            }
            // disconnected but still selected — force another advance
            game.setProcessingTurn(true);
            try {
                ENGINE.endTurn(game);
            } finally {
                game.setProcessingTurn(false);
            }
        }

        Player winner = game.findWinner();
        if (winner != null && game.getPlayers().size() > 1 && !session.isGameOver()) {
            session.markGameOver(winner.getName());
        }

        deliverPendingReports(session, game.getCurrentPlayer());
        StateFilter.broadcast(session);
    }

    private static void deliverPendingReports(ServerSession session, Player next) {
        if (next == null) {
            return;
        }
        ClientHandler client = session.getClients().byPlayerId(next.getId());
        if (client == null) {
            return;
        }
        for (BattleReport report : session.getGame().takeReportsFor(next)) {
            client.send(new BattleReportPush(report, false));
        }
    }
}
