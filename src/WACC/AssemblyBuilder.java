package WACC;

/**
 * Created by yh6714 on 03/12/15.
 */
public class AssemblyBuilder {

    //StringBuilder for main scope
    private StringBuilder main;
    //StringBuilder for header scope
    private StringBuilder header;
    private StringBuilder function;
    private StringBuilder label;
    private StringBuilder current;
    private StringBuilder affa;

    public AssemblyBuilder(StringBuilder main, StringBuilder header, StringBuilder function, StringBuilder label, StringBuilder current) {
        this.main = main;
        this.header = header;
        this.function = function;
        this.label = label;
        this.current = current;
    }

    public StringBuilder getMain() {
        return main;
    }

    public StringBuilder getHeader() {
        return header;
    }

    public StringBuilder getFunction() {
        return function;
    }

    public StringBuilder getLabel() {
        return label;
    }

    public StringBuilder getCurrent() {
        return current;
    }

    public void setMain(StringBuilder main) {
        this.main = main;
    }

    public void setHeader(StringBuilder header) {
        this.header = header;
    }

    public void setFunction(StringBuilder function) {
        this.function = function;
    }

    public void setLabel(StringBuilder label) {
        this.label = label;
    }

    public void setCurrent(StringBuilder current) {
        this.current = current;
    }
}
