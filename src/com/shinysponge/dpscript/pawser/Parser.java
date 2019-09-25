package com.shinysponge.dpscript.pawser;

import com.shinysponge.dpscript.pawser.conditions.*;
import com.shinysponge.dpscript.pawser.parsers.SelectorParser;
import com.shinysponge.dpscript.tokenizew.Token;
import com.shinysponge.dpscript.tokenizew.TokenIterator;
import com.shinysponge.dpscript.tokenizew.TokenType;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This is a vewy kewl pawsew by pwetty
 */
public class Parser {

    private ScopeType scope;
    public TokenIterator tokens;
    private List<String> tickCommands;
    private Map<String, List<String>> functions = new HashMap<>();
    private Map<String, Integer> consts = new HashMap<>();
    private List<String> globals = new ArrayList<>();
    private int autoGenerated = 1;

    private SelectorParser selectors;
    private Map<String,VariableType> variables = new HashMap<>();

    /**
     * Constwuctow is cool
     * but not really
     * Destwuctow is better
     * but im just kiddin
     * it sux
     * and so does resdsponge
     */
    public Parser(TokenIterator tokens) {
        this.tokens = tokens;
        this.scope = ScopeType.GLOBAL;
        this.tickCommands = new ArrayList<>();
        this.selectors = new SelectorParser(this);
    }

    public String parseNBTSource() {
        if (tokens.isNext("{")) {
            return "value " + parseNBTValue();
        }
        String source;
        if (tokens.skip("@")) {
            source = "entity " + selectors.parseSelector();
        } else {
            source = "block " + readPosition();
        }
        tokens.expect('[');
        String path = tokens.next(TokenType.STRING);
        tokens.expect(']');
        return "from " + source + " " + path;
    }

    public static void pawse(List<Token> towkewnz) {
        new Parser(new TokenIterator(towkewnz)).parse();

        // This is so sad alexa play minecraftcito 2
        // and mine diamonds (death metal cover)
        // so far i got Float.NEGATIVE_INFINITY

        /*HashMap<TokenType[], ParserMethod> parsers = new HashMap<>();
        ArrayList<Token> out = new ArrayList<>();
        for (Method method : Parser.class.getMethods()) {
            ParserPattern pattern = method.getAnnotation(ParserPattern.class);
            if(pattern != null) {
                if(parsers.containsKey(pattern.value())) {
                    throw new RuntimeException("Non Unique Pattern!");
                }
                parsers.put(pattern.value(), new ParserMethod(method, pattern.priority()));
            }
        }

        ArrayList<Token> currentStreak = new ArrayList<>();
        Queue<Token> tokens = new LinkedList<Token>(towkewnz);

        Token current;

        while ((current = tokens.poll()) != null) {
            currentStreak.add(current);
            for (TokenType[] tokenTypes : parsers.keySet()) {
                TokenType[] currentStreakArr = currentStreak.stream().map(Token::getType).toArray(TokenType[]::new);

//                System.out.println("KEY " + Arrays.toString(tokenTypes));
//                System.out.println("SELF" + Arrays.toString(currentStreakArr));

                if(Arrays.equals(tokenTypes, currentStreakArr)) {
                    Token newT = parsers.get(tokenTypes).invoke(currentStreak.toArray(new Token[0]));
                    currentStreak.clear();
                    currentStreak.add(newT);
                }
            }
        }

        System.out.println("OUT" + currentStreak);*/
    }

    private void parse() {
        while (tokens.hasNext()) {
            parseStatement();
            tokens.skip(TokenType.LINE_END);
        }

        System.out.println(">> TICK:");
        for (String c : tickCommands) {
            System.out.println(c);
        }
        for (Map.Entry<String,List<String>> f : functions.entrySet()) {
            System.out.println(">> FUNCTION " + f.getKey());
            for (String c : f.getValue()) {
                System.out.println(c);
            }
        }
    }

    private List<String> parseStatement() {
        List<String> list = new ArrayList<>();
        if (scope == ScopeType.GLOBAL) {
            parseGlobal();
        } else {
            list.addAll(parseNormal());
        }
        return list;
    }

