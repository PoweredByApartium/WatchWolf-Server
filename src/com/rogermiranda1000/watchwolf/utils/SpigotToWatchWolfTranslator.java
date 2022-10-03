package com.rogermiranda1000.watchwolf.utils;

import com.rogermiranda1000.watchwolf.entities.blocks.Block;
import com.rogermiranda1000.watchwolf.entities.blocks.Blocks;
import com.rogermiranda1000.watchwolf.entities.blocks.Orientable;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpigotToWatchWolfTranslator {
    public static Block getBlock(org.bukkit.block.Block block) {
        return SpigotToWatchWolfTranslator.getBlock(block.getBlockData());
    }

    public static Block getBlock(Material material) {
        return Blocks.getBlock(material.name());
    }

    /*   --- BLOCK DATA TO BLOCK ---   */

    public static Block getBlock(BlockData blockData) {
        Block block = SpigotToWatchWolfTranslator.getBlock(blockData.getMaterial());
        Map<String,String> arguments = getArgumentsAndProperty(blockData);

        Collection<Orientable.Orientation> orientations = SpigotToWatchWolfTranslator.getOrientations(arguments);
        if (orientations.size() > 0) {
            for (Orientable.Orientation orientation : Orientable.Orientation.values()) {
                if (orientations.contains(orientation)) block = (Block)((Orientable)block).set(orientation);
            }
        }

        // TODO others

        return block;
    }

    /*   --- INTERNAL USE ONLY ---   */

    private static final Pattern blockDataData = Pattern.compile("minecraft:([^\\[]+)\\[(.+)\\]");
    public static Map<String,String> getArgumentsAndProperty(BlockData blockData) {
        Map<String,String> arguments = new HashMap<>();

        Matcher m = SpigotToWatchWolfTranslator.blockDataData.matcher(blockData.getAsString());
        if (!m.find()) return arguments; // just a basic block

        String matName = m.group(1);
        String[] args = m.group(2).split(",");
        for (String arg : args) {
            String[] data = arg.split("=");
            // non-useful data
            if (data[0].startsWith("has_bottle_")
                    || data[0].startsWith("has_record")) continue;                          // inv dependant
            if (data[0].equals("enabled") && matName.equals("HOPPER")
                    || data[0].equals("triggered")) continue;                               // adjacent redstone dependant
            if (data[0].equals("instrument") && matName.equals("NOTE_BLOCK")) continue;  // bottom-block dependant
            if (data[0].equals("occupied")) continue;                                       // entity dependant
            if (data[0].equals("persistent")
                    || data[0].equals("unstable")
                    || data[0].equals("bloom")) continue;                                   // admin block
            if (data[0].equals("distance")) continue;                                       // block dependant (leaves relative to wood)
            if (data[0].equals("stage")) continue;                                          // same block type (aging saplings)
            if (data[0].equals("attached") || data[0].equals("disarmed")) continue;         // block dependant
            if (data[0].equals("power")) continue;                                          // block/event dependant
            if (data[0].equals("tilt")) continue;                                           // entity dependant (over the leaf)
            if (data[0].equals("can_summon") || data[0].equals("shrieking")) continue;      // admin/entity dependant (sculk shrieker)
            if (data[0].equals("bottom") && matName.equals("SCAFFOLDING")) continue;     // bottom-block dependant
            if (data[0].equals("has_book")) continue;                                       // inv dependant
            if (data[0].equals("sculk_sensor_phase")) continue;                             // admin block
            if (data[0].equals("signal_fire")) continue;                                    // bottom-block dependant
            if (data[0].equals("hatch")) continue;                                          // unable to concatenate
            if (data[0].equals("up") && matName.endsWith("_WALL")) continue;             // adjacent-block dependant
            if (data[0].equals("thickness")) continue;                                      // block dependant
            if (data[0].equals("snowy")) continue;                                          // block dependant
            if (data[0].equals("in_wall")) continue;                                        // same block
            // any block is tall by default
            //if (data[1].equals("tall")) continue;                                           // top-block dependant

            if (data[0].equals("lit") && (matName.equals("SMOKER")
                    || matName.equals("FURNACE"))) continue;                             // inv dependant
            if (data[0].equals("powered") && Arrays.asList("ACACIA_DOOR",
                    "ACACIA_FENCE_GATE", "ACACIA_TRAPDOOR", "ACTIVATOR_RAIL",
                    "BELL", "BIRCH_DOOR", "BIRCH_FENCE_GATE", "BIRCH_TRAPDOOR",
                    "CRIMSON_DOOR", "CRIMSON_FENCE_GATE", "CRIMSON_TRAPDOOR",
                    "DARK_OAK_DOOR", "DARK_OAK_FENCE_GATE", "DARK_OAK_TRAPDOOR",
                    "IRON_DOOR", "IRON_TRAPDOOR", "JUNGLE_DOOR", "JUNGLE_FENCE_GATE",
                    "JUNGLE_TRAPDOOR", "LECTERN", "MANGROVE_DOOR",
                    "MANGROVE_FENCE_GATE", "MANGROVE_TRAPDOOR", "NOTE_BLOCK",
                    "OAK_DOOR", "OAK_FENCE_GATE", "OAK_TRAPDOOR", "POWERED_RAIL",
                    "SPRUCE_DOOR", "SPRUCE_FENCE_GATE", "SPRUCE_TRAPDOOR",
                    "TRIPWIRE", "WARPED_DOOR", "WARPED_FENCE_GATE",
                    "WARPED_TRAPDOOR").contains(matName)) continue;
            if (data[0].equals("age") && Arrays.asList("CAVE_VINES", "CACTUS", "FIRE",
                    "KELP", "SUGAR_CANE", "MANGROVE_PROPAGULE", "TWISTING_VINES",
                    "WEEPING_VINES").contains(matName)) continue;

            arguments.put(data[0], data[1]);
        }

        return arguments;
    }

    public static Set<String> getArguments(BlockData blockData) {
        return SpigotToWatchWolfTranslator.getArgumentsAndProperty(blockData).keySet();
    }

    private static Collection<Orientable.Orientation> getOrientations(Map<String, String> options) {
        HashMap<String, List<String>> arg = new HashMap<>();
        for (Map.Entry<String,String> option : options.entrySet()) {
            List<String> l = new ArrayList<>();
            l.add(option.getValue());
            arg.put(option.getKey(), l);
        }
        return SpigotToWatchWolfTranslator.getOrientations(arg);
    }

    public static Collection<Orientable.Orientation> getOrientations(HashMap<String, List<String>> options) {
        Collection<Orientable.Orientation> r = new HashSet<>(); // TODO why duplicates?
        if (options.containsKey("up")) r.add(Orientable.Orientation.U);
        if (options.containsKey("hanging")) r.add(Orientable.Orientation.U);
        if (options.containsKey("down")) r.add(Orientable.Orientation.D);
        if (options.containsKey("north")) r.add(Orientable.Orientation.N);
        if (options.containsKey("south")) r.add(Orientable.Orientation.S);
        if (options.containsKey("east")) r.add(Orientable.Orientation.E);
        if (options.containsKey("west")) r.add(Orientable.Orientation.W);
        if (options.containsKey("face")) {
            if (options.get("face").contains("ceiling")) r.add(Orientable.Orientation.U);
            if (options.get("face").contains("floor")) r.add(Orientable.Orientation.D);
        }
        if (options.containsKey("half")) {
            if (options.get("half").contains("top") || options.get("half").contains("upper")) r.add(Orientable.Orientation.U);
            if (options.get("half").contains("bottom") || options.get("half").contains("upper")) r.add(Orientable.Orientation.D);
        }
        if (options.containsKey("facing")) {
            if (options.get("facing").contains("up")) r.add(Orientable.Orientation.U);
            if (options.get("facing").contains("down")) r.add(Orientable.Orientation.D);
            if (options.get("facing").contains("north")) r.add(Orientable.Orientation.N);
            if (options.get("facing").contains("south")) r.add(Orientable.Orientation.S);
            if (options.get("facing").contains("east")) r.add(Orientable.Orientation.E);
            if (options.get("facing").contains("west")) r.add(Orientable.Orientation.W);
        }
        if (options.containsKey("vertical-direction")) {
            if (options.get("vertical-direction").contains("up")) r.add(Orientable.Orientation.U);
            if (options.get("vertical-direction").contains("down")) r.add(Orientable.Orientation.D);
        }
        if (options.containsKey("type")) {
            if (options.get("type").contains("top") || options.get("type").contains("double")) r.add(Orientable.Orientation.U);
            if (options.get("type").contains("bottom") || options.get("type").contains("double")) r.add(Orientable.Orientation.D);
        }
        if (options.containsKey("orientation")) {
            if (regexContains(options.get("orientation"), "^up_")) r.add(Orientable.Orientation.U);
            if (regexContains(options.get("orientation"), "^down_")) r.add(Orientable.Orientation.D);
            if (regexContains(options.get("orientation"), "_north$") || options.get("orientation").contains("north_up")) r.add(Orientable.Orientation.N);
            if (regexContains(options.get("orientation"), "_south$") || options.get("orientation").contains("south_up")) r.add(Orientable.Orientation.S);
            if (regexContains(options.get("orientation"), "_east$") || options.get("orientation").contains("east_up")) r.add(Orientable.Orientation.E);
            if (regexContains(options.get("orientation"), "_west$") || options.get("orientation").contains("west_up")) r.add(Orientable.Orientation.W);
        }
        if (options.containsKey("shape")) {
            if (regexContains(options.get("shape"), "^ascending_")) r.add(Orientable.Orientation.U);
            if (regexContains(options.get("shape"), "^north_") || options.get("shape").contains("ascending_north")) r.add(Orientable.Orientation.N);
            if (regexContains(options.get("shape"), "^south_") || options.get("shape").contains("north_south") || options.get("shape").contains("ascending_south")) r.add(Orientable.Orientation.S);
            if (regexContains(options.get("shape"), "^east_") || options.get("shape").contains("ascending_east") || options.get("shape").contains("east_west")) r.add(Orientable.Orientation.E);
            if (regexContains(options.get("shape"), "^west_") || options.get("shape").contains("ascending_west")) r.add(Orientable.Orientation.W);
        }
        return r;
    }

    public static boolean regexContains(List<String> list, String regex) {
        Pattern pattern = Pattern.compile(regex);

        for (String e : list) {
            if (pattern.matcher(e).find()) return true;
        }
        return false;
    }
}
