package WACC;


import java.util.HashMap;
import java.util.List;

/**
 * Created by yh6714 on 13/11/15.
 */

/*
 * All unused methods and fields are the prepartion of the code generation part
 */
public class AST {

    private Registers registers = new Registers();

    private ProgramNode root;

    private boolean ifDeclarationCodeGenerated = false;

    private Stack currentStack;

    private Stack funcStack = new Stack();

    private Stack programStack = new Stack();

    private String resultReg = registers.get(0).toString();

    private Registers.Register currentlyUsedRegister;

    // label counters
    private int messageCount = 0;
    private int labelCount = 0;
    private int loopCount = 0;


    public ProgramNode getRoot() {
        return root;
    }

    public void setRoot(ProgramNode root) {
        this.root = root;
    }

    public int getWordLength(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\\') {
                count++;
                i++;
            } else {
                count++;
            }
        }
        return count - 2;
    }

    public boolean isIfDeclarationCodeGenerated() {
        return ifDeclarationCodeGenerated;
    }

    public void setIfDeclarationCodeGenerated(boolean ifDeclarationCodeGenerated) {
        this.ifDeclarationCodeGenerated = ifDeclarationCodeGenerated;
    }


    /*The base of all other nodes and other nodes need to implement the methods
    * Containing:
    * Symboltable as hashmap
    * Parent of the current node
    * Check method to do the semantic check
    * */
    public abstract class ASTNode {

        protected HashMap<String, ASTNode> symbolTable = new HashMap<>();

        protected ASTNode parent;
        protected boolean scope = false;


        public boolean getScope() {
            return scope;
        }

        public void setScope(boolean scope) {
            this.scope = scope;
        }

        public ASTNode getParent() {
            return parent;
        }

        public void setParent(ASTNode parent) {
            this.parent = parent;
        }

        public HashMap<String, ASTNode> getSymbolTable() {
            return symbolTable;
        }

        public void checkIfVaribleExist(IdentNode identNode) {
            ASTNode parent = getParent();
            boolean found = false;
            while (parent != null) {
                if (parent.getSymbolTable().containsKey(identNode.getIdent())) {
                    found = true;
                }
                parent = parent.getParent();
            }
            if (found == false) {
                throwSemanticError(identNode.getIdent() + " has not been decleared before");
            }
        }

        public void throwSyntaxError(String errorMessage) {
            System.out.println(errorMessage);
            System.out.println("#syntax_error#");
            System.exit(100);
        }

        public void throwSemanticError(String errorMessage) {
            System.out.println(errorMessage);
            System.out.println("#semantic_error#");
            System.exit(200);
        }

        public abstract String getType();

        public abstract String getValue();

        public abstract void check();

        public abstract void generate(AssemblyBuilder builder);



        public void addBackToStack(AssemblyBuilder builder) {
            int stackSize = currentStack.getSize();
            if(stackSize > 0) {
                int num = stackSize / Stack.MAX_STACK_SIZE;
                int remainder = stackSize % Stack.MAX_STACK_SIZE;
                for (int i = 0; i < num; i++) {
                    builder.getCurrent().append("ADD sp, sp, #" + Stack.MAX_STACK_SIZE + "\n");
                }
                builder.getCurrent().append("ADD sp, sp, #" + remainder + "\n");
            }
        }

        protected int calculateNumOfByte(String type) {
            switch (type) {
                case "Int":
                    return 4;
                case "String":
                    return 4;
                case "Bool":
                    return 1;
                case "Char":
                    return 1;
                default:
                    return 4;
            }
        }
    }

    /*
     * The basic program node, containing two symbol tables for functions and program statements.
     */
    public class ProgramNode extends ASTNode {

        private List<FuncNode> functionNodes;
        private StatNode statNode;
        private HashMap<String, ASTNode> functionSymbolTable = new HashMap<>();

        public ProgramNode(List<FuncNode> functionNodes, StatNode statNode) {

            this.functionNodes = functionNodes;
            for (FuncNode funcNode : functionNodes) {
                funcNode.setParent(this);
            }
            this.statNode = statNode;
            statNode.setParent(this);
            setRoot(this);
        }

        public HashMap<String, ASTNode> getFunctionSymbolTable() {
            return functionSymbolTable;
        }

        @Override
        public String getType() {
            return "Program";
        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void check() {

            setScope(true);

            currentStack = funcStack;
            for (FuncNode funcNode : functionNodes) {
                if (getFunctionSymbolTable().containsKey(funcNode.getIdentNode().getIdent())) {
                    throwSemanticError("The function " + funcNode.getIdentNode().getIdent()
                            + " cannot be overloaded");
                } else {
                    getFunctionSymbolTable().put(funcNode.getIdentNode().getIdent(), funcNode);
                }
            }

            for (FuncNode funcNode : functionNodes) {
                funcNode.check();
            }

            currentStack = programStack;

            statNode.check();
        }

        public void generate(AssemblyBuilder builder) {
            builder.setCurrent(builder.getMain());
            builder.getHeader().append(".data\n");
            builder.getMain().append("main: \n");
            builder.getMain().append("PUSH {lr}  \n");
            currentlyUsedRegister = registers.getFirstEmptyRegister();


            currentStack = funcStack;
            for (FuncNode funcNode : functionNodes) {
                funcNode.generate(builder);
            }

            currentlyUsedRegister.setValue(null);
            builder.setCurrent(builder.getMain());

            currentStack = programStack;
            statNode.generate(builder);

            if((statNode instanceof MultipleStatNode) && isIfDeclarationCodeGenerated()) {
                addBackToStack(builder);
            }
            setIfDeclarationCodeGenerated(false);
            statNode.setValue();
            builder.getHeader().append(".text\n");
            builder.getHeader().append(".global main\n");
            builder.getMain().append("MOV " + resultReg + ", #0\n");
            registers.get(0).setValue(true);
            builder.getMain().append("POP {pc}\n");
        }
    }

    /*
     * Representing the functions declare in the program
     * Has program node as parent
     * Has it own scope which is the symboltable
     */
    public class FuncNode extends ASTNode {

        private TypeNode typeNode;
        private IdentNode identNode;
        private List<ParamNode> paramNodes;
        private StatNode statNode;
        private String type;

        public FuncNode(TypeNode typeNode, IdentNode identNode, List<ParamNode> paramNodes, StatNode statNode) {

            type = "Function";
            this.typeNode = typeNode;
            typeNode.setParent(this);
            this.statNode = statNode;
            statNode.setParent(this);
            this.paramNodes = paramNodes;
            for (ParamNode paramNode : paramNodes) {
                paramNode.setParent(this);
                paramNode.getTypeNode().setValue();
            }
            this.identNode = identNode;
            identNode.setParent(this);
        }

        @Override
        public String getType() {
            return typeNode.getType();
        }

        @Override
        public String getValue() {
            return null;
        }

        public IdentNode getIdentNode() {
            return identNode;
        }

        public List<ParamNode> getParamNodes() {
            return paramNodes;
        }

        @Override
        public void check() {

            for (int i = paramNodes.size() - 1; i >= 0; i--) {
                ParamNode paramNode = paramNodes.get(i);
                paramNode.check();
            }

            setScope(true);

            for (ParamNode paramNode : paramNodes) {
                if (this.getSymbolTable().containsKey(paramNode.getIdentNode().getIdent())) {
                    throwSemanticError("The function parameters cannot have same variable name");
                } else {
                    this.getSymbolTable().put(paramNode.getIdentNode().getIdent(), paramNode.getTypeNode());
                }
            }

            statNode.check();
        }

        @Override
        public void generate(AssemblyBuilder builder) {

            StringBuilder functionStringBuilder = builder.getFunction();
            builder.setCurrent(functionStringBuilder);
            functionStringBuilder.append("f_" + identNode.getIdent() + ": \n");
            functionStringBuilder.append("PUSH {lr}  \n");


            for (int i = paramNodes.size() - 1; i >= 0; i--) {
                ParamNode paramNode = paramNodes.get(i);
                currentStack.add(paramNode.getIdentNode().getIdent(), paramNode.getTypeNode().getNumOfByte());
            }

            currentStack.add(identNode.getIdent(), 4);
            statNode.generate(builder);

            if((statNode instanceof MultipleStatNode) && isIfDeclarationCodeGenerated()) {
                addBackToStack(builder);
            }
            setIfDeclarationCodeGenerated(false);
            statNode.setValue();
            functionStringBuilder.append("POP {pc}\n");
            functionStringBuilder.append(".ltorg\n");
        }

    }

    /*
     * Spliting all statement to different nodes
     * The command field is for the use of code generation
     */
    public abstract class StatNode extends ASTNode {

        protected String command;

        public StatNode() {
            command = "";
        }

        public String getCommand() {
            return command;
        }

        public Pair_typeNode getPair_typeNode(ASTNode node) {
            Pair_typeNode result;
            if (node.getClass().toString().contains("IdentNode")) {
                result = (Pair_typeNode) ((IdentNode) node).getTypeNode();
            } else if (node.getClass().toString().contains("SNDNode")) {
                result = (Pair_typeNode) ((SNDNode) node).getTypeNode();
            } else if (node.getClass().toString().contains("FSTNode")) {
                result = (Pair_typeNode) ((FSTNode) node).getTypeNode();
            } else if (node.getClass().toString().contains("CallNode")) {
                result = (Pair_typeNode) ((CallNode) node).getTypeNode();
            } else {
                result = (Pair_typeNode) node;
            }
            return result;
        }

        protected int getStackOffset() {
            int stackOffset = 0;
            if (this instanceof DeclarationNode) {
                stackOffset = currentStack.getStackElemOffset(((DeclarationNode) this).identNode.getIdent());
            } else if (this instanceof AssignmentNode) {
                ASTNode assignLHS = ((AssignmentNode) this).assign_lhsNode;
                String ident;
                if (assignLHS instanceof IdentNode) {
                    ident = ((IdentNode) assignLHS).getIdent();
                } else if (assignLHS instanceof Array_elemNode) {
                    ident = ((Array_elemNode) assignLHS).identNode.getIdent();
                } else {
                    ident = ((Pair_elemNode) assignLHS).getIdentNode().getIdent();
                }
                stackOffset = currentStack.getStackElemOffset(ident);
            } else if (this instanceof IdentNode) {
                String ident = ((IdentNode) this).getIdent();
                TypeNode typeNode = ((IdentNode) this).getTypeNode();
                stackOffset = currentStack.getStackElemOffset(ident);
            }
            return stackOffset;
        }

        public abstract String getValue();

        protected String getStackPointer() {
            int stackOffset = getStackOffset();
            String result;
            if (stackOffset == 0) {
                result = ", [sp";
            } else {
                result = ", [sp, #" + stackOffset;
            }
            return result + "]";
        }

        public abstract void setValue();


        public void generateCheckArrayBounds(AssemblyBuilder builder) {
            if (!builder.getLabel().toString().contains("p_check_array_bounds:")) {
                builder.getLabel().append("p_check_array_bounds:\n");
                builder.getLabel().append("PUSH {lr}\n");
                builder.getLabel().append("CMP r0, #0\n");
                builder.getLabel().append("LDRLT r0, =msg_" + messageCount + "\n");

                builder.getHeader().append("msg_" + messageCount + ":\n");
                builder.getHeader().append(".word 44\n");
                builder.getHeader().append(".ascii\t\"ArrayIndexOutOfBoundsError: negative index\\n\\0\"\n");
                messageCount++;

                builder.getLabel().append("BLLT p_throw_runtime_error\n");
                builder.getLabel().append("LDR r1, [r4]\n");
                builder.getLabel().append("CMP r0, r1\n");
                builder.getLabel().append("LDRCS r0, =msg_" + messageCount + "\n");

                builder.getHeader().append("msg_" + messageCount + ":\n");
                builder.getHeader().append(".word 45\n");
                builder.getHeader().append(".ascii\t\"ArrayIndexOutOfBoundsError: index too large\\n\\0\"\n");
                messageCount++;

                builder.getLabel().append("BLCS p_throw_runtime_error\n");
                builder.getLabel().append("POP {pc}\n");
                if (!builder.getLabel().toString().contains("p_throw_runtime_error:")) {
                    builder.getLabel().append("p_throw_runtime_error:\n");
                    builder.getLabel().append("BL p_print_string\n");
                    builder.getLabel().append("MOV r0, #-1\n");
                    builder.getLabel().append("BL exit\n");

                    if (!builder.getLabel().toString().contains("p_print_string:")) {
                        builder.getLabel().append("p_print_string:\n");
                        builder.getLabel().append("PUSH {lr}\n");
                        Registers.Register registerZero = currentlyUsedRegister;
                        currentlyUsedRegister = registers.getFirstEmptyRegister();
                        builder.getLabel().append("LDR " + currentlyUsedRegister + ", [" + registerZero + "]\n");
                        currentlyUsedRegister.setValue(registerZero.getValue());
                        Registers.Register registerFirst = currentlyUsedRegister;
                        currentlyUsedRegister = registers.getFirstEmptyRegister();
                        builder.getLabel().append("ADD " + currentlyUsedRegister + ", " + registerZero + ", #4\n");

                        registerZero.setValue(null);
                        registerFirst.setValue(null);
                        currentlyUsedRegister.setValue(null);

                        currentlyUsedRegister = registers.getFirstEmptyRegister();

                        builder.getLabel().append("LDR " + currentlyUsedRegister + ", =msg_" + messageCount + "\n");
                        currentlyUsedRegister.setValue(true);
                        builder.getLabel().append("ADD " + currentlyUsedRegister + ", " + currentlyUsedRegister + ", #4\n");
                        builder.getLabel().append("BL printf\n");
                        builder.getLabel().append("MOV " + currentlyUsedRegister + ", #0\n");
                        builder.getLabel().append("BL fflush\n");
                        builder.getLabel().append("POP {pc}\n");

                        builder.getHeader().append("msg_" + messageCount + ":\n");
                        messageCount++;
                        builder.getHeader().append(".word 5\n");
                        builder.getHeader().append(".ascii\t\"%.*s\\0\"\n");

                        currentlyUsedRegister.setValue(null);
                    }
                }
            }
        }

        // generate code to print a array elem
        protected void generatePrintArrayElem(AssemblyBuilder builder, ExprNode exprNode) {
            Registers.Register currentlyUsedRegister = registers.getFirstEmptyRegister();
            Registers.Register register4 = registers.get(4);

            builder.getCurrent().append("LDR " + currentlyUsedRegister + ((Array_elemNode)exprNode).getIdentNode().getStackPointer() + "\n");
            currentlyUsedRegister.setValue(true);
            builder.getCurrent().append("PUSH {"  + register4 + "}\n");
            builder.getCurrent().append("MOV " + register4 + ", " + currentlyUsedRegister + "\n");
            register4.setValue(true);
            builder.getCurrent().append("LDR " + currentlyUsedRegister + ((Array_elemNode)exprNode).getIdentNode().getStackPointer() + "\n");
            currentStack.incSize(4);
            //builder.getCurrent().append("LDR " + currentlyUsedRegister + ", =" + ((Array_elemNode)exprNode).getIndex() + "\n");
            builder.getCurrent().append("BL p_check_array_bounds\n");
            generateCheckArrayBounds(builder);
            builder.getCurrent().append("ADD " + register4 + ", " + register4 + ", #4\n");
            //LSL #2 special case
            String lsl = "";
            if (exprNode.getType().contains("Int")) {
                lsl = ", LSL #2";
            }
            builder.getCurrent().append("ADD " + register4 + ", " + register4 + ", " + currentlyUsedRegister + lsl + "\n");
            builder.getCurrent().append("LDR " + register4 + ", [" + register4 + "]\n");
            builder.getCurrent().append("MOV " + currentlyUsedRegister + ", " + register4 + "\n");
            builder.getCurrent().append("POP {" + register4 + "}\n");
            currentStack.decSize(4);
            if (exprNode.getType().contains("Int")) {
                builder.getCurrent().append("BL p_print_int\n");
                if (!builder.getLabel().toString().contains("p_print_int:")) {
                    builder.getLabel().append("p_print_int:\n");
                    builder.getLabel().append("PUSH {lr}\n");
                    Registers.Register registerZero = currentlyUsedRegister;
                    registerZero.setValue(true);
                    currentlyUsedRegister = registerZero.getNext();
                    builder.getLabel().append("MOV " + currentlyUsedRegister + ", " + registerZero + "\n");
                    currentlyUsedRegister.setValue(true);

                    registerZero.setValue(null);
                    currentlyUsedRegister.setValue(null);
                    currentlyUsedRegister = registers.getFirstEmptyRegister();

                    builder.getLabel().append("LDR " + currentlyUsedRegister + ", =msg_" + messageCount + "\n");
                    builder.getLabel().append("ADD " + currentlyUsedRegister + ", " + currentlyUsedRegister + ", #4\n");
                    builder.getLabel().append("BL printf\n");
                    builder.getLabel().append("MOV " + currentlyUsedRegister + ", #0\n");
                    builder.getLabel().append("BL fflush\n");
                    builder.getLabel().append("POP {pc}\n");

                    builder.getHeader().append("msg_" + messageCount + ":\n");
                    messageCount++;
                    builder.getHeader().append(".word 3\n");
                    builder.getHeader().append(".ascii\t\"%d\\0\"\n");
                }
            } else if (exprNode.getType().contains("Char")) {
                generatePrintCharLiter(builder, exprNode);
            } else if (exprNode.getType().contains("Bool")) {
                generatePrintBoolLiter(builder, exprNode);
            }

            if (!builder.getLabel().toString().contains("p_print_int:")) {
                builder.getLabel().append("p_print_int:\n");
                builder.getLabel().append("PUSH {lr}\n");
                Registers.Register registerZero = currentlyUsedRegister;
                registerZero.setValue(true);
                currentlyUsedRegister = registers.getFirstEmptyRegister();
                builder.getLabel().append("MOV " + currentlyUsedRegister + ", " + registerZero + "\n");
                currentlyUsedRegister.setValue(true);

                registerZero.setValue(null);
                currentlyUsedRegister.setValue(null);
                currentlyUsedRegister = registers.getFirstEmptyRegister();

                builder.getLabel().append("LDR " + currentlyUsedRegister + ", =msg_" + messageCount + "\n");
                builder.getLabel().append("ADD " + currentlyUsedRegister + ", " + currentlyUsedRegister + ", #4\n");
                builder.getLabel().append("BL printf\n");
                builder.getLabel().append("MOV " + currentlyUsedRegister + ", #0\n");
                builder.getLabel().append("BL fflush\n");
                builder.getLabel().append("POP {pc}\n");

                builder.getHeader().append("msg_" + messageCount + ":\n");
                messageCount++;
                builder.getHeader().append(".word 3\n");
                builder.getHeader().append(".ascii\t\"%d\\0\"\n");
            }

            currentlyUsedRegister.setValue(null);

        }

        //generate assembly code to print a array address
        protected void generatePrintArrayAddress(AssemblyBuilder builder, ExprNode exprNode) {
            builder.getHeader().append("msg_" + messageCount +":\n");
            builder.getHeader().append(".word 3\n");
            builder.getHeader().append(".ascii\t\"%p\\0\"\n");

            Registers.Register r0 = registers.getFirstEmptyRegister();
            builder.getCurrent().append("LDR " + r0 + exprNode.getStackPointer() + "\n");
            r0.setValue(true);
            builder.getCurrent().append("BL p_print_reference\n");

            if (!builder.getLabel().toString().contains("p_print_reference:")) {
                Registers.Register r1 = r0.getNext();
                builder.getLabel().append("p_print_reference:\n");
                builder.getLabel().append("PUSH {lr}\n");
                builder.getLabel().append("MOV " + r1 + ", " + r0 + "\n");
                builder.getLabel().append("LDR " + r0 + ", =msg_" + messageCount + "\n");
                builder.getLabel().append("ADD " + r0 + ", " + r0 + ", #4\n");
                builder.getLabel().append("BL printf\n");
                builder.getLabel().append("MOV " + r0 + ", #0\n");
                builder.getLabel().append("BL fflush\n");
                builder.getLabel().append("POP {pc}\n");
                messageCount++;
                r1.setValue(null);
            }
            r0.setValue(null);
        }

        // generate code to print a char_liter
        protected void generatePrintCharLiter(AssemblyBuilder builder, ExprNode exprNode) {

            currentlyUsedRegister = registers.getFirstEmptyRegister();
            exprNode.generate(builder);
            currentlyUsedRegister.setValue(null);
            builder.getCurrent().append("BL putchar\n");
        }

        // generate code to print a int_liter
        protected void generatePrintIntLiter(AssemblyBuilder builder, ExprNode exprNode) {

            StringBuilder currentBuilder = builder.getCurrent();
            StringBuilder headerBuilder = builder.getHeader();
            StringBuilder labelBuilder = builder.getLabel();

            currentlyUsedRegister = registers.getFirstEmptyRegister();
            exprNode.generate(builder);

            currentBuilder.append("BL p_print_int\n");

            if (!builder.getLabel().toString().contains("p_print_int:")) {
                labelBuilder.append("p_print_int:\n");
                labelBuilder.append("PUSH {lr}\n");
                Registers.Register registerZero = currentlyUsedRegister;
                registerZero.setValue(true);
                currentlyUsedRegister = registers.getFirstEmptyRegister();
                labelBuilder.append("MOV " + currentlyUsedRegister + ", " + registerZero + "\n");
                currentlyUsedRegister.setValue(true);

                registerZero.setValue(null);
                currentlyUsedRegister.setValue(null);
                currentlyUsedRegister = registers.getFirstEmptyRegister();

                labelBuilder.append("LDR " + currentlyUsedRegister + ", =msg_" + messageCount + "\n");
                labelBuilder.append("ADD " + currentlyUsedRegister + ", " + currentlyUsedRegister + ", #4\n");
                labelBuilder.append("BL printf\n");
                labelBuilder.append("MOV " + currentlyUsedRegister + ", #0\n");
                labelBuilder.append("BL fflush\n");
                labelBuilder.append("POP {pc}\n");

                headerBuilder.append("msg_" + messageCount + ":\n");
                messageCount++;
                headerBuilder.append(".word 3\n");
                headerBuilder.append(".ascii\t\"%d\\0\"\n");
            }

            currentlyUsedRegister.setValue(null);

        }

        protected void generatePrintBoolLiter(AssemblyBuilder builder, ExprNode exprNode) {

            StringBuilder currentBuilder = builder.getCurrent();
            StringBuilder headerBuilder = builder.getHeader();
            StringBuilder labelBuilder = builder.getLabel();

            currentlyUsedRegister = registers.getFirstEmptyRegister();
            exprNode.generate(builder);

            currentBuilder.append("BL p_print_bool\n");

            if (!builder.getLabel().toString().contains("p_print_bool:")) {
                labelBuilder.append("p_print_bool:\n");
                labelBuilder.append("PUSH {lr}\n");
                labelBuilder.append("CMP " + currentlyUsedRegister + ", #0\n");
                currentlyUsedRegister.setValue(null);
                currentlyUsedRegister = registers.getFirstEmptyRegister();

                labelBuilder.append("LDRNE " + currentlyUsedRegister + ", =msg_" + messageCount + "\n");
                headerBuilder.append("msg_" + messageCount + ":\n");
                messageCount++;
                headerBuilder.append(".word 5\n");
                headerBuilder.append(".ascii\t\"true\\0\"\n");

                labelBuilder.append("LDREQ " + currentlyUsedRegister + ", =msg_" + messageCount + "\n");
                headerBuilder.append("msg_" + messageCount + ":\n");
                messageCount++;
                headerBuilder.append(".word 6\n");
                headerBuilder.append(".ascii\t\"false\\0\"\n");

                labelBuilder.append("ADD "+ currentlyUsedRegister + ", " + currentlyUsedRegister + ", #4\n");
                labelBuilder.append("BL printf\n");
                currentlyUsedRegister.setValue(null);
                currentlyUsedRegister = registers.getFirstEmptyRegister();
                labelBuilder.append("MOV " + currentlyUsedRegister + ", #0\n");
                currentlyUsedRegister.setValue(true);
                labelBuilder.append("BL fflush\n");
                labelBuilder.append("POP {pc}\n");

            }

            currentlyUsedRegister.setValue(null);

        }

        protected void generatePrintStringLiter(AssemblyBuilder builder, ExprNode exprNode) {

            StringBuilder currentBuilder = builder.getCurrent();
            StringBuilder headerBuilder = builder.getHeader();
            StringBuilder labelBuilder = builder.getLabel();

            currentlyUsedRegister = registers.getFirstEmptyRegister();
            exprNode.generate(builder);

            currentBuilder.append("BL p_print_string\n");

            if (!builder.getLabel().toString().contains("p_print_string:")) {
                labelBuilder.append("p_print_string:\n");
                labelBuilder.append("PUSH {lr}\n");
                Registers.Register registerZero = currentlyUsedRegister;
                currentlyUsedRegister = registers.getFirstEmptyRegister();
                labelBuilder.append("LDR " + currentlyUsedRegister + ", [" + registerZero + "]\n");
                currentlyUsedRegister.setValue(true);
                Registers.Register registerFirst = currentlyUsedRegister;
                currentlyUsedRegister = registers.getFirstEmptyRegister();
                labelBuilder.append("ADD " + currentlyUsedRegister + ", " + registerZero + ", #4\n");

                //r2 need to set value
                currentlyUsedRegister.setValue(0);
                registerZero.setValue(null);
                registerFirst.setValue(null);
                currentlyUsedRegister.setValue(null);

                currentlyUsedRegister = registers.getFirstEmptyRegister();

                labelBuilder.append("LDR " + currentlyUsedRegister + ", =msg_" + messageCount + "\n");
                labelBuilder.append("ADD " + currentlyUsedRegister + ", " + currentlyUsedRegister + ", #4\n");
                labelBuilder.append("BL printf\n");
                labelBuilder.append("MOV " + currentlyUsedRegister + ", #0\n");
                labelBuilder.append("BL fflush\n");
                labelBuilder.append("POP {pc}\n");

                headerBuilder.append("msg_" + messageCount + ":\n");
                messageCount++;
                headerBuilder.append(".word 5\n");
                headerBuilder.append(".ascii\t\"%.*s\\0\"\n");

                registerZero.setValue(null);

            }

            currentlyUsedRegister.setValue(null);

        }
    }

    public class SkipNode extends StatNode {

        public SkipNode() {
            command = "skip";
        }

        @Override
        public String getType() {
            return "Skip";
        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void setValue() {

        }

        @Override
        public void check() {
        }

        @Override
        public void generate(AssemblyBuilder builder) {

        }

    }

    /*
     * The DeclarationNode adds all varibles to the symboltable in correct scope
     * Checks semantically if the varible can be declare or not
     * Handles all edge cases
     */
    public class DeclarationNode extends StatNode {

        private TypeNode typeNode;
        private IdentNode identNode;
        private ASTNode assign_rhsNode;

        public DeclarationNode(TypeNode typeNode, IdentNode identNode, ASTNode assign_rhsNode) {

            command = "declaration";
            this.typeNode = typeNode;
            this.typeNode.setParent(this);
            this.typeNode.setIdent(identNode.getIdent());
            this.identNode = identNode;
            identNode.setParent(this);
            this.assign_rhsNode = assign_rhsNode;
            assign_rhsNode.setParent(this);


        }

        public TypeNode getTypeNode() {
            return typeNode;
        }

        @Override
        public String getType() {
            return "Declaration";
        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void setValue() {

        }

        @Override
        public void check() {
            setTypeNodeValue(typeNode, assign_rhsNode);

            currentStack.add(identNode.getIdent(), typeNode.getNumOfByte());

            putIntoSymbolTable(this, identNode.getIdent(), typeNode);

            assign_rhsNode.check();

            if (assign_rhsNode.getType().equals("Null")) {
                return;
            }
            if (typeNode.getType().contains("Pair") && assign_rhsNode.getType().contains("Pair")) {
                Pair_typeNode lhs = (Pair_typeNode) typeNode;
                Pair_typeNode rhs = getPair_typeNode(assign_rhsNode);
                if (!(rhs.getFirstElem().equals("Null") || lhs.getFirstElem().equals(rhs.getFirstElem()))) {
                    throwSemanticError("Need same type when declaring the variable");
                }
                if (!(rhs.getSecondElem().equals("Null") || lhs.getSecondElem().equals(rhs.getSecondElem()))) {
                    throwSemanticError("Need same type when declaring the variable");
                }
            } else if (!typeNode.getType().equals(assign_rhsNode.getType())) {
                if (typeNode.getType().contains("[]")) {
                    return;
                }
                throwSemanticError("Need same type when declaring the variable");
            }
        }

        @Override
        public void generate(AssemblyBuilder builder) {
            if (assign_rhsNode instanceof IdentNode) {
                typeNode.setIdent(((IdentNode) assign_rhsNode).getIdent());
            }
            if (typeNode.getType().equals("Int") && assign_rhsNode.getType().equals("Int")) {
                try {
                    if (!((assign_rhsNode instanceof CallNode || assign_rhsNode instanceof IdentNode || assign_rhsNode instanceof Pair_elemNode))) {
                        Integer.parseInt(assign_rhsNode.getValue());
                    }
                } catch (NumberFormatException e) {
                    throwSyntaxError("Integer value is too large for a 32-bit signed integer");
                }
            }
            int stackSize = currentStack.getSize();
            int num = stackSize / Stack.MAX_STACK_SIZE;
            int remainder = stackSize % Stack.MAX_STACK_SIZE;
            currentlyUsedRegister = registers.getFirstEmptyRegister();
            if (!isIfDeclarationCodeGenerated()) {
                for (int i = 0; i < num; i++) {
                    builder.getCurrent().append("SUB sp, sp, #" + Stack.MAX_STACK_SIZE + "\n");
                }
                builder.getCurrent().append("SUB sp, sp, #" + remainder + "\n");
                setIfDeclarationCodeGenerated(true);
            }
            assign_rhsNode.generate(builder);
            if (assign_rhsNode.getType().contains("Int") || assign_rhsNode.getType().contains("String") || assign_rhsNode.getType().contains("Pair")) {
                builder.getCurrent().append("STR " + currentlyUsedRegister + getStackPointer() + "\n");
            } else {
                builder.getCurrent().append("STRB " + currentlyUsedRegister + getStackPointer() + "\n");
            }
            currentlyUsedRegister.setValue(null);
            if (!(getParent() instanceof MultipleStatNode || getParent() instanceof ForNode)) {
                addBackToStack(builder);
            }
        }

        private void setTypeNodeValue(TypeNode typeNode, ASTNode assign_rhsNode) {
            if (typeNode.getType().toString().contains("[]") && assign_rhsNode.getType().toString().contains("[]")) {
                ((Array_typeNode) typeNode).setElems(((Array_literNode) assign_rhsNode).getArrayValue());
            } else {
                typeNode.setValue((assign_rhsNode).getValue());
            }

        }


        private void putIntoSymbolTable(ASTNode currentScope, String string, TypeNode node) {
            while (!currentScope.getScope()) {
                currentScope = currentScope.getParent();
            }
            if (currentScope.getSymbolTable().containsKey(string)) {
                throwSemanticError(currentScope.getClass().toString());
            } else {
                currentScope.getSymbolTable().put(string, node);
            }
        }

    }

    /*
     * Similar to DeclarationNode
     * Has handle left-hand-side node correctly
     */
    public class AssignmentNode extends StatNode {

        private ASTNode assign_lhsNode;
        private ASTNode assign_rhsNode;

        public AssignmentNode(ASTNode assign_lhsNode, ASTNode assign_rhsNode) {

            command = "assignment";
            this.assign_lhsNode = assign_lhsNode;
            assign_lhsNode.setParent(this);
            this.assign_rhsNode = assign_rhsNode;
            assign_rhsNode.setParent(this);
        }

        @Override
        public String getType() {
            return "Assignment";
        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void check() {

            assign_lhsNode.check();
            assign_rhsNode.check();

            if (assign_rhsNode.getType().equals("Null")) {
                return;
            }
            if (assign_lhsNode.getType().contains("Pair") && assign_rhsNode.getType().contains("Pair")) {
                Pair_typeNode lhs = getPair_typeNode(assign_lhsNode);
                Pair_typeNode rhs = getPair_typeNode(assign_rhsNode);
                if (!(rhs.getFirstElem().equals("Null") || lhs.getFirstElem().equals(rhs.getFirstElem()))) {
                    throwSemanticError("Need same type when assigning the variable");
                }
                if (!(rhs.getSecondElem().equals("Null") || lhs.getSecondElem().equals(rhs.getSecondElem()))) {
                    throwSemanticError("Need same type when assigning the variable");
                }
            } else if (!assign_lhsNode.getType().contains(assign_rhsNode.getType())) {
                if (assign_lhsNode.getType().equals("String") && assign_rhsNode.getType().equals("Char")) {
                    return;
                } else {
                    throwSemanticError("Need same type when assigning the variable");
                }
            }

        }

        @Override
        public void generate(AssemblyBuilder builder) {

            currentlyUsedRegister = registers.getFirstEmptyRegister();
            assign_rhsNode.generate(builder);
            currentlyUsedRegister.setValue(null);

            if (assign_rhsNode.getType().equals("Int") || assign_rhsNode.getType().equals("String")) {
                if (assign_lhsNode.getType().contains("[]")) {
                    assign_lhsNode.generate(builder);
                    currentlyUsedRegister.setValue(true);
                    Registers.Register r1 = registers.getFirstEmptyRegister();
                    if (assign_lhsNode.getType().equals("Char[]")) {
                        builder.getCurrent().append("STRB " + currentlyUsedRegister + ", [" + r1 + "]\n");
                    } else {
                        builder.getCurrent().append("STR " + currentlyUsedRegister + ", [" + r1 + "]\n");
                    }
                    r1.setValue(true); //not sure
                    currentlyUsedRegister.setValue(null);
                } else {
                    builder.getCurrent().append("STR " + currentlyUsedRegister + getStackPointer() + "\n");
                }
            } else {
                if (assign_rhsNode.getType().equals("Char") && assign_lhsNode.getType().equals("String")) {
                    assign_lhsNode.generate(builder);
                    currentlyUsedRegister.setValue(true);
                    Registers.Register r1 = currentlyUsedRegister.getNext();
                    builder.getCurrent().append("STRB " + currentlyUsedRegister + ", [" + r1 + "]\n");
                    currentlyUsedRegister.setValue(null);
                } else {
                    builder.getCurrent().append("STRB " + currentlyUsedRegister + getStackPointer() + "\n");
                }

            }

        }


        private TypeNode lookupSymbolTable(ASTNode currentScope, String string) {
            while (!currentScope.getScope()) {
                currentScope = currentScope.getParent();
            }
            return (TypeNode) currentScope.getSymbolTable().get(string);
        }

        public TypeNode getTypeNode(IdentNode identNode) {
            ASTNode parent = getParent();
            boolean found = false;
            while (parent != null) {
                if (parent.getSymbolTable().containsKey(identNode.getIdent())) {
                    return ((TypeNode) parent.getSymbolTable().get(identNode.getIdent()));
                }
                parent = parent.getParent();
            }
            if (found == false) {
                return null;
            }
            return null;
        }

        public void setValue() {
            if (assign_lhsNode instanceof IdentNode) {
                if (assign_rhsNode instanceof IdentNode) {
                    ((IdentNode) assign_lhsNode).getTypeNode().setIdent(((IdentNode) assign_rhsNode).getIdent());
                } else if (assign_rhsNode instanceof Str_literNode) {
                    getTypeNode((IdentNode) assign_lhsNode).setValue((assign_rhsNode).getValue());
                } else if (assign_rhsNode.getType().equals("Int")) {
                    getTypeNode((IdentNode) assign_lhsNode).setValue((assign_rhsNode.getValue()));
                }
            }
        }
    }

    /*
     * All other StatNodes are similar
     * Implemented their own check cases
     */
    public class ReadNode extends StatNode {

        private ASTNode assign_lhsNode;

        public ReadNode(ASTNode assign_lhsNode) {

            command = "read";
            this.assign_lhsNode = assign_lhsNode;
            assign_lhsNode.setParent(this);

        }


        @Override
        public String getType() {
            return "Read";
        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void setValue() {

        }

        @Override
        public void check() {
            assign_lhsNode.check();
            String type = assign_lhsNode.getType();
            switch (type) {
                case "Int":
                case "Char":
                    break;
                default:
                    throwSemanticError("The read statment can only read int or char type experssion");
            }
            if (!(assign_lhsNode instanceof IdentNode || assign_lhsNode instanceof Array_elemNode
                    || assign_lhsNode instanceof FSTNode || assign_lhsNode instanceof SNDNode)) {
                throwSemanticError("The read statment can only read a program varible, " +
                        "array element or a pair element");
            }
        }

        @Override
        public void generate(AssemblyBuilder builder) {
            if (assign_lhsNode instanceof IdentNode) {
                switch (assign_lhsNode.getType()) {
                    case ("String"):
                        generateReadStringLiter(builder);
                        break;
                    case ("Bool"):
                        generateReadBoolLiter(builder);
                        break;
                    case ("Int"):
                        generateReadIntLiter(builder);
                        break;
                    case ("Char"):
                        generateReadCharLiter(builder);
                        break;
                }
            }
        }

        private void generateReadCharLiter(AssemblyBuilder builder) {
            builder.getCurrent().append("ADD r0, sp, #" + ((IdentNode) assign_lhsNode).getStackOffset() + "\n");
            builder.getCurrent().append("BL p_read_char\n");
            if (!builder.getLabel().toString().contains("p_read_char:")) {
                builder.getLabel().append("p_read_char:\n");
                builder.getLabel().append("PUSH {lr}\n");
                builder.getLabel().append("MOV r1, r0\n");
                builder.getLabel().append("LDR r0, =msg_" + messageCount + "\n");
                builder.getLabel().append("ADD r0, r0, #4\n");
                builder.getLabel().append("BL scanf\n");
                builder.getLabel().append("POP {pc}\n");

                builder.getHeader().append("msg_" + messageCount + ":\n");
                builder.getHeader().append(".word 4\n");
                builder.getHeader().append(".ascii\t\" %c\\0\"\n");
                messageCount++;
            }

        }

        private void generateReadIntLiter(AssemblyBuilder builder) {

            builder.getCurrent().append("ADD r0, sp, #" + ((IdentNode) assign_lhsNode).getStackOffset() + "\n");
            builder.getCurrent().append("BL p_read_int\n");
            ((IdentNode) assign_lhsNode).getTypeNode().setValue("0");
            if (!builder.getLabel().toString().contains("p_read_int:")) {
                builder.getLabel().append("p_read_int:\n");
                builder.getLabel().append("PUSH {lr}\n");
                builder.getLabel().append("MOV r1, r0\n");
                builder.getLabel().append("LDR r0, =msg_" + messageCount + "\n");
                builder.getLabel().append("ADD r0, r0, #4\n");
                builder.getLabel().append("BL scanf\n");
                builder.getLabel().append("POP {pc}\n");

                builder.getHeader().append("msg_" + messageCount + ":\n");
                builder.getHeader().append(".word 3\n");
                builder.getHeader().append(".ascii\t\"%d\\0\"\n");
                messageCount++;
            }

        }

        private void generateReadBoolLiter(AssemblyBuilder builder) {

        }

        private void generateReadStringLiter(AssemblyBuilder builder) {
        }
    }

    public class FreeNode extends StatNode {

        private ExprNode exprNode;

        public FreeNode(ExprNode exprNode) {

            command = "free";
            this.exprNode = exprNode;
            exprNode.setParent(this);

        }

        @Override
        public String getType() {

            return "Free";
        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void setValue() {

        }

        @Override
        public void check() {

            exprNode.check();
            String type = exprNode.getType();

            if (!type.contains("Pair") || type.contains("[]")) {
                throwSemanticError("The free staement takes invalid arguments");
            }

        }

        @Override
        public void generate(AssemblyBuilder builder) {

        }


    }

    public class ReturnNode extends StatNode {

        private ExprNode exprNode;

        public ReturnNode(ExprNode exprNode) {

            command = "return";
            this.exprNode = exprNode;
            exprNode.setParent(this);

        }

        @Override
        public String getType() {

            return exprNode.getType();
        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void setValue() {

        }

        @Override
        public void check() {

            exprNode.check();
            ASTNode parent = getParent();
            while (!(parent instanceof FuncNode)) {
                if (parent.equals(getRoot())) {
                    throwSemanticError("Can not return from program");
                }
                parent = parent.getParent();

            }
            if (this.getType() != parent.getType()) {
                throwSemanticError("Cannot return in program statement");
            }
        }

        @Override
        public void generate(AssemblyBuilder builder) {

            currentlyUsedRegister = registers.getFirstEmptyRegister();
            exprNode.generate(builder);
            currentlyUsedRegister.setValue(null);

        }

    }

    public class ExitNode extends StatNode {

        private ExprNode exprNode;

        public ExitNode(ExprNode exprNode) {

            command = "exit";
            this.exprNode = exprNode;
            exprNode.setParent(this);

        }

        @Override
        public String getType() {
            return exprNode.getType();
        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void setValue() {

        }

        @Override
        public void check() {

            exprNode.check();

            if (!exprNode.getType().equals("Int")) {
                throwSemanticError("The exit statement must take int argument");
            }
        }

        @Override
        public void generate(AssemblyBuilder builder) {
            currentlyUsedRegister = registers.getFirstEmptyRegister();
            exprNode.generate(builder);
            currentlyUsedRegister.setValue(null);
            builder.getCurrent().append("BL exit\n");
        }
    }

    public class PrintNode extends StatNode {

        private ExprNode exprNode;

        public PrintNode(ExprNode exprNode) {

            command = "print";
            this.exprNode = exprNode;
            exprNode.setParent(this);

        }

        @Override
        public String getType() {
            return exprNode.getType();
        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void setValue() {

        }

        @Override
        public void check() {

            exprNode.check();
        }

        @Override
        public void generate(AssemblyBuilder builder) {
            String type = exprNode.getType();
            switch (type) {
                case "String":
                    generatePrintStringLiter(builder, exprNode);
                    return;
                case "Bool":
                    generatePrintBoolLiter(builder, exprNode);
                    return;
                case "Int":
                    generatePrintIntLiter(builder, exprNode);
                    return;
                case "Char":
                    generatePrintCharLiter(builder, exprNode);
                    return;
            }
            if (exprNode instanceof IdentNode) {
                generatePrintArrayAddress(builder, exprNode);
            } else if (type.contains("[]")) {
                generatePrintArrayElem(builder, exprNode);
            }
        }

    }

    public class PrintlnNode extends StatNode {

        private ExprNode exprNode;

        public PrintlnNode(ExprNode exprNode) {

            command = "println";
            this.exprNode = exprNode;
            exprNode.setParent(this);

        }

        @Override
        public String getType() {
            return exprNode.getType();
        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void setValue() {

        }

        @Override
        public void check() {

            exprNode.check();
        }

        @Override
        public void generate(AssemblyBuilder builder) {

            String type = exprNode.getType();
            switch (type) {
                case "String":
                    generatePrintStringLiter(builder, exprNode);
                    generatePrintln(builder);
                    return;
                case "Bool":
                    generatePrintBoolLiter(builder, exprNode);
                    generatePrintln(builder);
                    return;
                case "Int":
                    generatePrintIntLiter(builder, exprNode);
                    generatePrintln(builder);
                    return;
                case "Char":
                    generatePrintCharLiter(builder, exprNode);
                    generatePrintln(builder);
                    return;
            }
            if (exprNode instanceof IdentNode) {
                generatePrintArrayAddress(builder, exprNode);
            } else if (type.contains("[]")) {
                generatePrintArrayElem(builder, exprNode);
            }
            generatePrintln(builder);

        }

        private void generatePrintln(AssemblyBuilder builder) {

            StringBuilder currentBuilder = builder.getCurrent();
            StringBuilder headerBuilder = builder.getHeader();
            StringBuilder labelBuilder = builder.getLabel();

            currentBuilder.append("BL p_print_ln\n");

            if (!builder.getLabel().toString().contains("p_print_ln:")) {
                labelBuilder.append("p_print_ln:\n");
                labelBuilder.append("PUSH {lr}\n");

                currentlyUsedRegister = registers.getFirstEmptyRegister();

                labelBuilder.append("LDR " + currentlyUsedRegister + ", =msg_" + messageCount + "\n");
                labelBuilder.append("ADD " + currentlyUsedRegister + ", " + currentlyUsedRegister + ", #4\n");
                labelBuilder.append("BL puts\n");
                labelBuilder.append("MOV " + currentlyUsedRegister + ", #0\n");
                labelBuilder.append("BL fflush\n");
                labelBuilder.append("POP {pc}\n");

                headerBuilder.append("msg_" + messageCount + ":\n");
                messageCount++;
                headerBuilder.append(".word 1\n");
                headerBuilder.append(".ascii\t\"\\0\"\n");

                currentlyUsedRegister.setValue(null);
            }
        }

        public String getValue(IdentNode identNode) {
            ASTNode parent = getParent();
            boolean found = false;
            while (parent != null) {
                if (parent.getSymbolTable().containsKey(identNode.getIdent())) {
                    return ((TypeNode) parent.getSymbolTable().get(identNode.getIdent())).getValue();
                }
                parent = parent.getParent();
            }
            if (found == false) {
                return null;
            }
            return null;
        }

    }


    /*
     * IfNode has its own scope
     * Correctly checked the condition and ensure the statements are vaild
     */
    public class IfNode extends StatNode {

        private ExprNode exprNode;
        private StatNode statNodeTrue;
        private StatNode statNodeFalse;

        public IfNode(ExprNode exprNode, StatNode statNodeTrue, StatNode statNodeFalse) {

            command = "if";
            this.exprNode = exprNode;
            exprNode.setParent(this);
            this.statNodeTrue = statNodeTrue;
            statNodeTrue.setParent(this);
            if (statNodeFalse != null) {
                this.statNodeFalse = statNodeFalse;
                statNodeFalse.setParent(this);
            }

        }

        @Override
        public String getType() {
            return "If";
        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void setValue() {

        }

        @Override
        public void check() {

            statNodeTrue.setScope(true);
            statNodeFalse.setScope(true);
            exprNode.check();

            if (!exprNode.getType().equals("Bool")) {
                throwSemanticError("If statement can only take boolean argument");
            }
            statNodeTrue.check();
            if (statNodeFalse != null) {
                statNodeFalse.check();
            }
        }

        @Override
        public void generate(AssemblyBuilder builder) {

            StringBuilder currentStringBuilder = builder.getCurrent();
            String labelFalse = "L" + labelCount;
            StringBuilder stringBuilderFalse = new StringBuilder();
            stringBuilderFalse.append(labelFalse + ":\n");
            labelCount++;

            String labelTrue = "L" + labelCount;
            labelCount++;

            currentlyUsedRegister = registers.getFirstEmptyRegister();
            exprNode.generate(builder);

            builder.getCurrent().append("CMP " + currentlyUsedRegister + ", #0\n");
            builder.getCurrent().append("BEQ " + labelFalse + "\n");
            currentlyUsedRegister.setValue(null);

            statNodeTrue.generate(builder);
            builder.getCurrent().append("B " + labelTrue + "\n");
            builder.setCurrent(stringBuilderFalse);
            statNodeFalse.generate(builder);

            builder.setCurrent(currentStringBuilder);
            builder.getCurrent().append(stringBuilderFalse);
            builder.getCurrent().append(labelTrue + ":\n");

            if (exprNode.getValue().equals("true")) {
                if (statNodeTrue instanceof AssignmentNode) {
                    ((AssignmentNode) statNodeTrue).setValue();
                }
            } else {
                if (statNodeFalse instanceof AssignmentNode) {
                    ((AssignmentNode) statNodeFalse).setValue();
                }
            }
        }

    }

    public class ForNode extends StatNode{
        private DeclarationNode declarationNode;
        private ASTNode exprNode;
        private AssignmentNode assignmentNode;
        private ASTNode statNode;
        private int labelNumber;

        public ForNode(DeclarationNode declarationNode, ASTNode exprNode, AssignmentNode assignmentNode, ASTNode statNode) {

            this.declarationNode = declarationNode;
            this.exprNode = exprNode;
            this.assignmentNode = assignmentNode;
            this.statNode = statNode;
            declarationNode.setParent(this);
            exprNode.setParent(this);
            assignmentNode.setParent(this);
            statNode.setParent(this);
        }

        @Override
        public String getType() {
            return null;
        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void check() {
            setScope(true);
            declarationNode.check();

            if (!declarationNode.getTypeNode().getType().equals("Int")) {
                throwSemanticError("The initialization part of for loop needs to declared an int variable!");
            }
            if (!exprNode.getType().equals("Bool")) {
                throwSemanticError("The condition part of for loop has to have type bool!");
            }
            if (!assignmentNode.assign_lhsNode.getType().equals("Int")) {
                throwSemanticError("Must assign an int type variable of for loop!");
            }
        }

        @Override
        public void generate(AssemblyBuilder builder) {
            declarationNode.generate(builder);

            StringBuilder currentStringBuilder = builder.getCurrent();

            String labelWhileBody = "L" + labelCount;
            labelCount++;

            String labelWhileEnd = "L" + labelCount;
            labelNumber = labelCount;
            labelCount++;

            builder.getCurrent().append("B " + labelWhileEnd + "\n");

            currentStringBuilder.append(labelWhileBody + ":\n");
            statNode.generate(builder);
            ((StatNode) statNode).setValue();
            assignmentNode.generate(builder);

            currentStringBuilder.append(labelWhileEnd + ":\n");


            currentlyUsedRegister = registers.getFirstEmptyRegister();
            exprNode.generate(builder);

            currentStringBuilder.append("CMP " + currentlyUsedRegister + ", #1\n");
            currentlyUsedRegister.setValue(null);

            currentStringBuilder.append("BEQ " + labelWhileBody + "\n");

            currentStringBuilder.append("END" + loopCount + ":\n");
            loopCount++;

            if (!(getParent() instanceof MultipleStatNode)) {
                addBackToStack(builder);
            }
        }

        @Override
        public void setValue() {

        }

        public int getLabelNumber() {
            return labelNumber;
        }
    }

    /*
     * WhileNode has its own scope
     * Correctly checked the condition and ensure the statements are vaild
     */
    public class WhileNode extends StatNode {

        private ExprNode exprNode;
        private StatNode statNode;
        private int labelNum;

        public WhileNode(ExprNode exprNode, StatNode statNode) {

            command = "while";
            this.exprNode = exprNode;
            exprNode.setParent(this);
            this.statNode = statNode;
            statNode.setParent(this);

        }

        @Override
        public String getType() {
            return "While";
        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void setValue() {

        }

        @Override
        public void check() {

            setScope(true);
            exprNode.check();

            if (!exprNode.getType().equals("Bool")) {
                throwSemanticError("While statement condition can only take boolean argument");
            }
            statNode.check();
        }

        @Override
        public void generate(AssemblyBuilder builder) {
            StringBuilder currentStringBuilder = builder.getCurrent();

            String labelWhileBody = "L" + labelCount;
            labelCount++;

            String labelWhileEnd = "L" + labelCount;
            labelNum = labelCount;
            labelCount++;

            builder.getCurrent().append("B " + labelWhileEnd + "\n");

            currentStringBuilder.append(labelWhileBody + ":\n");
            statNode.generate(builder);
            statNode.setValue();

            currentStringBuilder.append(labelWhileEnd + ":\n");

            currentlyUsedRegister = registers.getFirstEmptyRegister();
            exprNode.generate(builder);

            currentStringBuilder.append("CMP " + currentlyUsedRegister + ", #1\n");
            currentlyUsedRegister.setValue(null);

            currentStringBuilder.append("BEQ " + labelWhileBody + "\n");

            currentStringBuilder.append("END" + loopCount + ":\n");
            loopCount++;
        }

        public int getLabelNumber() {
            return labelNum;
        }
    }

    public class BeginNode extends StatNode {

        private StatNode statNode;

        public BeginNode(StatNode statNode) {

            command = "begin";
            this.statNode = statNode;
            statNode.setParent(this);

        }

        @Override
        public String getType() {
            return "Begin";
        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void setValue() {

        }

        @Override
        public void check() {

            setScope(true);
            statNode.check();

        }

        @Override
        public void generate(AssemblyBuilder builder) {
            statNode.generate(builder);
        }

    }

    /*
     * To introduce more statemtns
     */
    public class MultipleStatNode extends StatNode {

        private StatNode statNodeFirst;
        private StatNode statNodeSecond;

        public MultipleStatNode(StatNode statNodeFirst, StatNode statNodeSecond) {

            command = "multiple";
            this.statNodeFirst = statNodeFirst;
            statNodeFirst.setParent(this);
            this.statNodeSecond = statNodeSecond;
            statNodeSecond.setParent(this);

        }

        @Override
        public String getType() {
            return "MultipleStat";
        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void setValue() {

        }

        @Override
        public void check() {

            statNodeFirst.check();
            statNodeSecond.check();
        }

        @Override
        public void generate(AssemblyBuilder builder) {

            statNodeFirst.generate(builder);
            statNodeFirst.setValue();

            statNodeSecond.generate(builder);
            statNodeSecond.setValue();

        }

    }

    public abstract class Pair_elemNode extends ASTNode {

        protected IdentNode exprNode;

        public Pair_elemNode(IdentNode exprNode) {
            this.exprNode = exprNode;
            exprNode.setParent(this);
        }

        public IdentNode getIdentNode() {
            return exprNode;
        }

        public TypeNode getTypeNode() {
            return exprNode.getTypeNode();
        }
    }

    /*
     * FSTNode has the type Pair
     * Need to return to correct type
     */
    public class FSTNode extends Pair_elemNode {

        private IdentNode exprNode;

        public FSTNode(IdentNode exprNode) {
            super(exprNode);
        }

        public String getType() {

            return ((Pair_typeNode) getTypeNode()).getFirstElem();
        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void check() {

            exprNode.check();

            if (!exprNode.getType().contains("Pair")) {
                throwSemanticError("The FST statement can only take argument of type pair");
            }

        }

        @Override
        public void generate(AssemblyBuilder builder) {
            builder.getCurrent().append("LDR r0, [sp, #4]\n");
            builder.getCurrent().append("BL p_check_null_pointer");
            builder.getCurrent().append("LDR r0, [r0]\n");
            if (getType().equals("Int") || getType().equals("String") || getType().contains("[]") || getType().contains("Pair")) {
                builder.getCurrent().append("LDR r0, [r0]\n");

                if (!builder.getLabel().toString().contains("p_check_null_pointer:")) {
                    builder.getLabel().append("p_check_null_pointer:\n");
                    builder.getLabel().append("PUSH {lr}\n");
                    builder.getLabel().append("CMP r0, #0\n");
                    builder.getLabel().append("LDREQ r0, =msg_0\n");
                    builder.getLabel().append("BLEQ p_throw_runtime_error\n");
                    builder.getLabel().append("POP {pc}\n");
                }
            }
        }

    }

    /*
     * SNDNode has the type Pair
     * Need to return to correct type
     */
    public class SNDNode extends Pair_elemNode {

        private IdentNode exprNode;

        public SNDNode(IdentNode exprNode) {
            super(exprNode);
        }

        public String getType() {

            return ((Pair_typeNode) getTypeNode()).getSecondElem();
        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void check() {

            exprNode.check();

            if (!exprNode.getType().contains("Pair")) {
                throwSemanticError("The SND statement can only take argument of type pair");
            }

        }

        @Override
        public void generate(AssemblyBuilder builder) {

        }

    }

    /*
     * Including base-type, array-type, pair-type
     * The getType() method will return the type as String
     */
    public abstract class TypeNode extends ASTNode {

        protected String type;
        private String value;
        private String ident;

        public TypeNode() {
            type = "";
        }

        public String getIdent() {
            return ident;
        }

        public void setIdent(String ident) {
            this.ident = ident;
        }

        @Override
        public boolean equals(Object that) {
            return getType().equals(((TypeNode) that).getType());
        }

        @Override
        public void check() {
        }

        public abstract int getNumOfByte();

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public abstract void setValue();
    }


    public abstract class Base_typeNode extends TypeNode {

        public Base_typeNode() {
            super();
        }

    }

    public class IntTypeNode extends Base_typeNode {

        public IntTypeNode() {
            type = "Int";
        }

        @Override
        public int getNumOfByte() {
            return 4;
        }

        @Override
        public void setValue() {
            setValue("0");
        }

        @Override
        public String getType() {
            return "Int";
        }

        @Override
        public void generate(AssemblyBuilder builder) {

        }

    }

    public class BoolTypeNode extends Base_typeNode {

        public BoolTypeNode() {
            type = "bool";
        }

        @Override
        public int getNumOfByte() {
            return 1;
        }

        @Override
        public void setValue() {
            setValue("false");

        }

        @Override
        public String getType() {
            return "Bool";
        }

        @Override
        public void generate(AssemblyBuilder builder) {

        }

    }

    public class CharTypeNode extends Base_typeNode {

        public CharTypeNode() {
            type = "char";
        }

        @Override
        public int getNumOfByte() {
            return 1;
        }

        @Override
        public void setValue() {
            setValue(" ");
        }

        @Override
        public String getType() {
            return "Char";
        }

        @Override
        public void generate(AssemblyBuilder builder) {

        }

    }

    public class StringTypeNode extends Base_typeNode {

        public StringTypeNode() {

            type = "string";
        }

        @Override
        public int getNumOfByte() {
            return 4;
        }

        @Override
        public void setValue() {
            setValue(" ");
        }

        @Override
        public String getType() {
            return "String";
        }

        @Override
        public void generate(AssemblyBuilder builder) {

        }

    }


    public class Array_typeNode extends TypeNode {

        private TypeNode typeNode;
        private List<ASTNode> elems;

        public Array_typeNode(TypeNode typeNode) {

            this.typeNode = typeNode;
            typeNode.setParent(this);

        }

        @Override
        public int getNumOfByte() {
            return 4;
        }

        @Override
        public void setValue() {

        }

        @Override
        public String getType() {
            return typeNode.getType() + "[]";
        }

        @Override
        public void generate(AssemblyBuilder builder) {

        }

        public List<ASTNode> getArrayValue() {
            return elems;
        }

        public void setElems(List<ASTNode> elems) {
            this.elems = elems;
        }

    }

    /*
     * Representing the nodes of the parameters of the function
     * Save all parameters in the relating function scope
     */
    public class ParamNode extends ASTNode {

        private TypeNode typeNode;
        private IdentNode identNode;

        public ParamNode(TypeNode typeNode, IdentNode identNode) {
            this.typeNode = typeNode;
            typeNode.setParent(this);
            this.identNode = identNode;
            identNode.setParent(this);
        }

        public IdentNode getIdentNode() {
            return identNode;
        }

        public TypeNode getTypeNode() {
            return typeNode;
        }

        @Override
        public String getType() {
            return typeNode.getType();
        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void check() {

        }

        @Override
        public void generate(AssemblyBuilder builder) {

        }

    }

    public class Pair_typeNode extends TypeNode {

        private ASTNode pair_elemNode1;
        private ASTNode pair_elemNode2;

        public Pair_typeNode(ASTNode pair_elemNode1, ASTNode pair_elemNode2) {
            this.pair_elemNode1 = pair_elemNode1;
            pair_elemNode1.setParent(this);
            this.pair_elemNode2 = pair_elemNode2;
            pair_elemNode2.setParent(this);
        }


        public String getFirstElem() {
            return pair_elemNode1.getType();
        }

        public String getSecondElem() {
            return pair_elemNode2.getType();
        }

        @Override
        public int getNumOfByte() {
            return 4;
        }

        @Override
        public void setValue() {

        }

        @Override
        public String getType() {
            return "Pair";
        }

        @Override
        public void generate(AssemblyBuilder builder) {

        }

    }

    public class PairNode extends TypeNode {

        @Override
        public int getNumOfByte() {
            return 4;
        }

        @Override
        public void setValue() {

        }

        @Override
        public String getType() {
            return "Pair";
        }

        @Override
        public void check() {

        }

        @Override
        public void generate(AssemblyBuilder builder) {

        }
    }

    /*
     * Each different unary-operation needs to implement their own check() method in order to get meaningful
     * error message and condition.
     */
    public abstract class Unary_operNode extends ExprNode {

        protected String unOp;
        protected ExprNode exprNode;

        public Unary_operNode(ExprNode exprNode) {

            this.exprNode = exprNode;
            exprNode.setParent(this);
            unOp = "";

        }

        public String getUnOp() {
            return unOp;
        }

        public ExprNode getExprNdoe() {
            return exprNode;
        }

    }

    public class NotOperNode extends Unary_operNode {

        public NotOperNode(ExprNode exprNode) {

            super(exprNode);
            unOp = "!";

        }

        @Override
        public String getType() {

            return "Bool";
        }

        @Override
        public void check() {

            exprNode.check();
            if (!exprNode.getType().equals("Bool")) {
                throwSemanticError("Not operator only take boolean argument");
            }

        }

        @Override
        public String getValue() {
            return String.valueOf(!(Boolean.parseBoolean(exprNode.getValue())));
        }

        @Override
        public void setValue() {

        }

        @Override
        public void generate(AssemblyBuilder builder) {

            exprNode.generate(builder);
            builder.getCurrent().append("EOR " + currentlyUsedRegister + ", " + currentlyUsedRegister + ", #1\n");

        }

    }

    public class NegateOperNode extends Unary_operNode {

        public NegateOperNode(ExprNode exprNode) {

            super(exprNode);
            unOp = "-";
        }

        @Override
        public String getType() {

            return "Int";
        }

        @Override
        public void check() {

            exprNode.check();
            if (!exprNode.getType().equals("Int")) {
                throwSemanticError("Negate operator only take int argument");
            }

        }

        @Override
        public String getValue() {
            long x = Long.parseLong(exprNode.getValue());
            x = x / (-1);
            return String.valueOf(x);
        }

        @Override
        public void setValue() {

        }

        @Override
        public void generate(AssemblyBuilder builder) {
            if (exprNode instanceof Int_literNode) {
                builder.getCurrent().append("LDR " + currentlyUsedRegister + ", =" + getValue() + "\n");
                currentlyUsedRegister.setValue(getValue());
            } else {
                builder.getCurrent().append("LDR " + currentlyUsedRegister + getStackPointer() + "\n");
                currentlyUsedRegister.setValue(true);
                builder.getCurrent().append("RSBS " + currentlyUsedRegister + ", " + currentlyUsedRegister+ ", #0\n");
                builder.getCurrent().append("BLVS p_throw_overflow_error\n");
                if (!builder.getLabel().toString().contains("p_throw_overflow_error:")) {
                    builder.getLabel().append("p_throw_overflow_error:\n");
                    builder.getLabel().append("LDR " + currentlyUsedRegister + " , =msg_" + messageCount + "\n");
                    currentlyUsedRegister.setValue(true);
                    builder.getLabel().append("BL p_throw_runtime_error\n");

                    builder.getHeader().append("msg_" + messageCount + ":\n");
                    builder.getHeader().append(".word 82\n");
                    builder.getHeader().append(".ascii\t\"OverflowError: the result is too small/large"
                            + "to store in a 4-byte signed-integer.\\n\"\n");
                    messageCount++;

                    if (!builder.getLabel().toString().contains("p_throw_runtime_error:")) {
                        builder.getLabel().append("p_throw_runtime_error:\n");
                        builder.getLabel().append("BL p_print_string\n");
                        builder.getLabel().append("MOV r0, #-1\n");
                        builder.getLabel().append("BL exit\n");

                        if (!builder.getLabel().toString().contains("p_print_string:")) {
                            builder.getLabel().append("p_print_string:\n");
                            builder.getLabel().append("PUSH {lr}\n");
                            Registers.Register registerZero = currentlyUsedRegister;
                            currentlyUsedRegister = registers.getFirstEmptyRegister();
                            builder.getLabel().append("LDR " + currentlyUsedRegister + ", [" + registerZero + "]\n");
                            currentlyUsedRegister.setValue(registerZero.getValue());
                            Registers.Register registerFirst = currentlyUsedRegister;
                            currentlyUsedRegister = registers.getFirstEmptyRegister();
                            builder.getLabel().append("ADD " + currentlyUsedRegister + ", " + registerZero + ", #4\n");

                            //r2 need to set value
                            currentlyUsedRegister.setValue(0);
                            registerZero.setValue(null);
                            registerFirst.setValue(null);
                            currentlyUsedRegister.setValue(null);

                            currentlyUsedRegister = registers.getFirstEmptyRegister();

                            builder.getLabel().append("LDR " + currentlyUsedRegister + ", =msg_" + messageCount + "\n");
                            builder.getLabel().append("ADD " + currentlyUsedRegister + ", " + currentlyUsedRegister + ", #4\n");
                            builder.getLabel().append("BL printf\n");
                            builder.getLabel().append("MOV " + currentlyUsedRegister + ", #0\n");
                            builder.getLabel().append("BL fflush\n");
                            builder.getLabel().append("POP {pc}\n");

                            builder.getHeader().append("msg_" + messageCount + ":\n");
                            messageCount++;
                            builder.getHeader().append(".word 5\n");
                            builder.getHeader().append(".ascii\t\"%.*s\\0\"\n");

                            registerZero.setValue(null);
                        }
                    }
                }
            }
        }

    }

    public class LenOperNode extends Unary_operNode {

        public LenOperNode(ExprNode exprNode) {

            super(exprNode);
            unOp = "len";

        }

        @Override
        public String getType() {

            return "Int";
        }

        @Override
        public void check() {

            exprNode.check();

            if (!(exprNode.getType().contains("[]") || exprNode.getType().equals("String"))) {
                throwSemanticError("Len operator only take int argument");
            }

        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void setValue() {

        }

        @Override
        public void generate(AssemblyBuilder builder) {

            exprNode.generate(builder);
            builder.getCurrent().append("LDR " + currentlyUsedRegister + ", [" + currentlyUsedRegister + "]\n");

        }
    }

    public class OrdOperNode extends Unary_operNode {

        public OrdOperNode(ExprNode exprNode) {

            super(exprNode);
            unOp = "ord";

        }

        @Override
        public String getType() {

            return "Int";
        }

        @Override
        public void check() {

            exprNode.check();

            if (!exprNode.getType().equals("Char")) {
                throwSemanticError("Ord operator only take int argument");
            }

        }

        @Override
        public String getValue() {
            return String.valueOf((int) exprNode.getValue().charAt(1));
        }

        @Override
        public void setValue() {

        }

        @Override
        public void generate(AssemblyBuilder builder) {
            exprNode.generate(builder);
        }
    }

    public class CharOperNode extends Unary_operNode {

        public CharOperNode(ExprNode exprNode) {

            super(exprNode);
            unOp = "chr";

        }

        @Override
        public String getType() {

            return "Char";
        }

        @Override
        public void check() {

            exprNode.check();

            if (!exprNode.getType().equals("Int")) {
                throwSemanticError("Char operator only take int argument");
            }

        }

        @Override
        public String getValue() {
            int value = Integer.valueOf(exprNode.getValue());
            return "\'" + (char) value + "\'";
        }

        @Override
        public void setValue() {

        }

        @Override
        public void generate(AssemblyBuilder builder) {
            exprNode.generate(builder);
        }

    }

    /*
     * Different binary-operation has different semantic requirement
     * All sub-classes overwrite the check() method
     */
    public abstract class Binary_operNode extends ExprNode {

        protected ASTNode exp1;
        protected ASTNode exp2;
        protected String binOp;

        public Binary_operNode(ASTNode exp1, ASTNode exp2) {

            this.exp1 = exp1;
            exp1.setParent(this);
            this.exp2 = exp2;
            exp2.setParent(this);
            binOp = "";

        }

        @Override
        public String getType() {
            return (exp1).getType();
        }

        public String getBinOp() {
            return binOp;
        }

        @Override
        public void check() {

            exp1.check();
            exp2.check();

            if (!exp1.getType().equals(exp2.getType()) && !exp2.getType().equals("Null")) {
                throwSemanticError("Binary operation need both sides have the same type");
            } else if (!exp1.getType().contains("Pair") && exp2.getType().contains("Pair")) {
                throwSemanticError("Binary operation for pair need to have type pair on both sides");
            }
        }

        protected void generateBranchCode(AssemblyBuilder builder) {
            StringBuilder currentBuilder = builder.getCurrent();

            currentlyUsedRegister = registers.getFirstEmptyRegister();

            exp1.generate(builder);
            currentBuilder.append("PUSH {" + currentlyUsedRegister + "}\n");
            currentlyUsedRegister.setValue(null);
            currentStack.incSize(4);

            currentlyUsedRegister = registers.getFirstEmptyRegister();
            exp2.generate(builder);
            if (exp2 instanceof Binary_operNode) {
                currentlyUsedRegister.setValue(true);
            }

            Registers.Register registerFirst = registers.getFirstEmptyRegister();

            currentBuilder.append("MOV " + registerFirst + ", " + currentlyUsedRegister + "\n");
            registerFirst.setValue(true);
            currentBuilder.append("POP {" + currentlyUsedRegister + "}\n");
            // need to set value
            currentStack.decSize(4);
            currentBuilder.append("CMP " + currentlyUsedRegister + ", " + registerFirst + "\n");

//            currentlyUsedRegister.setValue(null);
            registerFirst.setValue(null);

        }

        private void generateCheckDivideByZero(AssemblyBuilder builder) {
            builder.getCurrent().append("BL p_check_divide_by_zero\n");
            if (!builder.getLabel().toString().contains("p_check_divide_by_zero:")) {
                builder.getLabel().append("p_check_divide_by_zero:\n");
                builder.getLabel().append("PUSH {lr}\n");
                builder.getLabel().append("CMP r1, #0\n");
                builder.getLabel().append("LDREQ r0, =msg_" + messageCount + "\n");
                builder.getLabel().append("BLEQ p_throw_runtime_error\n");
                builder.getLabel().append("POP {pc}\n");
                builder.getHeader().append("msg_" + messageCount + ":\n");
                builder.getHeader().append(".word 45\n");
                builder.getHeader().append(".ascii\t\"DivideByZeroError: divide or modulo by zero\\n\\0\"\n");
                messageCount++;
            }
            if (!builder.getLabel().toString().contains("p_throw_runtime_error:")) {
                builder.getLabel().append("p_throw_runtime_error:\n");
                builder.getLabel().append("BL p_print_string\n");
                builder.getLabel().append("MOV r0, #-1\n");
                builder.getLabel().append("BL exit\n");
                if (!builder.getLabel().toString().contains("p_print_string:")) {
                    builder.getLabel().append("p_print_string:\n");
                    builder.getLabel().append("PUSH {lr}\n");
                    Registers.Register registerZero = currentlyUsedRegister;
                    currentlyUsedRegister = registers.getFirstEmptyRegister();
                    builder.getLabel().append("LDR " + currentlyUsedRegister + ", [" + registerZero + "]\n");
                    currentlyUsedRegister.setValue(registerZero.getValue());
                    Registers.Register registerFirst = currentlyUsedRegister;
                    currentlyUsedRegister = registers.getFirstEmptyRegister();
                    builder.getLabel().append("ADD " + currentlyUsedRegister + ", " + registerZero + ", #4\n");

                    //r2 need to set value
                    currentlyUsedRegister.setValue(0);
                    registerZero.setValue(null);
                    registerFirst.setValue(null);
                    currentlyUsedRegister.setValue(null);

                    currentlyUsedRegister = registers.getFirstEmptyRegister();

                    builder.getLabel().append("LDR " + currentlyUsedRegister + ", =msg_" + messageCount + "\n");
                    builder.getLabel().append("ADD " + currentlyUsedRegister + ", " + currentlyUsedRegister + ", #4\n");
                    builder.getLabel().append("BL printf\n");
                    builder.getLabel().append("MOV " + currentlyUsedRegister + ", #0\n");
                    builder.getLabel().append("BL fflush\n");
                    builder.getLabel().append("POP {pc}\n");

                    builder.getHeader().append("msg_" + messageCount + ":\n");
                    messageCount++;
                    builder.getHeader().append(".word 5\n");
                    builder.getHeader().append(".ascii\t\"%.*s\\0\"\n");

                    registerZero.setValue(null);
                }
            }
        }

        private void generateCheckOverflow(AssemblyBuilder builder) {
            if (!builder.getLabel().toString().contains("p_throw_overflow_error:")) {
                builder.getLabel().append("p_throw_overflow_error:\n");
                builder.getLabel().append("LDR " + currentlyUsedRegister + " , =msg_" + messageCount + "\n");
                currentlyUsedRegister.setValue(true);
                builder.getLabel().append("BL p_throw_runtime_error\n");

                builder.getHeader().append("msg_" + messageCount + ":\n");
                builder.getHeader().append(".word 82\n");
                builder.getHeader().append(".ascii\t\"OverflowError: the result is too small/large"
                        + "to store in a 4-byte signed-integer.\\n\"\n");
                messageCount++;

                if (!builder.getLabel().toString().contains("p_throw_runtime_error:")) {
                    builder.getLabel().append("p_throw_runtime_error:\n");
                    builder.getLabel().append("BL p_print_string\n");
                    builder.getLabel().append("MOV r0, #-1\n");
                    builder.getLabel().append("BL exit\n");

                    if (!builder.getLabel().toString().contains("p_print_string:")) {
                        builder.getLabel().append("p_print_string:\n");
                        builder.getLabel().append("PUSH {lr}\n");
                        Registers.Register registerZero = currentlyUsedRegister;
                        currentlyUsedRegister = registers.getFirstEmptyRegister();
                        builder.getLabel().append("LDR " + currentlyUsedRegister + ", [" + registerZero + "]\n");
                        currentlyUsedRegister.setValue(registerZero.getValue());
                        Registers.Register registerFirst = currentlyUsedRegister;
                        currentlyUsedRegister = registers.getFirstEmptyRegister();
                        builder.getLabel().append("ADD " + currentlyUsedRegister + ", " + registerZero + ", #4\n");

                        //r2 need to set value
                        currentlyUsedRegister.setValue(0);
                        registerZero.setValue(null);
                        registerFirst.setValue(null);
                        currentlyUsedRegister.setValue(null);

                        currentlyUsedRegister = registers.getFirstEmptyRegister();

                        builder.getLabel().append("LDR " + currentlyUsedRegister + ", =msg_" + messageCount + "\n");
                        builder.getLabel().append("ADD " + currentlyUsedRegister + ", " + currentlyUsedRegister + ", #4\n");
                        builder.getLabel().append("BL printf\n");
                        builder.getLabel().append("MOV " + currentlyUsedRegister + ", #0\n");
                        builder.getLabel().append("BL fflush\n");
                        builder.getLabel().append("POP {pc}\n");

                        builder.getHeader().append("msg_" + messageCount + ":\n");
                        messageCount++;
                        builder.getHeader().append(".word 5\n");
                        builder.getHeader().append(".ascii\t\"%.*s\\0\"\n");

                        registerZero.setValue(null);
                    }
                }
            }
        }


        protected void generateMathsmaticsOperationCode(AssemblyBuilder builder, String operation) {
            StringBuilder currentBuilder = builder.getCurrent();

            currentlyUsedRegister = registers.getFirstEmptyRegister();

            exp1.generate(builder);

            currentBuilder.append("PUSH {" + currentlyUsedRegister + "}\n");
            currentlyUsedRegister.setValue(null);
            currentStack.incSize(4);

            currentlyUsedRegister = registers.getFirstEmptyRegister();
            exp2.generate(builder);
            if (exp2 instanceof Binary_operNode) {
                currentlyUsedRegister.setValue(true);
            }

            Registers.Register registerFirst = registers.getFirstEmptyRegister();
            currentBuilder.append("MOV " + registerFirst + ", " + currentlyUsedRegister + "\n");
            registerFirst.setValue(true);
            currentBuilder.append("POP {" + currentlyUsedRegister + "}\n");
            //need to set value
            currentStack.decSize(4);

            currentlyUsedRegister.setValue(null);
            registerFirst.setValue(null);

            switch (operation) {
                case "ADDS":
                    currentBuilder.append(operation + " " + currentlyUsedRegister + ", " + currentlyUsedRegister +
                            ", " + registerFirst + "\n");
                    currentBuilder.append("BLVS p_throw_overflow_error\n");
                    generateCheckOverflow(builder);
                    break;
                case "SUBS":
                    currentBuilder.append(operation + " " + currentlyUsedRegister + ", " + currentlyUsedRegister +
                            ", " + registerFirst + "\n");
                    currentBuilder.append("BLVS p_throw_overflow_error\n");
                    generateCheckOverflow(builder);
                    break;
                case "SMULL":
                    currentBuilder.append(operation + " " + currentlyUsedRegister + ", " + registerFirst + ", " +
                            currentlyUsedRegister + ", " + registerFirst + "\n");
                    currentBuilder.append("CMP " + registerFirst + ", " + currentlyUsedRegister + ", ASR #31\n");
                    currentBuilder.append("BLNE p_throw_overflow_error\n");
                    generateCheckOverflow(builder);
                    break;
                case "DIVS":
                    generateCheckDivideByZero(builder);
                    currentBuilder.append("BL __aeabi_idiv" + "\n");
                    break;
                case "MODS":
                    generateCheckDivideByZero(builder);
                    currentBuilder.append("BL __aeabi_idivmod" + "\n");
                    currentBuilder.append("MOV " + currentlyUsedRegister + ", " + registerFirst + "\n");
                    break;
            }
        }

    }

    public class MultNode extends Binary_operNode {

        public MultNode(ASTNode exp1, ASTNode exp2) {
            super(exp1, exp2);
            binOp = "*";
        }

        @Override
        public void check() {

            exp1.check();
            exp2.check();

            if (!exp1.getType().equals(exp2.getType())) {
                throwSemanticError("Both expressions must have the same type on mutiply binary operator");
            } else if (!exp1.getType().equals("Int")) {
                throwSemanticError("Multiplication can only take int arguments");
            }
        }

        @Override
        public String getValue() {
            return String.valueOf(Integer.valueOf(exp1.getValue()) * Integer.valueOf(exp2.getValue()));
        }

        @Override
        public void setValue() {

        }

        @Override
        public void generate(AssemblyBuilder builder) {

            generateMathsmaticsOperationCode(builder, "SMULL");

        }

    }

    public class DivNode extends Binary_operNode {

        public DivNode(ASTNode exp1, ASTNode exp2) {
            super(exp1, exp2);
            binOp = "/";
        }

        @Override
        public void check() {
            exp1.check();
            exp2.check();

            if (!exp1.getType().equals(exp2.getType())) {
                throwSemanticError("Both expressions must have the same type on divide binary operator");
            } else if (!exp1.getType().equals("Int")) {
                throwSemanticError("Division can only take int arguments");
            }
        }

        @Override
        public String getValue() {
            if (exp2.getValue().equals("0")) {
                return "-1";
            }
            return String.valueOf(Integer.valueOf(exp1.getValue()) / Integer.valueOf(exp2.getValue()));
        }

        @Override
        public void setValue() {

        }

        @Override
        public void generate(AssemblyBuilder builder) {

            generateMathsmaticsOperationCode(builder, "DIVS");

        }
    }

    public class ModNode extends Binary_operNode {

        public ModNode(ASTNode exp1, ASTNode exp2) {
            super(exp1, exp2);
            binOp = "%";
        }

        @Override
        public void check() {

            exp1.check();
            exp2.check();

            if (!exp1.getType().equals(exp2.getType())) {
                throwSemanticError("Both expressions must have the same type on mod binary operator");
            } else if (!exp1.getType().equals("Int")) {
                throwSemanticError("Modules can only take int arguments");
            }
        }

        @Override
        public String getValue() {
            return String.valueOf(Integer.valueOf(exp1.getValue()) % Integer.valueOf(exp2.getValue()));
        }

        @Override
        public void setValue() {

        }

        @Override
        public void generate(AssemblyBuilder builder) {

            generateMathsmaticsOperationCode(builder, "MODS");

        }

    }

    public class PlusNode extends Binary_operNode {

        public PlusNode(ASTNode exp1, ASTNode exp2) {
            super(exp1, exp2);
            binOp = "+";
        }

        @Override
        public void check() {

            exp1.check();
            exp2.check();

            if (!exp1.getType().equals(exp2.getType())) {
                throwSemanticError("Both expressions must have the same type on add binary operator");
            } else if (!exp1.getType().equals("Int")) {
                throwSemanticError("Addition can only take int arguments");
            }
        }

        @Override
        public String getValue() {
            String result = "";
            try {
                result = String.valueOf(Integer.valueOf(exp1.getValue()) + Integer.valueOf(exp2.getValue()));
                return result;
            } catch (NumberFormatException e) {
                throwSemanticError("Expresission type mismatch");
            }
            return result;
        }

        @Override
        public void setValue() {

        }

        @Override
        public void generate(AssemblyBuilder builder) {

            generateMathsmaticsOperationCode(builder, "ADDS");

        }
    }

    public class MinusNode extends Binary_operNode {

        public MinusNode(ASTNode exp1, ASTNode exp2) {
            super(exp1, exp2);
            binOp = "-";
        }

        @Override
        public void check() {

            exp1.check();
            exp2.check();

            if (!exp1.getType().equals(exp2.getType())) {
                throwSemanticError("Both expressions must have the same type on minus binary operator");
            } else if (!exp1.getType().equals("Int")) {
                throwSemanticError("Minus binary operator can only take int arguments");
            }
        }

        @Override
        public String getValue() {
            return String.valueOf(Integer.valueOf(exp1.getValue()) - Integer.valueOf(exp2.getValue()));
        }

        @Override
        public void setValue() {

        }

        @Override
        public void generate(AssemblyBuilder builder) {

            generateMathsmaticsOperationCode(builder, "SUBS");

        }

    }

    public class GreaterNode extends Binary_operNode {

        public GreaterNode(ASTNode exp1, ASTNode exp2) {
            super(exp1, exp2);
            binOp = ">";
        }

        @Override
        public String getType() {
            return "Bool";
        }

        @Override
        public void check() {

            exp1.check();
            exp2.check();

            if (!exp1.getType().equals(exp2.getType())) {
                throwSemanticError("Both expressions must have the same type on greater binary operator");
            } else if (!(exp1.getType().equals("Int") || exp1.getType().equals("Char"))) {
                throwSemanticError("Greater binary operator can only take int or char arguments");
            }
        }

        @Override
        public String getValue() {
            if (exp1.getType().equals("Char")) {
                return String.valueOf((int) ((exp1).getValue().charAt(1)) > ((int) (exp2).getValue().charAt(1)));
            }
            return String.valueOf(Integer.valueOf(exp1.getValue()) > Integer.valueOf(exp2.getValue()));
        }

        @Override
        public void setValue() {

        }

        @Override
        public void generate(AssemblyBuilder builder) {

            generateBranchCode(builder);
            builder.getCurrent().append("MOVGT " + currentlyUsedRegister + ", #1\n");
            builder.getCurrent().append("MOVLE " + currentlyUsedRegister + ", #0\n");

        }

    }

    public class GreaterOrEqualNode extends Binary_operNode {

        public GreaterOrEqualNode(ASTNode exp1, ASTNode exp2) {
            super(exp1, exp2);
            binOp = ">=";
        }

        @Override
        public String getType() {
            return "Bool";
        }

        @Override
        public void check() {

            exp1.check();
            exp2.check();

            if (!exp1.getType().equals(exp2.getType())) {
                throwSemanticError("Both expressions must have the same type on greater or equal binary operator");
            } else if (!(exp1.getType().equals("Int") || exp1.getType().equals("Char"))) {
                throwSemanticError("Greater or equal binary operator can only take int or char arguments");
            }
        }

        @Override
        public String getValue() {
            if (exp1.getType().equals("Char")) {
                return String.valueOf((int) ((exp1).getValue().charAt(1)) >= ((int) (exp2).getValue().charAt(1)));
            }
            return String.valueOf(Integer.valueOf(exp1.getValue()) >= Integer.valueOf(exp2.getValue()));
        }

        @Override
        public void setValue() {

        }

        @Override
        public void generate(AssemblyBuilder builder) {

            generateBranchCode(builder);
            builder.getCurrent().append("MOVGE " + currentlyUsedRegister + ", #1\n");
            builder.getCurrent().append("MOVLT " + currentlyUsedRegister + ", #0\n");

        }

    }

    public class SmallerNode extends Binary_operNode {

        public SmallerNode(ASTNode exp1, ASTNode exp2) {
            super(exp1, exp2);
            binOp = "<";
        }

        @Override
        public String getType() {
            return "Bool";
        }

        @Override
        public void check() {

            exp1.check();
            exp2.check();

            if (!exp1.getType().equals(exp2.getType())) {
                throwSemanticError("Both expressions must have the same type on smaller binary operator");
            } else if (!(exp1.getType().equals("Int") || exp1.getType().equals("Char"))) {
                throwSemanticError("Smaller binary operator can only take int or char arguments");
            }
        }

        @Override
        public String getValue() {
            if (exp1.getType().equals("Char")) {
                return String.valueOf((int) ((exp1).getValue().charAt(1)) < ((int) (exp2).getValue().charAt(1)));
            }
            return String.valueOf(Integer.valueOf(exp1.getValue()) < Integer.valueOf(exp2.getValue()));
        }

        @Override
        public void setValue() {

        }

        @Override
        public void generate(AssemblyBuilder builder) {
            generateBranchCode(builder);

            builder.getCurrent().append("MOVLT " + currentlyUsedRegister + ", #1\n");
            builder.getCurrent().append("MOVGE " + currentlyUsedRegister + ", #0\n");

        }
    }

    public class SmallerOrEqualNode extends Binary_operNode {

        public SmallerOrEqualNode(ASTNode exp1, ASTNode exp2) {
            super(exp1, exp2);
            binOp = "<=";
        }

        @Override
        public String getType() {

            return "Bool";
        }

        @Override
        public void check() {

            exp1.check();
            exp2.check();

            if (!exp1.getType().equals(exp2.getType())) {
                throwSemanticError("Both expressions must have the same type on smaller or equal binary operator");
            } else if (!(exp1.getType().equals("Int") || exp1.getType().equals("Char"))) {
                throwSemanticError("Smaller or equal binary operator can only take int or char arguments");
            }
        }

        @Override
        public String getValue() {
            if (exp1.getType().equals("Char")) {
                return String.valueOf((int) ((exp1).getValue().charAt(1)) <= ((int) (exp2).getValue().charAt(1)));
            }
            return String.valueOf(Integer.valueOf(exp1.getValue()) <= Integer.valueOf(exp2.getValue()));
        }

        @Override
        public void setValue() {

        }

        @Override
        public void generate(AssemblyBuilder builder) {
            generateBranchCode(builder);

            builder.getCurrent().append("MOVLE " + currentlyUsedRegister + ", #1\n");
            builder.getCurrent().append("MOVGT " + currentlyUsedRegister + ", #0\n");
        }

    }

    public class EqualNode extends Binary_operNode {

        public EqualNode(ASTNode exp1, ASTNode exp2) {
            super(exp1, exp2);
            binOp = "==";
        }

        @Override
        public String getType() {
            return "Bool";
        }

        @Override
        public void check() {

            exp1.check();
            exp2.check();

            if (!exp1.getType().equals(exp2.getType())) {
                throwSemanticError("Both expressions must have the same type on equal binary operator");
            }
        }

        @Override
        public String getValue() {
            if (exp1 instanceof Str_literNode || exp2 instanceof Str_literNode) {
                return "false";
            } else if (exp1.getType().equals("String")) {
                return String.valueOf(((IdentNode) exp1).getTypeNode().getIdent().equals(((IdentNode) exp2).getTypeNode().getIdent()));
            }
            return String.valueOf(exp1.getValue().equals(exp2.getValue()));
        }

        @Override
        public void setValue() {

        }

        @Override
        public void generate(AssemblyBuilder builder) {
            generateBranchCode(builder);

            builder.getCurrent().append("MOVEQ " + currentlyUsedRegister + ", #1\n");
            builder.getCurrent().append("MOVNE " + currentlyUsedRegister + ", #0\n");
        }
    }

    public class NotEqualNode extends Binary_operNode {

        public NotEqualNode(ASTNode exp1, ASTNode exp2) {
            super(exp1, exp2);
            binOp = "!=";
        }

        @Override
        public String getType() {
            return "Bool";
        }

        @Override
        public void check() {

            exp1.check();
            exp2.check();

            if (!exp1.getType().equals(exp2.getType())) {
                throwSemanticError("Both expressions must have the same type on not equal binary operator");
            }
        }

        @Override
        public String getValue() {
            if (exp1 instanceof Str_literNode || exp2 instanceof Str_literNode) {
                return "true";
            } else if (exp1.getType().equals("String")) {
                return String.valueOf(!((IdentNode) exp1).getTypeNode().getIdent().equals(((IdentNode) exp2).getTypeNode().getIdent()));
            }
            return String.valueOf(!exp1.getValue().equals(exp2.getValue()));
        }

        @Override
        public void setValue() {

        }

        @Override
        public void generate(AssemblyBuilder builder) {
            generateBranchCode(builder);

            builder.getCurrent().append("MOVNE " + currentlyUsedRegister + ", #1\n");
            builder.getCurrent().append("MOVEQ " + currentlyUsedRegister + ", #0\n");
        }
    }

    public class LogicalAndNode extends Binary_operNode {

        public LogicalAndNode(ASTNode exp1, ASTNode exp2) {
            super(exp1, exp2);
            binOp = "&&";
        }

        @Override
        public void check() {

            exp1.check();
            exp2.check();

            if (!exp1.getType().equals(exp2.getType())) {
                throwSemanticError("Both expressions must have the same type on not logical and operator");
            } else if (!exp1.getType().equals("Bool")) {
                throwSemanticError("Logical and operator can only take bool arguments");
            }
        }

        @Override
        public String getValue() {
            return String.valueOf(Boolean.valueOf(exp1.getValue()) && Boolean.valueOf(exp2.getValue()));
        }

        @Override
        public void setValue() {

        }

        @Override
        public void generate(AssemblyBuilder builder) {
            StringBuilder currentBuilder = builder.getCurrent();

            Registers.Register exp1Register = registers.getFirstEmptyRegister();
            currentlyUsedRegister = exp1Register;
            exp1.generate(builder);
            currentlyUsedRegister.setValue(null);

            String label = "L" + labelCount;
            labelCount++;
            currentBuilder.append("CMP " + currentlyUsedRegister + ", #0\n");
            currentBuilder.append("BEQ " + label + "\n");

            Registers.Register exp2Register = registers.getFirstEmptyRegister();
            currentlyUsedRegister = exp2Register;
            exp2.generate(builder);
            exp2Register.setValue(null);
            currentBuilder.append(label + ":\n");
        }
    }

    public class LogicalOrNode extends Binary_operNode {

        public LogicalOrNode(ASTNode exp1, ASTNode exp2) {
            super(exp1, exp2);
            binOp = "||";
        }

        @Override
        public void check() {

            exp1.check();
            exp2.check();
            if (!exp1.getType().equals(exp2.getType())) {
                throwSemanticError("Both expressions must have the same type on not logical or operator");
            } else if (!exp1.getType().equals("Bool")) {
                throwSemanticError("Logical or operator can only take bool arguments");
            }
        }

        @Override
        public String getValue() {
            return String.valueOf(Boolean.valueOf(exp1.getValue()) || Boolean.valueOf(exp2.getValue()));
        }

        @Override
        public void setValue() {

        }

        @Override
        public void generate(AssemblyBuilder builder) {
            StringBuilder currentBuilder = builder.getCurrent();

            Registers.Register exp1Register = registers.getFirstEmptyRegister();
            currentlyUsedRegister = exp1Register;
            exp1.generate(builder);
            currentlyUsedRegister.setValue(null);

            String label = "L" + labelCount;
            labelCount++;
            currentBuilder.append("CMP " + currentlyUsedRegister + ", #1\n");
            currentBuilder.append("BEQ " + label + "\n");

            Registers.Register exp2Register = registers.getFirstEmptyRegister();
            currentlyUsedRegister = exp2Register;
            exp2.generate(builder);
            exp2Register.setValue(null);
            currentBuilder.append(label + ":\n");
        }
    }

    public abstract class ExprNode extends StatNode {
        @Override
        public void check() {
        }

    }

    /*
     * IdentNode checks through the related scopes for the detail of a varible
     * getType() method returns the correct type of the ident
     */
    public class IdentNode extends ExprNode {

        private String ident;
        private String value;

        public IdentNode(String ident) {
            this.ident = ident;
        }

        public String getIdent() {
            return ident;
        }

        public void setIdent(String ident) {
            this.ident = ident;
        }

        public TypeNode getTypeNode() {

            ASTNode parent = getParent();
            ASTNode typeNode = null;
            while (parent != null && typeNode == null) {
                if (typeNode instanceof FuncNode) {
                    return (TypeNode) typeNode;
                }
                typeNode = parent.getSymbolTable().get(ident);
                parent = parent.getParent();
            }
            return (TypeNode) typeNode;

        }

        @Override
        public String getType() {

            ASTNode typeNode = getTypeNode();

            if (typeNode == null) {
                return "";
            }
            return typeNode.getType();

        }

        @Override
        public void check() {
            checkIfVaribleExist(this);
        }

        @Override
        public String getValue() {
            ASTNode node = getTypeNode();
            return getTypeNode().getValue();
        }

        @Override
        public void setValue() {

        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public void generate(AssemblyBuilder builder) {
            if (getType().equals("Int") || getType().equals("String") || getType().contains("[]")) {
                builder.getCurrent().append("LDR " + currentlyUsedRegister + getStackPointer() + "\n");
            } else {
                builder.getCurrent().append("LDRSB " + currentlyUsedRegister + getStackPointer() + "\n");
            }
            currentlyUsedRegister.setValue(true);
        }

    }

    /*
     * The single element of an array
     */
    public class Array_elemNode extends ExprNode {

        private IdentNode identNode;
        private List<ExprNode> exprNodes;

        public Array_elemNode(IdentNode identNode, List<ExprNode> exprNodes) {

            this.identNode = identNode;
            identNode.setParent(this);
            this.exprNodes = exprNodes;
            for (ExprNode exprNode : exprNodes) {
                exprNode.setParent(this);
            }

        }

        @Override
        public void check() {
            identNode.check();
            for (ExprNode exprNode : exprNodes) {
                exprNode.check();
            }
            checkIfVaribleExist(identNode);
        }

        @Override
        public String getValue() {
            Array_typeNode array_typeNode = (Array_typeNode) identNode.getTypeNode();
            return String.valueOf(array_typeNode.getArrayValue().get(Integer.valueOf(exprNodes.get(0).getValue())).getValue());
        }

        @Override
        public void setValue() {

        }

        @Override
        public void generate(AssemblyBuilder builder) {
            Registers.Register currentlyUsedRegister = registers.getFirstEmptyRegister();

            builder.getCurrent().append("PUSH {" + currentlyUsedRegister + ", r4}\n");
            currentStack.incSize(8);
            builder.getCurrent().append("LDR r4" + identNode.getStackPointer() + "\n");

            //not sure
            if (((AssignmentNode) getParent()).assign_rhsNode instanceof IdentNode) {
                builder.getCurrent().append("LDR " + currentlyUsedRegister + getStackPointer() + "\n");
            } else {
                builder.getCurrent().append("LDR " + currentlyUsedRegister + ", =" + getIndex() + "\n");
            }

            currentlyUsedRegister.setValue(true);
            builder.getCurrent().append("BL p_check_array_bounds\n");
            generateCheckArrayBounds(builder);
            builder.getCurrent().append("ADD r4, r4, #4\n");
            String lsl = "";
            if (getType().contains("Int")) {
                lsl = ", LSL #2";
            }
            currentlyUsedRegister.setValue(true);
            builder.getCurrent().append("ADD r4, r4, " + currentlyUsedRegister + lsl + "\n");
            Registers.Register r1 = registers.getFirstEmptyRegister();
            builder.getCurrent().append("MOV " + r1 + ", r4\n");
            r1.setValue(true);
            builder.getCurrent().append("POP {r0, r4}\n");
            currentStack.decSize(8);
            r1.setValue(null);
        }

        @Override
        public String getType() {
            return (identNode.getTypeNode()).getType();
        }

        private TypeNode lookupSymbolTable(ASTNode currentScope, String string) {
            while (!currentScope.getScope()) {
                currentScope = currentScope.getParent();
            }
            return (TypeNode) currentScope.getSymbolTable().get(string);
        }

        public int getIndex() {
            return Integer.valueOf(exprNodes.get(0).getValue());
        }

        public IdentNode getIdentNode() {
            return identNode;
        }
    }

    /*
     * All liters use the getType() method to return their type
     */
    public class Int_literNode extends ExprNode {

        private String value;
        private String sign;

        public Int_literNode(String sign, String value) {
            this.value = value;
            this.sign = sign;
        }

        @Override
        public String getType() {
            return "Int";
        }

        @Override
        public void generate(AssemblyBuilder builder) {

            int num = Integer.parseInt(value);
            if (sign.equals("-")) {
                num *= -1;
            }
            builder.getCurrent().append("LDR " + currentlyUsedRegister + ", =" + num + "\n");
            currentlyUsedRegister.setValue(true);
        }

        public int getvalue() {
            if (sign.equals("-")) {
                return Integer.parseInt(value) / (-1);
            }
            return Integer.parseInt(value);
        }

        @Override
        public String getValue() {
            long v = 0;
            if (sign.equals("-")) {
                v = Long.parseLong(value) / (-1);
            }
            v =  Long.parseLong(value);
            return String.valueOf(v);
        }

        @Override
        public void setValue() {

        }
    }


    public class Bool_literNode extends ExprNode {

        private boolean value;

        public Bool_literNode(String value) {
            this.value = Boolean.parseBoolean(value);
        }

        public String getValue() {
            return String.valueOf(value);
        }

        @Override
        public void setValue() {

        }

        @Override
        public String getType() {

            return "Bool";
        }

        @Override
        public void generate(AssemblyBuilder builder) {
            if (value) {
                builder.getCurrent().append("MOV " + currentlyUsedRegister + ", #1\n");
            } else {
                builder.getCurrent().append("MOV " + currentlyUsedRegister + ", #0\n");
            }
            currentlyUsedRegister.setValue(true);
        }
    }

    public class Char_literNode extends ExprNode {

        private String value;
        private char c;

        public Char_literNode(String v) {
            c = v.charAt(1);
            if (v.charAt(1) == '\\') {
                switch (v.charAt(2)) {
                    case '0':
                        value = "0";
                        break;
                    case 'b':
                        value = "8";
                        break;
                    case 't':
                        value = "9";
                        break;
                    case 'n':
                        value = "10";
                        break;
                    case 'f':
                        value = "12";
                        break;
                    case 'r':
                        value = "13";
                        break;
                    case '"':
                        value = "\'\"\'";
                        break;
                    case '\'':
                        value = "\'\\\'\'";
                        break;
                    case '\\':
                        value = "\'\\\'";
                        break;
                }

            } else {
                this.value = "\'" + v.charAt(1) + "\'";
            }
        }

        public char getChar() {
            return c;
        }

        @Override
        public String getType() {

            return "Char";
        }

        @Override
        public void generate(AssemblyBuilder builder) {
            builder.getCurrent().append("MOV " + currentlyUsedRegister + ", #" + value + "\n");
            currentlyUsedRegister.setValue(true);
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public void setValue() {

        }
    }

    public class Str_literNode extends ExprNode {

        private String value;

        public Str_literNode(String value) {
            this.value = value;
        }

        @Override
        public String getType() {

            return "String";
        }

        @Override
        public void generate(AssemblyBuilder builder) {

            builder.getCurrent().append("LDR " + currentlyUsedRegister + ", =msg_" + messageCount + "\n");
            currentlyUsedRegister.setValue(true);
            builder.getHeader().append("msg_" + messageCount + ": \n");
            builder.getHeader().append(".word " + getWordLength(value) + "\n");
            builder.getHeader().append(".ascii\t" + value + "\n");
            messageCount++;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public void setValue() {

        }
    }


    public class Array_literNode extends ASTNode {

        List<ASTNode> exprNodeList;

        public Array_literNode(List<ASTNode> exprNodeList) {

            this.exprNodeList = exprNodeList;
            for (ASTNode node : exprNodeList) {
                node.setParent(this);
            }

        }

        @Override
        public String getType() {
            if (exprNodeList.size() == 0) {
                return "[]";
            }
            return exprNodeList.get(0).getType() + "[]";
        }

        @Override
        public String getValue() {
            return null;
        }

        private String getElemType() {
            return exprNodeList.get(0).getType();
        }

        @Override
        public void check() {

            for (ASTNode astNode : exprNodeList) {
                astNode.check();
            }
            if (!getType().equals("")) {
                for (ASTNode astNode : exprNodeList) {
                    if (!astNode.getType().equals(getElemType())) {
                        throwSemanticError("Array need to contain same type arguments");
                    }
                }
            }
        }

        public int getLength() {
            return exprNodeList.size();
        }

        @Override
        public void generate(AssemblyBuilder builder) {
            Registers.Register currentlyUsedRegister = registers.getFirstEmptyRegister();
            Registers.Register register3 = registers.get(3);
            builder.getCurrent().append("MOV " + currentlyUsedRegister + ", #" + (4 + 4 * exprNodeList.size()) + "\n");
            builder.getCurrent().append("BL malloc\n");
            builder.getCurrent().append("MOV " + register3 + ", " + currentlyUsedRegister +"\n");
            for (int i = 0; i < exprNodeList.size(); i++) {
                builder.getCurrent().append("LDR " + currentlyUsedRegister + ", =" + exprNodeList.get(i).getValue() + "\n");
                builder.getCurrent().append("STR " + currentlyUsedRegister + ", [" + register3 + ", #" + (4 * i + 4) + "]\n");
            }

            builder.getCurrent().append("MOV " + currentlyUsedRegister + ", #" + exprNodeList.size() + "\n");
            builder.getCurrent().append("STR " + currentlyUsedRegister + ", [" + register3 + "]\n");
            builder.getCurrent().append("MOV " + currentlyUsedRegister + ", " + register3 + "\n");
        }

        public List<ASTNode> getArrayValue() {
            return exprNodeList;
        }

    }

    public class Pair_literNode extends ExprNode {

        @Override
        public String getType() {

            return "Null";
        }

        @Override
        public void generate(AssemblyBuilder builder) {
            builder.getCurrent().append("MOV r0, #0\n");
        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void setValue() {

        }
    }

    public class NewPairNode extends Pair_typeNode {

        private ExprNode exprNode1;
        private ExprNode exprNode2;

        public NewPairNode(ExprNode exprNode1, ExprNode exprNode2) {

            super(exprNode1, exprNode2);
            this.exprNode1 = exprNode1;
            exprNode1.setParent(this);
            this.exprNode2 = exprNode2;
            exprNode2.setParent(this);

        }

        public String getType() {
            return "Pair";
        }

        @Override
        public void check() {
            exprNode1.check();
            exprNode2.check();
        }

        public void generate(AssemblyBuilder builder) {
            Registers.Register tempReg;

            currentlyUsedRegister = registers.getFirstEmptyRegister();
            exprNode1.generate(builder);
            builder.getCurrent().append("PUSH {" + currentlyUsedRegister + "}\n");
            if ((exprNode1.getType().contains("Int") || exprNode1.getType().contains("String") || exprNode1.getType().contains("Pair")) || exprNode1.getType().contains("[]")) {
                builder.getCurrent().append("MOV " + currentlyUsedRegister + ", #4\n");
            } else {
                builder.getCurrent().append("MOV " + currentlyUsedRegister + ", #1\n");
            }
            builder.getCurrent().append("BL malloc\n");
            tempReg = currentlyUsedRegister;
            currentlyUsedRegister = registers.getFirstEmptyRegister();
            builder.getCurrent().append("POP {" + currentlyUsedRegister + "}\n");
            if (exprNode1.getType().contains("Int") || exprNode1.getType().contains("String") || exprNode1.getType().contains("Pair") || exprNode1.getType().contains("[]")) {
                builder.getCurrent().append("STR " + currentlyUsedRegister + ", [" + tempReg + "]\n");
            } else {
                builder.getCurrent().append("STRB " + currentlyUsedRegister + ", [" + tempReg + "]\n");
            }
            tempReg.setValue(null);
            builder.getCurrent().append("PUSH {" + tempReg + "}\n");
            currentlyUsedRegister = registers.getFirstEmptyRegister();
            exprNode2.generate(builder);
            builder.getCurrent().append("PUSH {" + currentlyUsedRegister + "}\n");
            if ((exprNode2.getType().contains("Int") || exprNode2.getType().contains("String") || exprNode2.getType().contains("Pair")) || exprNode2.getType().contains("[]")) {
                builder.getCurrent().append("MOV " + currentlyUsedRegister + ", #4\n");
            } else {
                builder.getCurrent().append("MOV " + currentlyUsedRegister + ", #1\n");
            }
            builder.getCurrent().append("BL malloc\n");
            tempReg = currentlyUsedRegister;
            currentlyUsedRegister = registers.getFirstEmptyRegister();
            builder.getCurrent().append("POP {" + currentlyUsedRegister + "}\n");
            if (exprNode2.getType().contains("Int") || exprNode2.getType().contains("String") || exprNode2.getType().contains("Pair") || exprNode2.getType().contains("[]")) {
                builder.getCurrent().append("STR " + currentlyUsedRegister + ", [" + tempReg + "]\n");
            } else {
                builder.getCurrent().append("STRB " + currentlyUsedRegister + ", [" + tempReg + "]\n");
            }
            builder.getCurrent().append("PUSH {" + tempReg + "}\n");
            builder.getCurrent().append("MOV " + tempReg + ", #8\n");
            builder.getCurrent().append("BL malloc\n");
            builder.getCurrent().append("POP {" + currentlyUsedRegister + ", " + currentlyUsedRegister.getNext() + "}\n");
            builder.getCurrent().append("STR " + currentlyUsedRegister.getNext() + ", [" + tempReg + "]\n");
            builder.getCurrent().append("STR " + currentlyUsedRegister + ", [" + tempReg + ", #4]\n");
            tempReg.setValue(null);
            currentlyUsedRegister = registers.getFirstEmptyRegister();
        }

    }

    /*
     * Used when calling a method
     * Check the if the arguments are valid or not
     * Goes through the function symboltable in program scope
     */
    public class CallNode extends ASTNode {

        private IdentNode identNode;
        private List<ExprNode> exprNodeList;
        private FuncNode funcNode;
        private int stackSize = 0;

        public CallNode(IdentNode identNode, List<ExprNode> exprNodeList) {

            this.identNode = identNode;
            identNode.setParent(this);
            this.exprNodeList = exprNodeList;
            for (ExprNode exprNode : exprNodeList) {
                exprNode.setParent(this);
            }
        }

        public String getType() {
            FuncNode funcNode = (FuncNode) getRoot().getFunctionSymbolTable().get(identNode.getIdent());
            return funcNode.getType();
        }

        @Override
        public String getValue() {
            return null;
        }

        public TypeNode getTypeNode() {
            return identNode.getTypeNode();
        }

        @Override
        public void check() {

            for (int i = exprNodeList.size() - 1; i >= 0; i--) {
                ExprNode exprNode = exprNodeList.get(i);
                exprNode.check();
            }

            if (!getRoot().getFunctionSymbolTable().containsKey(identNode.getIdent())) {
                throwSemanticError("The function " + identNode.getIdent() + " has not been declared");
            }
            funcNode = (FuncNode) getRoot().getFunctionSymbolTable().get(identNode.getIdent());
            if (funcNode.getParamNodes().size() != exprNodeList.size()) {
                throwSemanticError("Argument size not matched in function: " + identNode.getIdent());
            }
            for (int i = 0; i < exprNodeList.size(); i++) {
                if (!funcNode.getParamNodes().get(i).getType().equals(exprNodeList.get(i).getType())) {
                    throwSemanticError("The " + (i + 1) + "th argument in function "
                            + identNode.getIdent() + " not match");
                }
            }
        }



        @Override
        public void generate(AssemblyBuilder builder) {

            funcStack = new Stack();

            StringBuilder currentStringBuilder = builder.getCurrent();

            for (ExprNode exprNode : exprNodeList) {
                if (exprNode instanceof IdentNode) {
                    funcStack.add(((IdentNode) exprNode).getIdent(),
                            calculateNumOfByte(((IdentNode) exprNode).getTypeNode().getType()));
                } else {
                    funcStack.incSize(calculateNumOfByte(exprNode.getType()));
                }
            }

            for (int i = exprNodeList.size() - 1; i >= 0; i--) {

                ExprNode exprNode = exprNodeList.get(i);
                currentlyUsedRegister = registers.getFirstEmptyRegister();

                exprNode.generate(builder);
                int numOfByte = calculateNumOfByte(exprNode.getType());
                if (getType().equals("Int") || getType().equals("String")) {
                    currentStringBuilder.append("STR " + currentlyUsedRegister + getStackPointer(-numOfByte) + "!" + "\n");
                } else {
                    currentStringBuilder.append("STRB " + currentlyUsedRegister + getStackPointer(-numOfByte) + "!" + "\n");
                }
                currentlyUsedRegister.setValue(null);
            }

            currentStack = funcStack;
            currentStringBuilder.append("BL f_" + identNode.getIdent() + "\n");
            addBackToStack(builder);
            currentStack = programStack;

        }

        private String getStackPointer(int offset) {
            String result;
            if (offset == 0) {
                result = ", [sp";
            } else {
                result = ", [sp, #" + offset;
            }
            return result + "]";
        }
    }

    public class BreakNode extends StatNode{

        @Override
        public String getType() {
            return null;
        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void check() {
            getLoopParent();
        }

        @Override
        public void generate(AssemblyBuilder builder) {
            builder.getCurrent().append("B END" + loopCount + "\n");
        }

        @Override
        public void setValue() {

        }
        private ASTNode getLoopParent() {
            ASTNode parent = getParent();
            while (!(parent instanceof WhileNode || parent instanceof ForNode)) {
                if (parent instanceof ProgramNode) {
                    throwSemanticError("Break should only exist in loop body!");
                } else {
                    parent = parent.getParent();
                }
            }
            return parent;
        }
    }

    public class ContinueNode extends StatNode{

        @Override
        public String getType() {
            return null;
        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void check() {
            getLoopParent();
        }

        @Override
        public void generate(AssemblyBuilder builder) {
            int labelNum = 0;
            if (getLoopParent() instanceof WhileNode) {
                labelNum = ((WhileNode) getLoopParent()).getLabelNumber();
            } else if (getLoopParent() instanceof ForNode) {
                labelNum= ((ForNode) getLoopParent()).getLabelNumber();
            }
            builder.getCurrent().append("B L" + labelNum + "\n");
        }

        @Override
        public void setValue() {

        }

        private ASTNode getLoopParent() {
            ASTNode parent = getParent();
            while (!(parent instanceof WhileNode || parent instanceof ForNode)) {
                if (parent instanceof ProgramNode) {
                    throwSemanticError("Continue should only exist in loop body!");
                } else {
                    parent = parent.getParent();
                }
            }
            return parent;
        }
    }
}