    private void parseGlobal() {
        Token t = tokens.next();
        switch (t.getValue()) {
            case "tick":
                parseTick();
                break;
            case "function": {
                String name = tokens.next(TokenType.IDENTIFIER);
                if (functions.containsKey(name))
                    throw new RuntimeException("Duplicate function " + name);
                tokens.expect('{');
                scope = ScopeType.NORMAL;
                functions.put(name, parseBlock());
                scope = ScopeType.GLOBAL;
                break;
            }
            case "const": {
                String name = tokens.next(TokenType.IDENTIFIER);
                if (consts.containsKey(name))
                    throw new RuntimeException("Duplicate constants " + name);
                tokens.expect('=');
                int value = Integer.parseInt(tokens.next(TokenType.INT));
                consts.put(name, value);
                break;
            }
            case "global": {
                String name = tokens.next(TokenType.IDENTIFIER);
                if (globals.contains(name))
                    throw new RuntimeException("Duplicate globals " + name);
                globals.add(name);
                break;
            }
        }
    }

    private void parseTick() {
        tokens.expect('{');
        tokens.nextLine();
        scope = ScopeType.NORMAL;
        List<String> cmds = parseBlock();
        scope = ScopeType.GLOBAL;
        tickCommands.addAll(cmds);
    }

    private List<String> parseBlock() {
        List<String> list = new ArrayList<>();
        while (!tokens.isNext("}")) {
            if (!tokens.isNext(TokenType.LINE_END)) {
                list.addAll(parseStatement());
            }
            tokens.expect(TokenType.LINE_END);
        }
        tokens.skip("}");
        return list;
    }

