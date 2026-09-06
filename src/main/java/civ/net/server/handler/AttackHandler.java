package civ.net.server.handler;

import civ.model.Game;
import civ.model.Hex;
import civ.model.Player;
import civ.model.combat.BattleReport;
import civ.net.protocol.Errors;
import civ.net.protocol.Message;
import civ.net.protocol.push.BattleReportPush;
import civ.net.protocol.request.AttackRequest;
import civ.net.protocol.response.ErrorResponse;
import civ.net.server.ClientHandler;
import civ.net.server.ServerSession;
import civ.net.server.StateFilter;

public class AttackHandler implements RequestHandler {

    @Override
    public void handle(ServerSession session, ClientHandler client, Message message) {
        AttackRequest request = (AttackRequest) message;
        Game game = HandlerSupport.requireGame(session, client);
        if (game == null) {
            return;
        }
        Player attacker = HandlerSupport.requireTurn(session, client, game);
        if (attacker == null) {
            return;
        }

        Hex from = game.getMap().get(request.getFromCol(), request.getFromRow());
        Hex to = game.getMap().get(request.getToCol(), request.getToRow());
        if (from == null || to == null) {
            client.send(new ErrorResponse("Invalid hex."));
            return;
        }

        if (game.canAttackWall(from, to) && !game.canAttack(from, to)) {
            game.attackWall(from, to);
            HandlerSupport.ok(client, request);
            StateFilter.broadcast(session);
            return;
        }

        Player defender = game.ownerOfUnitsAt(to);

        if (defender != null && !game.getDiplomacy().canAttack(attacker, defender)) {
            client.send(new ErrorResponse(
                    game.getDiplomacy().between(attacker, defender).lockReason()));
            return;
        }
        if (!game.ownsAllUnitsAt(attacker, from)) {
            client.send(new ErrorResponse(Errors.NOT_YOUR_UNIT));
            return;
        }
        if (!game.canAttack(from, to)) {
            client.send(new ErrorResponse("You cannot attack that hex from there."));
            return;
        }

        if (game.isDiceAttack(from, to)) {
            BattleReport report = game.beginDiceAttack(from, to);
            if (report == null) {
                client.send(new ErrorResponse("You cannot attack that hex from there."));
                return;
            }
            game.applyPendingDiceAttack();
            client.send(new BattleReportPush(report, true));
            if (defender != null) {
                game.queueReportFor(defender, report);
            }
            if (defender != null) {
                game.checkElimination(defender);
            }
        } else {
            BattleReport report = game.performQuietAttackWithReport(from, to);
            if (report != null && defender != null) {
                game.queueReportFor(defender, report);
                client.send(new BattleReportPush(report, true));
            }
            if (defender != null) {
                game.checkElimination(defender);
            }
        }

        HandlerSupport.ok(client, request);
        Player winner = game.findWinner();
        if (winner != null && game.getPlayers().size() > 1 && !session.isGameOver()) {
            session.markGameOver(winner.getName());
        }
        StateFilter.broadcast(session);
    }
}
