package com.championbrain.overlay;

import java.util.*;

final class BattleAnalyzer {
    static final class Result {
        final String play, reason; final int confidence;
        Result(String play, String reason, int confidence){this.play=play;this.reason=reason;this.confidence=confidence;}
    }

    private static final Map<String,String[]> MOVES = new LinkedHashMap<>();
    private static final Map<String,String[]> TYPES = new HashMap<>();
    private static final Map<String,String> MOVE_TYPE = new HashMap<>();
    static {
        add("Arcanine", new String[]{"Fire"}, "Flare Blitz:Fire","Extreme Speed:Normal","Will-O-Wisp:Fire","Morning Sun:Normal");
        add("Vileplume", new String[]{"Grass","Poison"}, "Giga Drain:Grass","Sludge Bomb:Poison","Sleep Powder:Grass","Moonlight:Fairy");
        add("Annihilape", new String[]{"Fighting","Ghost"}, "Rage Fist:Ghost","Drain Punch:Fighting","Bulk Up:Fighting","Taunt:Dark");
        add("Slowking", new String[]{"Water","Psychic"}, "Scald:Water","Psychic:Psychic","Thunder Wave:Electric","Slack Off:Normal");
        add("Jolteon", new String[]{"Electric"}, "Thunderbolt:Electric","Volt Switch:Electric","Shadow Ball:Ghost","Calm Mind:Psychic");
        add("Steelix", new String[]{"Steel","Ground"}, "Heavy Slam:Steel","Earthquake:Ground","Stealth Rock:Rock","Body Press:Fighting");
        add("Garchomp", new String[]{"Dragon","Ground"}, "Earthquake:Ground","Dragon Claw:Dragon","Stone Edge:Rock","Swords Dance:Normal");
        add("Dragonite", new String[]{"Dragon","Flying"}, "Dragon Claw:Dragon","Extreme Speed:Normal","Fire Punch:Fire","Dragon Dance:Dragon");
        add("Gholdengo", new String[]{"Steel","Ghost"}, "Make It Rain:Steel","Shadow Ball:Ghost","Thunderbolt:Electric","Nasty Plot:Dark");
        add("Incineroar", new String[]{"Fire","Dark"}, "Flare Blitz:Fire","Knock Off:Dark","Fake Out:Normal","Parting Shot:Dark");
        add("Rillaboom", new String[]{"Grass"}, "Grassy Glide:Grass","Wood Hammer:Grass","Fake Out:Normal","U-turn:Bug");
        add("Blastoise", new String[]{"Water"}, "Hydro Pump:Water","Ice Beam:Ice","Aura Sphere:Fighting","Shell Smash:Normal");
        add("Kingler", new String[]{"Water"}, "Crabhammer:Water","Knock Off:Dark","High Horsepower:Ground","Swords Dance:Normal");
        add("Sharpedo", new String[]{"Water","Dark"}, "Liquidation:Water","Crunch:Dark","Ice Fang:Ice","Protect:Normal");
    }
    private static void add(String mon,String[] types,String... moves){TYPES.put(mon,types);String[] names=new String[moves.length];for(int i=0;i<moves.length;i++){String[] p=moves[i].split(":");names[i]=p[0];MOVE_TYPE.put(p[0],p[1]);}MOVES.put(mon,names);}

    static Result analyze(String text) {
        String clean=text==null?"":text.replace("\n"," "); List<String> foundMons=new ArrayList<>(),foundMoves=new ArrayList<>();
        for(String mon:MOVES.keySet()) if(clean.toLowerCase().contains(mon.toLowerCase())) foundMons.add(mon);
        for(String move:MOVE_TYPE.keySet()) if(clean.toLowerCase().contains(move.toLowerCase())) foundMoves.add(move);
        if(foundMoves.isEmpty()) return new Result("Open the move-selection screen","I captured the battle, but I need the four move names visible to calculate the best play.",45);
        String user=foundMons.size()>0?foundMons.get(0):null, enemy=foundMons.size()>1?foundMons.get(foundMons.size()-1):null;
        String best=foundMoves.get(0); double bestScore=-1;
        for(String move:foundMoves){double score=1;String mt=MOVE_TYPE.get(move);if(user!=null&&Arrays.asList(TYPES.get(user)).contains(mt))score*=1.5;if(enemy!=null)for(String dt:TYPES.get(enemy))score*=effect(mt,dt);if(score>bestScore){bestScore=score;best=move;}}
        int confidence=enemy==null?62:(bestScore>=3?91:bestScore>=2?84:72);
        String matchup=enemy==null?"the visible matchup":enemy;
        return new Result("Use " + best, MOVE_TYPE.get(best)+" pressure gives the strongest visible line into "+matchup+". Check abilities and revealed items before committing.",confidence);
    }

    private static double effect(String a,String d){
        String key=a+">"+d; Set<String> two=new HashSet<>(Arrays.asList("Fire>Grass","Fire>Ice","Fire>Bug","Fire>Steel","Water>Fire","Water>Ground","Water>Rock","Electric>Water","Electric>Flying","Grass>Water","Grass>Ground","Grass>Rock","Ice>Grass","Ice>Ground","Ice>Flying","Ice>Dragon","Fighting>Normal","Fighting>Ice","Fighting>Rock","Fighting>Dark","Fighting>Steel","Poison>Grass","Poison>Fairy","Ground>Fire","Ground>Electric","Ground>Poison","Ground>Rock","Ground>Steel","Flying>Grass","Flying>Fighting","Flying>Bug","Psychic>Fighting","Psychic>Poison","Rock>Fire","Rock>Ice","Rock>Flying","Rock>Bug","Ghost>Psychic","Ghost>Ghost","Dragon>Dragon","Dark>Psychic","Dark>Ghost","Steel>Ice","Steel>Rock","Steel>Fairy","Fairy>Fighting","Fairy>Dragon","Fairy>Dark"));
        if(two.contains(key))return 2; if((a.equals("Electric")&&d.equals("Ground"))||(a.equals("Dragon")&&d.equals("Fairy"))||(a.equals("Ghost")&&d.equals("Normal")))return 0; return 1;
    }
}
