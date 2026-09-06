package civ.net.protocol;

import civ.net.protocol.push.AlliancePrompt;
import civ.net.protocol.push.BattleReportPush;
import civ.net.protocol.push.ChatBroadcast;
import civ.net.protocol.push.GameOverBroadcast;
import civ.net.protocol.push.GameStateBroadcast;
import civ.net.protocol.push.LobbyStateBroadcast;
import civ.net.protocol.push.NoticePush;
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
import civ.net.protocol.response.ErrorResponse;
import civ.net.protocol.response.OkResponse;
import java.util.HashMap;
import java.util.Map;

/** The single table that maps a JSON "type" string to the class that models it. */
public final class MessageTypes {

    private static final Map<String, Class<? extends Message>> REGISTRY = new HashMap<>();

    static {
        register(JoinRequest.TYPE, JoinRequest.class);
        register(ReadyRequest.TYPE, ReadyRequest.class);
        register(SelectMapRequest.TYPE, SelectMapRequest.class);
        register(StartGameRequest.TYPE, StartGameRequest.class);
        register(ChatRequest.TYPE, ChatRequest.class);
        register(MoveUnitRequest.TYPE, MoveUnitRequest.class);
        register(BuildRequest.TYPE, BuildRequest.class);
        register(StationRequest.TYPE, StationRequest.class);
        register(UnstationRequest.TYPE, UnstationRequest.class);
        register(BuildRoadRequest.TYPE, BuildRoadRequest.class);
        register(BuildWallRequest.TYPE, BuildWallRequest.class);
        register(DemolishWallRequest.TYPE, DemolishWallRequest.class);
        register(DemolishRequest.TYPE, DemolishRequest.class);
        register(ExpandBorderRequest.TYPE, ExpandBorderRequest.class);
        register(FoundTownHallRequest.TYPE, FoundTownHallRequest.class);
        register(TrainRequest.TYPE, TrainRequest.class);
        register(ResearchRequest.TYPE, ResearchRequest.class);
        register(UpgradeTownHallRequest.TYPE, UpgradeTownHallRequest.class);
        register(CancelTownHallOrderRequest.TYPE, CancelTownHallOrderRequest.class);
        register(AttackRequest.TYPE, AttackRequest.class);
        register(EndTurnRequest.TYPE, EndTurnRequest.class);
        register(DeclareWarRequest.TYPE, DeclareWarRequest.class);
        register(AllianceRequest.TYPE, AllianceRequest.class);
        register(AllianceReplyRequest.TYPE, AllianceReplyRequest.class);
        register(BreakAllianceRequest.TYPE, BreakAllianceRequest.class);
        register(TradeOfferRequest.TYPE, TradeOfferRequest.class);
        register(TradeReplyRequest.TYPE, TradeReplyRequest.class);
        register(CancelTradeRequest.TYPE, CancelTradeRequest.class);
        register(CraftItemRequest.TYPE, CraftItemRequest.class);
        register(UseItemRequest.TYPE, UseItemRequest.class);
        register(CheatRequest.TYPE, CheatRequest.class);
        register(SetCheatsRequest.TYPE, SetCheatsRequest.class);

        register(OkResponse.TYPE, OkResponse.class);
        register(ErrorResponse.TYPE, ErrorResponse.class);
        register(LobbyStateBroadcast.TYPE, LobbyStateBroadcast.class);
        register(GameStateBroadcast.TYPE, GameStateBroadcast.class);
        register(ChatBroadcast.TYPE, ChatBroadcast.class);
        register(NoticePush.TYPE, NoticePush.class);
        register(AlliancePrompt.TYPE, AlliancePrompt.class);
        register(BattleReportPush.TYPE, BattleReportPush.class);
        register(GameOverBroadcast.TYPE, GameOverBroadcast.class);
    }

    private static void register(String type, Class<? extends Message> clazz) {
        REGISTRY.put(type, clazz);
    }

    public static Class<? extends Message> classFor(String type) {
        return REGISTRY.get(type);
    }

    private MessageTypes() {
    }
}
