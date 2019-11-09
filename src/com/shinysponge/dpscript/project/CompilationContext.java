package com.shinysponge.dpscript.project;

import com.shinysponge.dpscript.entities.Entities;
import com.shinysponge.dpscript.oop.AbstractClass;
import com.shinysponge.dpscript.oop.DPClass;
import com.shinysponge.dpscript.oop.LazyValue;
import com.shinysponge.dpscript.pawser.CompilationError;
import com.shinysponge.dpscript.pawser.GlobalLaterCheck;
import com.shinysponge.dpscript.tokenizew.CodePos;
import com.shinysponge.dpscript.tokenizew.Token;

import java.util.*;
import java.util.stream.Collectors;

public class CompilationContext {

    private List<Datapack> imports;
    private Datapack project;
    private DPScript file;

    public Map<String, Integer> consts = new HashMap<>();
    public List<String> bossbars = new ArrayList<>();
    public List<String> objectives = new ArrayList<>();
    public List<String> globals = new ArrayList<>();
    public List<String> triggers = new ArrayList<>();
    private int autoGenerated = 1;
    private List<CompilationError> errors = new ArrayList<>();
    private MCFunction load;
    private MCFunction tick;
    private Map<String, MCFunction> functions = new HashMap<>();
    public List<GlobalLaterCheck> checks = new ArrayList<>();
    public Map<Token, String[]> suggestions = new HashMap<>();
    public Map<String, AbstractClass> classes = new HashMap<>();
    public Stack<Map<String,LazyValue<?>>> variables = new Stack<>();
    private String path = "";

    public CompilationContext(Datapack project) {
        this.project = project;
        this.load = addFunction("init",FunctionType.LOAD);
        this.tick = addFunction("loop",FunctionType.TICK);
        this.variables.push(new HashMap<>());
        this.classes.put("int",DPClass.INT);
        this.classes.put("string",DPClass.STRING);
        this.classes.put("boolean",DPClass.BOOLEAN);
        this.classes.put("double",DPClass.DOUBLE);
        for (Entities e : Entities.values()) {
            this.classes.put(e.getTypeClass().getName(),e.getTypeClass());
        }
    }

    public MCFunction addFunction(String name, FunctionType type) {
        MCFunction f = new MCFunction(project.getName(), path + name, type);
        functions.put(name,f);
        return f;
    }

    public void suggest(Token token, String... suggestions) {
        this.suggestions.put(token,suggestions);
    }

    public void setFile(DPScript script) {
        this.file = script;
    }

    public DPScript getFile() {
        return file;
    }

    public void addTick(String command) {
        tick.add(command);
    }

    public void addLoad(String command) {
        load.add(command);
    }

    public MCFunction getFunction(String name) {
        return this.functions.get(name.substring(name.indexOf(":")+1));
    }

    public void addError(CompilationError err) {
        System.out.println("added compilation error: ");
        err.printStackTrace(System.out);
        errors.add(err);
    }

    public boolean hasConstant(String name) {
        return consts.containsKey(name);
    }

    public boolean hasGlobal(String name) {
        System.out.println("globals: " + globals);
        return globals.contains(name);
    }

    public String generateFunctionName() {
        return "autogenerated" + (autoGenerated++);
    }

    public void addFunction(String name, List<String> commands) {
        MCFunction f = addFunction(name,FunctionType.FUNCTION);
        commands.forEach(f::add);
    }

    /**
     * Returns a /function [namespace]:[name] command.
     * if that function doesn't exist, will wait for the end of compilation and will throw a compilation error if it doesn't exist.
     * @param pos The token position of where the function was called
     * @param name The functions name
     */
    public String callFunction(CodePos pos, String name) {
        if (!functions.containsKey(name)) {
            checks.add(new GlobalLaterCheck(name,"function",pos,ctx->ctx.functions.containsKey(name)));
        }
        return "function " + project.getName() + ":" + name;
    }

    public void enterBlock() {
        variables.push(new HashMap<>());
    }

    public void exitBlock() {
        variables.pop();
    }

    public void putVariable(String name, LazyValue<?> value) {
        variables.peek().put(name,value);
    }

    public LazyValue<?> getVariable(String name) {
        return variables.peek().get(name);
    }

    public void runChecks() {
        checks.forEach(c->c.check(this));
    }

    public void logResults() {
        if (!errors.isEmpty()) {
            System.out.println(">>>>>>>>>>> COMPILATION FAILED <<<<<<<<<<<<<");
            for (CompilationError e : errors.stream().sorted(Comparator.comparing(CompilationError::getPos)).collect(Collectors.toList())) {
                System.out.println("line " + e.getPos().getLine() + ": " + e.getMessage());
                System.out.println("------------------------");
            }
        } else {
            System.out.println(">>>>>>>>>>>> COMPILATION SUCCEED <<<<<<<<<<<<");
            for (Map.Entry<String, MCFunction> f : functions.entrySet()) {
                System.out.println(">> FUNCTION " + f.getKey());
                f.getValue().forEachCommand(System.out::println);
            }
        }
    }

    public void ensureConstants() {
        ensureObjective("Consts");
    }

    public void ensureGlobal() {
        ensureObjective("Global");
    }

    public String getNamespace() {
        return project.getName();
    }

    public CompilationResults getResults() {
        return new CompilationResults(project,errors,functions,suggestions);
    }

    public boolean hasClass(String name) {
        return classes.containsKey(name);
    }

    public void ensureObjective(String name) {
        if (!objectives.contains(name)) {
            objectives.add(name);
            addLoad("scoreboard objectives add " + name + " dummy");
        }
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
