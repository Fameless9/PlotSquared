/*
 * Copyright (c) IntellectualCrafters - 2014.
 * You are not allowed to distribute and/or monetize any of our intellectual property.
 * IntellectualCrafters is not affiliated with Mojang AB. Minecraft is a trademark of Mojang AB.
 *
 * >> File = Merge.java
 * >> Generated by: Citymonstret at 2014-08-09 01:41
 */

package com.intellectualcrafters.plot.commands;

import java.util.ArrayList;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.intellectualcrafters.plot.C;
import com.intellectualcrafters.plot.PlayerFunctions;
import com.intellectualcrafters.plot.Plot;
import com.intellectualcrafters.plot.PlotHelper;
import com.intellectualcrafters.plot.PlotId;
import com.intellectualcrafters.plot.PlotMain;
import com.intellectualcrafters.plot.PlotWorld;
import com.intellectualcrafters.plot.SetBlockFast;
import com.intellectualcrafters.plot.database.DBFunc;
import com.intellectualcrafters.plot.events.PlotMergeEvent;

/**
 * 
 * @author Citymonstret
 * 
 */
public class Merge extends SubCommand {

    public static String[] values = new String[] { "north", "east", "south", "west" };
    public static String[] aliases = new String[] { "n", "e", "s", "w"};
    
    public Merge() {
        super(Command.MERGE, "Merge the plot you are standing on with another plot.", "merge", CommandCategory.ACTIONS);
    }

    public static String direction(float yaw) {
        yaw = yaw / 90;
        yaw = (float)Math.round(yaw);
     
        if (yaw == -4 || yaw == 0 || yaw == 4) {return "SOUTH";}
        if (yaw == -1 || yaw == 3) {return "EAST";}
        if (yaw == -2 || yaw == 2) {return "NORTH";}
        if (yaw == -3 || yaw == 1) {return "WEST";}
        return "";
    }
    
    @Override
    public boolean execute(Player plr, String... args) {
        if (!PlayerFunctions.isInPlot(plr)) {
            PlayerFunctions.sendMessage(plr, C.NOT_IN_PLOT);
            return true;
        }
        Plot plot = PlayerFunctions.getCurrentPlot(plr);
        if (plot==null || !plot.hasOwner()) {
            PlayerFunctions.sendMessage(plr, C.NO_PLOT_PERMS);
            return false;
        }
        if (!plot.getOwner().equals(plr.getUniqueId())) {
            PlayerFunctions.sendMessage(plr, C.NO_PLOT_PERMS);
            return false;
        }
        if (args.length<1) {
            PlayerFunctions.sendMessage(plr, C.SUBCOMMAND_SET_OPTIONS_HEADER.s() + StringUtils.join(values,C.BLOCK_LIST_SEPARATER.s()));
            return false;
        }
        int direction = -1;
        for (int i = 0; i<values.length; i++) {
            if (args[0].equalsIgnoreCase(values[i]) || args[0].equalsIgnoreCase(aliases[i])) {
                direction = i;
                break;
            }
        }
        int direction2 = direction > 1 ? direction-2 : direction+2;
        if (direction==-1) {
            PlayerFunctions.sendMessage(plr, C.SUBCOMMAND_SET_OPTIONS_HEADER.s() + StringUtils.join(values,C.BLOCK_LIST_SEPARATER.s()));
            PlayerFunctions.sendMessage(plr, C.DIRECTION.s().replaceAll("%dir%", direction(plr.getLocation().getYaw())));
            return false;
        }
        World world = plr.getWorld();
        PlotId bot = PlayerFunctions.getBottomPlot(world, plot).id;
        PlotId top = PlayerFunctions.getTopPlot(world, plot).id;
        ArrayList<PlotId> plots;
        switch (direction) {
            case 0: // north = -y
                plots = PlayerFunctions.getPlotSelectionIds(plr.getWorld(), new PlotId(bot.x,bot.y-1), new PlotId(top.x,bot.y-1));
                break;
            case 1: // east = +x
                plots = PlayerFunctions.getPlotSelectionIds(plr.getWorld(), new PlotId(top.x+1,bot.y), new PlotId(top.x+1,top.y));
                break;
            case 2: // south = +y
                plots = PlayerFunctions.getPlotSelectionIds(plr.getWorld(), new PlotId(bot.x,top.y+1), new PlotId(top.x,top.y+1));
                break;
            case 3: // west = -x
                plots = PlayerFunctions.getPlotSelectionIds(plr.getWorld(), new PlotId(bot.x-1,bot.y), new PlotId(bot.x-1,top.y));
                break;
            default:
                return false;
        }
        for (PlotId myid:plots) {
            Plot myplot = PlotMain.getPlots(world).get(myid);
            if (myplot==null || !myplot.hasOwner() || !(myplot.getOwner().equals(plr.getUniqueId()))) {
                PlayerFunctions.sendMessage(plr, C.NO_PERM_MERGE.s().replaceAll("%plot%", myid.toString()));
                return false;
            }
            if (!PlayerFunctions.getBottomPlot(world, myplot).equals(PlayerFunctions.getTopPlot(world, myplot))) {
                PlayerFunctions.sendMessage(plr, C.NO_MERGE_TO_MEGA);
                return false;
            }
        }
        
        PlotMergeEvent event = new PlotMergeEvent(world, plot, plots);
        
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            event.setCancelled(true);
            PlayerFunctions.sendMessage(plr, "&cMerge has been cancelled");
            return false;
        }
        PlayerFunctions.sendMessage(plr, "&cPlots have been merged");
        return PlotHelper.mergePlot(world, plr, PlayerFunctions.getPlotSelectionIds(plr.getWorld(), new PlotId(bot.x,bot.y), new PlotId(top.x,top.y)), plots, direction, direction2);
    }
    
}