    private List<String> parseNormal() {
        List<String> list = new ArrayList<>();
        String token = tokens.nextValue();
        switch (token){
            case "{":
                list.addAll(parseBlock());
                break;
            case "print":
                list.add("say " + tokens.next(TokenType.STRING));
                break;
            case "if":
                list.addAll(parseIf());
                break;
            case "@":
                list.addAll(selectors.parseSelectorCommand());
                break;
            case "bossbar":
                String id = parseResourceLocation(false);
                String displayName = readJsonText();
                list.add("bossbar add " + id + " " + displayName);
                variables.put(id,VariableType.BOSSBAR);
                break;
            case "clone": {
                String clone = "clone " + readPosition() + " " + readPosition() + " " + readPosition();
                String block = "";
                if (!tokens.isNext(TokenType.LINE_END)) {
                    if (tokens.skip("no_air", "non_air", "nonair", "masked")) {
                        clone += " masked";
                    } else if (tokens.skip("filter", "filtered", "only")) {
                        tokens.expect('(');
                        block = parseBlockId(true);
                        tokens.expect(')');
                        clone += " filtered";
                    } else if (tokens.skip("replace", "all")) {
                        clone += " replace";
                    } else {
                        throw new RuntimeException("Invalid mask mode for clone command: " + tokens.nextValue());
                    }

                    if (!tokens.isNext(TokenType.LINE_END)) {
                        if (tokens.skip("force", "forced", "overlap")) {
                            clone += " force";
                        } else if (tokens.skip("move")) {
                            clone += " move";
                        } else if (tokens.skip("normal", "copy")) {
                            clone += " normal";
                        } else {
                            throw new RuntimeException("Invalid clone mode for clone command: " + tokens.nextValue());
                        }
                    }
                }
                clone += " " + block;
                list.add(clone);

                break;
            }
            case "fill": {
                String fill = "fill " + readPosition() + " " + readPosition();
                fill += " " + parseBlockId(false);
                if (tokens.isNext(TokenType.IDENTIFIER)) {
                    String mode = tokens.expect("destroy","keep","replace","hollow","outline");
                    fill += " " + mode;
                    if ("replace".equals(mode)) {
                        tokens.skip("(");
                        fill += " " + parseBlockId(true);
                        tokens.skip(")");
                    }
                }
                list.add(fill);
                break;
            }
            case "summon":
                String entity = parseResourceLocation(false);
                if (!entityIds.contains(entity.substring(entity.indexOf(':')+1))) {
                    throw new RuntimeException("Unknown entity ID " + entity);
                }
                list.add("summon " + entity + " " + readPosition());
                break;
            case "for": {
                tokens.expect("@");
                String selector = selectors.parseSelector();
                List<String> cmds = parseStatement();
                String fName = generateFunction(cmds);
                list.add("execute as " + selector + " at @s run function " + fName);
                break;
            }
            case "as":
            case "at": {
                tokens.expect('@');
                String selector = selectors.parseSelector();
                list.add(chainExecute(token + " " + selector));
                break;
            }
            case "facing":
            case "face": {
                String args;
                if (tokens.skip("@")) {
                    String selector = selectors.parseSelector();
                    tokens.expect('.');
                    String anchor = tokens.expect("feet","eyes");
                    args = "entity " + selector + " " + anchor;
                } else {
                    args = readPosition();
                }
                list.add("facing " + args);
                break;
            }
            case "in":
                String dim = tokens.expect("overworld","the_nether","the_end");
                list.add(chainExecute("in " + dim));
                break;
            case "offset":
            case "positioned": {
                String args;
                if (tokens.skip("@")) {
                    args = "as " + selectors.parseSelector();
                } else {
                    args = readPosition();
                }
                list.add(chainExecute("positioned " + args));
                break;
            }
            case "rotate":
            case "rotated": {
                String args;
                if (tokens.skip("@")) {
                    args = "as " + selectors.parseSelector();
                } else {
                    args = readRotation();
                }
                list.add(chainExecute("rotated " + args));
                break;
            }
            case "defaultgamemode":
                tokens.skip("=");
                if (tokens.isNext(TokenType.LINE_END)) {
                    list.add("defaultgamemode");
                } else {
                    list.add("defaultgamemode " + parseIdentifierOrIndex("gamemode",gamemodes));
                }
                break;
            case "difficulty":
                tokens.skip("=");
                if (tokens.isNext(TokenType.LINE_END)) {
                    list.add("difficulty");
                } else {
                    list.add("difficulty " + parseIdentifierOrIndex("difficulty",difficulties));
                }
                break;
            case "worldspawn":
                tokens.skip("=");
                list.add("setworldspawn " + readPosition());
                break;
            case "time":
                if (tokens.skip("=") || !tokens.isNext(TokenType.LINE_END)) {
                    if (tokens.isNext(TokenType.INT)) {
                        list.add("time set " + tokens.nextValue());
                    } else {
                        list.add("time set " + tokens.expect("day","night","midnight","noon"));
                    }
                } else if (tokens.isNext("+=")) {
                    list.add("time add " + tokens.next(TokenType.INT));
                } else if (tokens.skip(".")) {
                    list.add("time query " + tokens.expect("day","daytime","gametime"));
                }
                break;
            default:
                VariableType varType = variables.get(token);
                if (varType != null) {
                    switch (varType) {
                        case BOSSBAR:
                            list.add(parseBossbarCommand(token));
                        default:
                    }
                }
        }
        return list;
    }

    private String chainExecute(String chain) {
        List<String> cmds = parseStatement();
        if (cmds.size() == 1) {
            if (cmds.get(0).startsWith("execute")) {
                return "execute " + chain + cmds.get(0).substring("execute".length());
            } else {
                return "execute " + chain + " run " + cmds.get(0);
            }
        } else {
            String fName = generateFunction(cmds);
            return "execute " + chain + " run function " + fName;
        }
    }

