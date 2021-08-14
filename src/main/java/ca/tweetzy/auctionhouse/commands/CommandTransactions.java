package ca.tweetzy.auctionhouse.commands;

import ca.tweetzy.auctionhouse.AuctionHouse;
import ca.tweetzy.auctionhouse.api.AuctionAPI;
import ca.tweetzy.auctionhouse.guis.transaction.GUITransactionList;
import ca.tweetzy.core.commands.AbstractCommand;
import ca.tweetzy.core.utils.TimeUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * The current file has been created by Kiran Hart
 * Date Created: March 23 2021
 * Time Created: 9:29 p.m.
 * Usage of any code found within this class is prohibited unless given explicit permission otherwise
 */
public class CommandTransactions extends AbstractCommand {

    public CommandTransactions() {
        super(CommandType.PLAYER_ONLY, "transactions");
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        Player player = (Player) sender;
        if (AuctionAPI.tellMigrationStatus(player)) return ReturnType.FAILURE;

        if (AuctionHouse.getInstance().getAuctionBanManager().checkAndHandleBan(player)) {
            return ReturnType.FAILURE;
        }

        AuctionHouse.getInstance().getGuiManager().showGUI(player, new GUITransactionList(AuctionHouse.getInstance().getAuctionPlayerManager().getPlayer(player.getUniqueId())));
        return ReturnType.SUCCESS;
    }

    @Override
    public String getPermissionNode() {
        return "auctionhouse.cmd.transactions";
    }

    @Override
    public String getSyntax() {
        return AuctionHouse.getInstance().getLocale().getMessage("commands.syntax.transactions").getMessage();
    }

    @Override
    public String getDescription() {
        return AuctionHouse.getInstance().getLocale().getMessage("commands.description.transactions").getMessage();
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
        return null;
    }
}
