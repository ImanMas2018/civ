package civ.net.server;

import civ.net.protocol.request.AllianceReplyRequest;
import civ.net.protocol.request.AllianceRequest;
import civ.net.protocol.request.AttackRequest;
import civ.net.protocol.request.BreakAllianceRequest;
import civ.net.protocol.request.BuildRequest;
import civ.net.protocol.request.BuildRoadRequest;
import civ.net.protocol.request.BuildWallRequest;
import civ.net.protocol.request.CancelTownHallOrderRequest;
import civ.net.protocol.request.CancelTradeRequest;
import civ.net.protocol.request.ChatRequest;
import civ.net.protocol.request.CheatRequest;
import civ.net.protocol.request.CraftItemRequest;
import civ.net.protocol.request.DeclareWarRequest;
import civ.net.protocol.request.DemolishRequest;
import civ.net.protocol.request.DemolishWallRequest;
import civ.net.protocol.request.EndTurnRequest;
import civ.net.protocol.request.ExpandBorderRequest;
import civ.net.protocol.request.FoundTownHallRequest;
import civ.net.protocol.request.JoinRequest;
import civ.net.protocol.request.MoveUnitRequest;
import civ.net.protocol.request.ReadyRequest;
import civ.net.protocol.request.ResearchRequest;
import civ.net.protocol.request.SelectMapRequest;
import civ.net.protocol.request.SetCheatsRequest;
import civ.net.protocol.request.StartGameRequest;
import civ.net.protocol.request.StationRequest;
import civ.net.protocol.request.TradeOfferRequest;
import civ.net.protocol.request.TradeReplyRequest;
import civ.net.protocol.request.TrainRequest;
import civ.net.protocol.request.UnstationRequest;
import civ.net.protocol.request.UpgradeTownHallRequest;
import civ.net.protocol.request.UseItemRequest;
import civ.net.server.handler.AllianceHandler;
import civ.net.server.handler.AllianceReplyHandler;
import civ.net.server.handler.AttackHandler;
import civ.net.server.handler.BreakAllianceHandler;
import civ.net.server.handler.BuildHandler;
import civ.net.server.handler.BuildRoadHandler;
import civ.net.server.handler.BuildWallHandler;
import civ.net.server.handler.CancelTownHallOrderHandler;
import civ.net.server.handler.CancelTradeHandler;
import civ.net.server.handler.ChatHandler;
import civ.net.server.handler.CheatHandler;
import civ.net.server.handler.CraftItemHandler;
import civ.net.server.handler.DeclareWarHandler;
import civ.net.server.handler.DemolishHandler;
import civ.net.server.handler.DemolishWallHandler;
import civ.net.server.handler.EndTurnHandler;
import civ.net.server.handler.ExpandBorderHandler;
import civ.net.server.handler.FoundTownHallHandler;
import civ.net.server.handler.JoinHandler;
import civ.net.server.handler.MoveUnitHandler;
import civ.net.server.handler.ReadyHandler;
import civ.net.server.handler.RequestHandler;
import civ.net.server.handler.ResearchHandler;
import civ.net.server.handler.SelectMapHandler;
import civ.net.server.handler.SetCheatsHandler;
import civ.net.server.handler.StartGameHandler;
import civ.net.server.handler.StationHandler;
import civ.net.server.handler.TradeOfferHandler;
import civ.net.server.handler.TradeReplyHandler;
import civ.net.server.handler.TrainHandler;
import civ.net.server.handler.UnstationHandler;
import civ.net.server.handler.UpgradeTownHallHandler;
import civ.net.server.handler.UseItemHandler;
import java.util.HashMap;
import java.util.Map;

public class RequestRouter {

    private final Map<String, RequestHandler> handlers = new HashMap<>();

    public RequestRouter(ServerSession session) {
        handlers.put(JoinRequest.TYPE, new JoinHandler());
        handlers.put(ReadyRequest.TYPE, new ReadyHandler());
        handlers.put(SelectMapRequest.TYPE, new SelectMapHandler());
        handlers.put(StartGameRequest.TYPE, new StartGameHandler());
        handlers.put(ChatRequest.TYPE, new ChatHandler());

        handlers.put(MoveUnitRequest.TYPE, new MoveUnitHandler());
        handlers.put(BuildRequest.TYPE, new BuildHandler());
        handlers.put(StationRequest.TYPE, new StationHandler());
        handlers.put(UnstationRequest.TYPE, new UnstationHandler());
        handlers.put(BuildRoadRequest.TYPE, new BuildRoadHandler());
        handlers.put(BuildWallRequest.TYPE, new BuildWallHandler());
        handlers.put(DemolishWallRequest.TYPE, new DemolishWallHandler());
        handlers.put(DemolishRequest.TYPE, new DemolishHandler());
        handlers.put(ExpandBorderRequest.TYPE, new ExpandBorderHandler());
        handlers.put(FoundTownHallRequest.TYPE, new FoundTownHallHandler());
        handlers.put(TrainRequest.TYPE, new TrainHandler());
        handlers.put(ResearchRequest.TYPE, new ResearchHandler());
        handlers.put(UpgradeTownHallRequest.TYPE, new UpgradeTownHallHandler());
        handlers.put(CancelTownHallOrderRequest.TYPE, new CancelTownHallOrderHandler());
        handlers.put(EndTurnRequest.TYPE, new EndTurnHandler());
        handlers.put(AttackRequest.TYPE, new AttackHandler());
        handlers.put(DeclareWarRequest.TYPE, new DeclareWarHandler());
        handlers.put(AllianceRequest.TYPE, new AllianceHandler());
        handlers.put(AllianceReplyRequest.TYPE, new AllianceReplyHandler());
        handlers.put(BreakAllianceRequest.TYPE, new BreakAllianceHandler());
        handlers.put(TradeOfferRequest.TYPE, new TradeOfferHandler());
        handlers.put(TradeReplyRequest.TYPE, new TradeReplyHandler());
        handlers.put(CancelTradeRequest.TYPE, new CancelTradeHandler());
        handlers.put(CraftItemRequest.TYPE, new CraftItemHandler());
        handlers.put(UseItemRequest.TYPE, new UseItemHandler());
        handlers.put(CheatRequest.TYPE, new CheatHandler());
        handlers.put(SetCheatsRequest.TYPE, new SetCheatsHandler());
    }

    public RequestHandler handlerFor(String type) {
        return handlers.get(type);
    }
}