    private String parseBossbarCommand(String bossbar) {
        tokens.expect('.');
        String field = tokens.next(TokenType.IDENTIFIER);
        switch (field) {
            case "color":
                tokens.expect('=');
                String color = tokens.expect("blue","green","pink","purple","red","white","yellow");
                return "bossbar set " + bossbar + " color " + color;
            case "max":
                tokens.expect('=');
                int max = Integer.parseInt(tokens.next(TokenType.INT));
                return "bossbar set " + bossbar + " max " + max;
            case "name":
                tokens.expect('=');
                String name = readJsonText();
                return "bossbar set " + bossbar + " name " + name;
            case "players":
                tokens.expect('=');
                tokens.expect('@');
                String selector = selectors.parseSelector();
                return "bossbar set " + bossbar + " players " + selector;
            case "style":
                tokens.expect('=');
                String style = tokens.expect("notched_6","notched_10","notched_12","notched_20","progress");
                return "bossbar set " + bossbar + "style " + style;
            case "value":
                tokens.expect('=');
                int value = Integer.parseInt(tokens.next(TokenType.INT));
                return "bossbar set " + bossbar + " value " + value;
            case "visible":
                tokens.expect('=');
                String bool = tokens.expect("true","false");
                return "bossbar set " + bossbar + " visible " + bool;
            case "show":
            case "display":
                tokens.expect('(');tokens.expect(')');
                return "bossbar set " + bossbar + " visible true";
            case "hide":
                tokens.expect('(');tokens.expect(')');
                return "bossbar set " + bossbar + " visible false";
            case "remove":
                tokens.expect('(');tokens.expect(')');
                return "bossbar remove " + bossbar;
        }
        throw new RuntimeException("Unknown bossbar field/command " + field);
    }

    /**
     * Parses an identifier or an index of the identifier from the specified values. Used currently for gamemodes and difficulties.
     * @param name The name of the items. Used to throw an exception.
     * @param values The ids of the values
     * @return The matched identifier
     */
    public String parseIdentifierOrIndex(String name, String... values) {
        if (tokens.isNext(TokenType.INT)) {
            int index = Integer.parseInt(tokens.next(TokenType.INT));
            if (index < 0 || index > values.length) {
                throw new RuntimeException("Invalid gamemode index " + index + "must be 0-3");
            }
            return values[index];
        } else if (tokens.isNext(TokenType.IDENTIFIER)) {
            for (String v : values) {
                if (v.equalsIgnoreCase(tokens.peek().getValue())) {
                    tokens.skip();
                    return v;
                }
            }
        }
        throw new RuntimeException("Invalid " + name + " id " + tokens.peek());
    }

    public static final String[] gamemodes = new String[]{"survival","creative","adventure","spectator"};
    private static final String[] difficulties = new String[]{"peaceful","easy","normal","hard"};
    public static final Map<String,Integer> INVENTORY_SIZES = new HashMap<String, Integer>(){{
        put("inventory",27);
        put("hotbar",9);
        put("container",54);
        put("enderchest",27);
        put("horse",15);
        put("villager",8);
    }};
    public static final Map<String,String> ARMOR_SLOT_NAMES = new HashMap<String, String>(){{
        put("chestplate","chest");
        put("boots","feet");
        put("leggings","legs");
        put("helmet","head");
    }};

    public String parseResourceLocation(boolean taggable) {
        String loc = "";
        if (taggable && tokens.skip("#")) {
            loc += "#";
        }
        loc += tokens.next(TokenType.IDENTIFIER);
        boolean checkPath = false;
        if (tokens.skip(":")) {
            loc += ":";
            checkPath = true;
        }
        if (tokens.skip("/") || checkPath) {
            if (!checkPath) {
                loc += "/";
            }
            while (true) {
                loc += tokens.next(TokenType.IDENTIFIER);
                if (tokens.skip("/")) {
                    loc += "/";
                } else {
                    break;
                }
            }
        }
        return loc;
    }

    public int readOptionalInt() {
        if (tokens.isNext(TokenType.INT)) return Integer.parseInt(tokens.next(TokenType.INT));
        return 1;
    }

    public String parseItemId(boolean tag) {
        String id = parseResourceLocation(tag);
        if (tokens.isNext("{")) {
            id += parseNBT();
        }
        return id;
    }

