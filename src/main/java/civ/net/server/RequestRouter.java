package civ.net.server;

import civ.net.protocol.request.BuildRequest;
import civ.net.protocol.request.BuildRoadRequest;
import civ.net.protocol.request.BuildWallRequest;
import civ.net.protocol.request.CancelTownHallOrderRequest;
import civ.net.protocol.request.ChatRequest;
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
import civ.net.protocol.request.StartGameRequest;
import civ.net.protocol.request.StationRequest;
import civ.net.protocol.request.TrainRequest;
import civ.net.protocol.request.UnstationRequest;
import civ.net.protocol.request.UpgradeTownHallRequest;
import civ.net.server.handler.BuildHandler;
import civ.net.server.handler.BuildRoadHandler;
import civ.net.server.handler.BuildWallHandler;
import civ.net.server.handler.CancelTownHallOrderHandler;
import civ.net.server.handler.ChatHandler;
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
import civ.net.server.handler.StartGameHandler;
import civ.net.server.handler.StationHandler;
import civ.net.server.handler.TrainHandler;
import civ.net.server.handler.UnstationHandler;
import civ.net.server.handler.UpgradeTownHallHandler;
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
    }

    public RequestHandler handlerFor(String type) {
        return handlers.get(type);
    }
}