    public Duration parseDuration() {
        Duration d = Duration.ofNanos(0);
        int n = Integer.parseInt(tokens.next(TokenType.INT));
        if (tokens.isNext(TokenType.IDENTIFIER)) {
            while (true) {
                String unit = tokens.next(TokenType.IDENTIFIER);
                switch (unit) {
                    case "s":
                    case "seconds":
                    case "secs":
                        d = d.plusSeconds(n);
                        break;
                    case "t":
                    case "ticks":
                        d = d.plusMillis(n * 50);
                        break;
                    case "m":
                    case "mins":
                    case "minutes":
                        d = d.plusMinutes(n);
                        break;
                    case "h":
                    case "hrs":
                    case "hours":
                        d = d.plusHours(n);
                        break;
                    case "d":
                    case "days":
                        d = d.plusDays(n);
                        break;
                        default:
                            throw new RuntimeException("Invalid duration unit " + unit);
                }
                if (tokens.isNext(TokenType.INT)) {
                    n = Integer.parseInt(tokens.next(TokenType.INT));
                } else {
                    break;
                }
            }
        } else {
            d = Duration.ofSeconds(n);
        }
        return d;
    }

    private static Map<Character,Integer> romanToNumber = new HashMap<Character, Integer>(){{
        put('I',1);
        put('V',5);
        put('X',10);
        put('L',50);
        put('C',100);
        put('D',500);
        put('M',1000);
    }};

    public int readRomanNumber(String roman) {
        int res = 0;
        for (int i = 0; i<roman.length(); i++) {
            int s1 = romanToNumber.get(roman.charAt(i));
            if (i+1 < roman.length()) {
                int s2 = romanToNumber.get(roman.charAt(i+1));
                if (s1 >= s2) {
                    res += s1;
                }
                else {
                    res += s2 - s1;
                    i++;
                }
            }
            else {
                res += s1;
                i++;
            }
        }
        return res;
    }

    public static final List<String> entityIds = Arrays.asList("creeper","skeleton","item","tnt","spider","zombie","ender_dragon");

    /**
     * Reads position coordinates. Joins 3 {@link #readCoordinate()} calls.
     */
    private String readPosition() {
        String pos = "";
        for (int i = 0; i < 3; i++) {
            pos += readCoordinate() + " ";
        }
        return pos.trim();
    }

    /**
     * Reads rotation coordinates. Joins 2 {@link #readCoordinate()} calls.
     */
    private String readRotation() {
        String pos = "";
        for (int i = 0; i < 2; i++) {
            pos += readCoordinate() + " ";
        }
        return pos.trim();
    }

    /**
     * Reads a single coordinate (absolute, relative (with ~) or rotated (with ^)
     * @return A valid coordinate string
     */
    private String readCoordinate() {
        if (tokens.skip("~")) {
            if (tokens.isNext(TokenType.DOUBLE,TokenType.INT)) return "~" + tokens.nextValue();
            return "~";
        } else if (tokens.skip("^")) {
            if (tokens.isNext(TokenType.DOUBLE,TokenType.INT)) return "^" + tokens.nextValue();
            return "^";
        } else if (tokens.isNext(TokenType.DOUBLE,TokenType.INT)) return tokens.nextValue();
        throw new RuntimeException("Invalid position coordinate! " + tokens.peek());
    }

    /**
     * Parses a block ID in the format of <code>minecraft:block[property=value]{SomeNBT:"value"}</code>
     * @param tag Whether or not to read block tags (block ids that start with #)
     * @return A valid block state selector
     */
    private String parseBlockId(boolean tag) {
        String block = parseResourceLocation(tag);
        boolean hadState = false;
        if (tokens.skip("[")) {
            block += parseState();
            hadState = true;
        }
        if (tokens.isNext("{")) {
            block += parseNBT();
        }
        if (!hadState && tokens.skip("[")) {
            block += parseState();
        }
        return block;
    }

    /**
     * Parses an NBT object. Expects to have the next token be a curly bracket '{'
     * @return A validated NBT tag compound
     */
    public String parseNBT() {
        tokens.expect('{');
        String nbt = "{";
        while (!tokens.isNext("}")) {
            System.out.println(tokens.peek());
            nbt += tokens.next(TokenType.IDENTIFIER);
            tokens.expect(':');
            nbt += ":";
            nbt += parseNBTValue();
            System.out.println(nbt);
            if (tokens.skip(",")) {
                nbt += ",";
            } else if (!tokens.isNext("}")) {
                System.out.println(tokens.peek());
                throw new RuntimeException("Expected } or , after NBT entry");
            }
        }
        tokens.skip();
        nbt += "}";
        return nbt;
    }

    /**
     * Parses an NBT value. This can be any valid NBT value, including string literals, numbers, arrays and NBT objects (using {@link #parseNBT()}).
     * @return A string of the valid minecraft NBT.
     */
    private String parseNBTValue() {
        if (tokens.isNext(TokenType.INT,TokenType.DOUBLE)) {
            String v = tokens.nextValue();
            if (tokens.isNext(TokenType.IDENTIFIER)) {
                if (tokens.isNext("d","D","s","S","F","f","B","b")) {
                    v += tokens.nextValue();
                } else {
                    throw new RuntimeException("Invalid number suffix " + tokens.peek().getValue());
                }
            }
            return v;
        } else if (tokens.isNext(TokenType.STRING)) {
            return  "\"" + tokens.nextValue() + "\"";
        } else if (tokens.isNext("{")) {
            return parseNBT();
        } else if (tokens.skip("[")) {
            String arr = "[";
            while (!tokens.isNext("]")) {
                arr += parseNBTValue();
                if (tokens.skip(",")) {
                    arr += ",";
                } else if (!tokens.isNext("]")) {
                    throw new RuntimeException("Expected ] or , after NBT array value");
                }
            }
            tokens.skip();
            arr += "]";
            return arr;
        }
        throw new RuntimeException("Invalid NBT value!");
    }

    private String parseState() {
        String state = "[";
        while (!tokens.isNext("]")) {
            state += tokens.next(TokenType.IDENTIFIER);
            tokens.expect('=');
            state += "=" + tokens.next(TokenType.IDENTIFIER);
            if (tokens.isNext(",")) state += ",";
            if (!tokens.isNext("]",",")) throw new RuntimeException("Invalid block state! expected ] or ,");
        }
        tokens.skip("]");
        state += "]";
        return state;
    }

    public String readJsonText() {
        if(tokens.isNext(TokenType.STRING) || tokens.isNext("{", "[")) {
            Token t = tokens.next();

            // Handling single strings
            if(t.getType() == TokenType.STRING) return "\"" + t.getValue() + "\"";

            // Handling things in brackets
            String s = t.getValue();
            String closer = s.equals("{") ? "}" : "]";

            String out = "" + s;

            int bracket = 1;
            while(bracket > 0) {
                t = tokens.next();
                if(t.getValue().equals(s)) {
                    bracket++;
                } else if(t.getValue().equals(closer)) {
                    bracket--;
                }

                if(t.getType() != TokenType.LINE_END) {
                    String added = t.getValue();
                    if(t.getType() == TokenType.STRING) {
                        added = "\"" + added + "\"";
                    }

                    out += added;
                }
            }

            return out;
        }

        throw new RuntimeException("Unexpected token " + tokens.peek() + " when parsing json!");
    }

    private List<String> parseIf() {
        Condition cond = parseCondition();
        List<String> then = parseStatement();
        String command;
        if (then.size() == 1) {
            command = then.get(0);
        } else {
            command = "function " + generateFunction(then);
        }
        System.out.println("command = " + command);
        return cond.toCommands(this, command).stream().map(c->"execute " + c + (cond instanceof JoinedCondition ? "" : " run " + command)).collect(Collectors.toList());
    }

    /**
     * Parses a condition tree. Used for <code>execute if...</code>.
     * @return A {@link Condition} object that holds the information for generating the execute commands, using {@link Condition#toCommands(Parser, String)}.
     */
    private Condition parseCondition() {
        Token t = tokens.next();
        switch (t.getValue()) {
            case "this":
                tokens.expect('.');
                String field = tokens.next(TokenType.IDENTIFIER);
                return parseScoreOperators("@s " + field, false);
            case "(":
                Condition c = parseCondition();
                tokens.expect(')');
                return chainConditions(c);
            case "@":
                String selector = selectors.parseSelector();
                if (tokens.skipAll(".","exists","(",")")) {
                    return chainConditions(new EntityExistsCondition(selector));
                }
                break;
        }
        switch (t.getType()) {
            case IDENTIFIER:
                if (!globals.contains(t.getValue()) && !consts.containsKey(t.getValue()))
                    throw new RuntimeException("Unknown constant " + t.getValue());
                return parseScoreOperators(getVariableAccess(t.getValue()),false);
            case INT:
                return parseScoreOperators(t.getValue(),true);
                default:
                    String pos;
                    try {
                        tokens.pushBack();
                        pos = readPosition();
                    } catch (Exception e) {
                        throw new RuntimeException("Invalid token " + t + " in condition",e);
                    }
                    if (tokens.skip(",")) {
                        String end = readPosition();
                        tokens.expect("=","==");
                        String mode = "all";
                        String dest;
                        if (tokens.skip("masked","mask")) {
                            mode = "masked";
                            tokens.expect('(');
                            dest = readPosition();
                            tokens.expect(')');
                        } else if (tokens.skip("all")){
                            tokens.expect('(');
                            dest = readPosition();
                            tokens.expect(')');
                        } else {
                            dest = readPosition();
                        }
                        return chainConditions(new BlockAreaCondition(pos,end,dest,mode));
                    } else {
                        if (tokens.skip("has")) {
                            tokens.expect('(');
                            String path = tokens.next(TokenType.STRING);
                            tokens.expect(')');
                            return chainConditions(new HasDataCondition("block",pos,path));
                        }
                        tokens.expect("=","==");
                        String block = parseBlockId(true);
                        return chainConditions(new BlockCondition(pos, block));
                    }
        }
    }

    /**
     *
     * checks if this variable is a const or a local var, and creates a &lt;name&gt; &lt;objective&gt;
     *
     */
    private String getVariableAccess(String name) {
        if (consts.containsKey(name)) {
            return name + " Constants";
        }
        if (globals.contains(name)) {
            return name + " Global";
        }
        return "@s " + name;
    }

    /**
     * Parses operators after a score access, for comparing 2 score values. Used for if(condition)s
     * @param first The first score access
     * @param literal Whether this score is a literal value, aka a constant hardcoded number.
     * @return A {@link ScoreCondition} joined by the next condition.
     */
    private Condition parseScoreOperators(String first, boolean literal) {
        String op = tokens.peek().getValue();
        switch (op) {
            case "<":
            case "<=":
            case ">":
            case ">=":
                break;
            case "==":
                op = "=";
                break;
                default:
                    return null;
        }
        tokens.skip();
        Token secondTok = tokens.next();
        String second = "";
        boolean secondLiteral = false;
        switch (secondTok.getType()) {
            case IDENTIFIER:
                if (!globals.contains(secondTok.getValue()) && !consts.containsKey(secondTok.getValue()))
                    throw new RuntimeException("Unknown variable " + secondTok.getValue());
                second = getVariableAccess(secondTok.getValue());
                break;
            case INT:
                second = secondTok.getValue();
                secondLiteral = true;
                break;
            default:
                throw new RuntimeException("Invalid token in condition");
        }

        return chainConditions(new ScoreCondition(new Value(first,literal),op,new Value(second,secondLiteral)));
    }

    /**
     * Chains two conditions together. Checks for logic gates.
     * @param cond The first condition
     * @return A {@link JoinedCondition} of the given condition and the next condition, or just the given condition if no logic gate token is present.
     */
    private Condition chainConditions(Condition cond) {
        String chain = tokens.peek().getValue();
        switch (chain) {
            case "&&":
            case "||":
                tokens.skip();
                return new JoinedCondition(chain,cond,parseCondition());
        }
        return cond;
    }


    public String generateFunction(List<String> commands) {
        String s = "autogenrated" + autoGenerated;
        functions.put(s,commands);
        autoGenerated++;
        return s;
    }
}
