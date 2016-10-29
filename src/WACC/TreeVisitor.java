package WACC;

import antlr.WACCParser;
import antlr.WACCParserBaseVisitor;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;

public class TreeVisitor extends WACCParserBaseVisitor<AST.ASTNode> {

    AST ast = new AST();

    /**
     * {@inheritDoc}
     * <p/>
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitProgram(@NotNull WACCParser.ProgramContext ctx) {
        AST.ProgramNode programNode = null;
        List<AST.FuncNode> functionNodes = new ArrayList<>();
        for (WACCParser.FuncContext functionContext : ctx.func()) {
            AST.FuncNode funcNode = (AST.FuncNode) visit(functionContext);
            functionNodes.add(funcNode);
        }

        AST.StatNode statNode = (AST.StatNode) visit(ctx.stat());

        programNode = ast.new ProgramNode(functionNodes, statNode);


      /*  for (int i = 0; i < functionNodes.size(); i++) {
            programNode.getFunctionSymbolTable().put(ctx.func().get(i).ident().getText(), functionNodes.get(i));
        }
*/
        return programNode;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitFunc(@NotNull WACCParser.FuncContext ctx) {

        List<AST.ParamNode> paramNodeList = new ArrayList<>();
        List<ParseTree> paramContextList = new ArrayList<>();

        if (ctx.param_list() != null) {
            for (int i = 0; i < ctx.param_list().getChildCount(); i = i + 2) {
                paramContextList.add(ctx.param_list().getChild(i));
            }
        }

        for (ParseTree param : paramContextList) {
            paramNodeList.add((AST.ParamNode) visit(param));
        }

        AST.FuncNode funcNode = ast.new FuncNode((AST.TypeNode) visit(ctx.type()), (AST.IdentNode) visit(ctx.ident()),
                paramNodeList, (AST.StatNode) visit(ctx.func_return()));

//        for (int i = 0; i < paramContextList.size(); i++) {
//            BasicParser.ParamContext paramContext = (BasicParser.ParamContext) paramContextList.get(i);
//            funcNode.getSymbolTable().put(paramContext.ident().getText(), paramNodeList.get(i).getTypeNode());
//        }
        return funcNode;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitFunc_return(@NotNull WACCParser.Func_returnContext ctx) {
        if (ctx.IF() != null) {
            return ast.new IfNode((AST.ExprNode) visit(ctx.expr()), (AST.StatNode) visit(ctx.func_return(0)), (AST.StatNode) visit(ctx.func_return(1)));
        } else if (ctx.SEMICOLON() != null) {
            return ast.new MultipleStatNode((AST.StatNode) visit(ctx.stat()), (AST.StatNode) visit(ctx.expr()));
        } else if (ctx.expr() != null) {
            return ast.new ReturnNode((AST.ExprNode) visit(ctx.expr()));
        }
        System.out.println("Error");
        return visitChildren(ctx);
    }

    /**
     * {@inheritDoc}
     *
     * @author Davies
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitIdent(@NotNull WACCParser.IdentContext ctx) {

        return ast.new IdentNode(ctx.getText());
    }

    /**
     * {@inheritDoc}
     *
     * @author Davies
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitPair_liter(@NotNull WACCParser.Pair_literContext ctx) {

        return ast.new Pair_literNode();
    }

    /**
     * {@inheritDoc}
     *
     * @author Davies
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitParam(@NotNull WACCParser.ParamContext ctx) {

        AST.TypeNode typeNode = (AST.TypeNode) visit(ctx.type());

        AST.IdentNode identNode = (AST.IdentNode) visit(ctx.ident());

        AST.ParamNode paramNode = ast.new ParamNode(typeNode, identNode);

        return paramNode;
    }

    /**
     * {@inheritDoc}
     *
     * @author Davies
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitExpr(@NotNull WACCParser.ExprContext ctx) {
        if (ctx.OPEN_PARENTHESES() != null) {
            return visit(ctx.expr(0));
        } else if (ctx.GREATER() != null) {
            return ast.new GreaterNode(visit(ctx.expr(0)), visit(ctx.expr(1)));
        } else if (ctx.GREATER_OR_EQUAL() != null) {
            return ast.new GreaterOrEqualNode(visit(ctx.expr(0)), visit(ctx.expr(1)));
        } else if (ctx.SMALLER() != null) {
            return ast.new SmallerNode(visit(ctx.expr(0)), visit(ctx.expr(1)));
        } else if (ctx.SMALLER_OR_EQUAL() != null) {
            return ast.new SmallerOrEqualNode(visit(ctx.expr(0)), visit(ctx.expr(1)));
        } else if (ctx.EQUAL() != null) {
            return ast.new EqualNode(visit(ctx.expr(0)), visit(ctx.expr(1)));
        } else if (ctx.NOT_EQUAL() != null) {
            return ast.new NotEqualNode(visit(ctx.expr(0)), visit(ctx.expr(1)));
        } else if (ctx.LOGICAL_AND() != null) {
            return ast.new LogicalAndNode(visit(ctx.expr(0)), visit(ctx.expr(1)));
        } else if (ctx.LOGICAL_OR() != null) {
            return ast.new LogicalOrNode(visit(ctx.expr(0)), visit(ctx.expr(1)));
        } else if (ctx.MULT() != null) {
            return ast.new MultNode(visit(ctx.expr(0)), visit(ctx.expr(1)));
        } else if (ctx.DIV() != null) {
            return ast.new DivNode(visit(ctx.expr(0)), visit(ctx.expr(1)));
        } else if (ctx.MOD() != null) {
            return ast.new ModNode(visit(ctx.expr(0)), visit(ctx.expr(1)));
        } else if (ctx.PLUS() != null) {
            return ast.new PlusNode(visit(ctx.expr(0)), visit(ctx.expr(1)));
        } else if (ctx.MINUS() != null) {
            return ast.new MinusNode(visit(ctx.expr(0)), visit(ctx.expr(1)));
        } else if (ctx.unary_oper() != null) {
            switch (ctx.unary_oper().getText()) {
                case "!":
                    return ast.new NotOperNode((AST.ExprNode) visit(ctx.expr(0)));
                case "-":
                    return ast.new NegateOperNode((AST.ExprNode) visit(ctx.expr(0)));
                case "len":
                    return ast.new LenOperNode((AST.ExprNode) visit(ctx.expr(0)));
                case "ord":
                    return ast.new OrdOperNode((AST.ExprNode) visit(ctx.expr(0)));
                case "chr":
                    return ast.new CharOperNode((AST.ExprNode) visit(ctx.expr(0)));
            }

        } else if (ctx.array_elem() != null) {
            return visit(ctx.array_elem());

        } else if (ctx.ident() != null) {
            return visit(ctx.ident());

        } else if (ctx.int_liter() != null) {
            return visit(ctx.int_liter());

        } else if (ctx.bool_liter() != null) {
            return visit(ctx.bool_liter());

        } else if (ctx.char_liter() != null) {
            return visit(ctx.char_liter());

        } else if (ctx.str_liter() != null) {
            return visit(ctx.str_liter());

        } else if (ctx.pair_liter() != null) {
            return visit(ctx.pair_liter());

        }
        System.out.println("Error");
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @author Davies
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitType(@NotNull WACCParser.TypeContext ctx) {
        if (ctx.base_type() != null) {
            visit(ctx.base_type());
        } else if (ctx.OPEN_SQUARE_BRACKET() != null) {
            return ast.new Array_typeNode((AST.TypeNode) visit(ctx.type()));
        } else if (ctx.pair_type() != null) {
            return visit(ctx.pair_type());
        }
        return visitChildren(ctx);
    }

    /**
     * {@inheritDoc}
     *
     * @author Davies
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitUnary_oper(@NotNull WACCParser.Unary_operContext ctx) {
        //TODO
        System.out.println("Not Implemented");
        return visitChildren(ctx);
    }

    /**
     * {@inheritDoc}
     *
     * @author Davies
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitPair_elem(@NotNull WACCParser.Pair_elemContext ctx) {
        if (ctx.FST() != null) {

            return ast.new FSTNode((AST.IdentNode) visit(ctx.expr()));
        } else {
            return ast.new SNDNode((AST.IdentNode) visit(ctx.expr()));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @author Davies
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitArray_type(@NotNull WACCParser.Array_typeContext ctx) {

        return ast.new Array_typeNode((AST.TypeNode) visit(ctx.type()));
    }

    /**
     * {@inheritDoc}
     *
     * @author YinJun
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitBase_type(@NotNull WACCParser.Base_typeContext ctx) {
        if (ctx.BOOL() != null) {
            return ast.new BoolTypeNode();
        } else if (ctx.CHAR() != null) {
            return ast.new CharTypeNode();
        } else if (ctx.INT() != null) {
            return ast.new IntTypeNode();
        } else {
            return ast.new StringTypeNode();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @author YinJun
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitPair_type(@NotNull WACCParser.Pair_typeContext ctx) {


        return ast.new Pair_typeNode((AST.ASTNode) visit(ctx.pair_elem_type(0)),
                (AST.ASTNode) visit(ctx.pair_elem_type(1)));
    }

    /**
     * {@inheritDoc}
     *
     * @author YinJun
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitStr_liter(@NotNull WACCParser.Str_literContext ctx) {

        return ast.new Str_literNode(ctx.STR_LITER().getText());
    }

    /**
     * {@inheritDoc}
     *
     * @author YinJun
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitInt_sign(@NotNull WACCParser.Int_signContext ctx) {
        //TODO
        System.out.println("Not Implemented");
        return visitChildren(ctx);
    }

    /**
     * {@inheritDoc}
     *
     * @author YinJun
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitAssign_lhs(@NotNull WACCParser.Assign_lhsContext ctx) {
        if (ctx.array_elem() != null) {
            return visit(ctx.array_elem());
        } else if (ctx.ident() != null) {
            return visit(ctx.ident());
        } else if (ctx.pair_elem() != null) {
            return visit(ctx.pair_elem());
        }
        System.out.println("Error");
        return visitChildren(ctx);
    }

    /**
     * {@inheritDoc}
     *
     * @author WangJiaYing & Jimmy
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitAssign_rhs(@NotNull WACCParser.Assign_rhsContext ctx) {
        if (ctx.CALL() != null) {
            List<AST.ExprNode> argNodeList = new ArrayList<>();
            if (ctx.arg_list() != null) {
                for (int i = 0; i < (ctx.arg_list().getChildCount() + 1) / 2; i++) {
                    argNodeList.add((AST.ExprNode) visit(ctx.arg_list().expr(i)));
                }
            }
            return ast.new CallNode((AST.IdentNode) visit(ctx.ident()), argNodeList);

        } else if (ctx.NEWPAIR() != null) {
            return ast.new NewPairNode((AST.ExprNode) visit(ctx.expr(0)), (AST.ExprNode) visit(ctx.expr(1)));

        } else if (ctx.pair_elem() != null) {
            return visit(ctx.pair_elem());

        } else if (ctx.array_liter() != null) {
            List<AST.ASTNode> exprNodeList = new ArrayList<>();
            for (int i = 0; i < ctx.array_liter().expr().size(); i++) {
                exprNodeList.add(visit(ctx.array_liter().expr(i)));
            }
            return ast.new Array_literNode(exprNodeList);
        } else if (ctx.expr(0) != null) {
            return visit(ctx.expr(0));
        }
        System.out.println("Error");
        return visitChildren(ctx);
    }

    /**
     * {@inheritDoc}
     *
     * @author YinJun
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitStat(@NotNull WACCParser.StatContext ctx) {
        //TODO symbol table config
        if (ctx.SEMICOLON() != null) {
            return ast.new MultipleStatNode((AST.StatNode) visit(ctx.stat(0)), (AST.StatNode) visit(ctx.stat(1)));
        } else if (ctx.BEGIN() != null) {
            return ast.new BeginNode((AST.StatNode) visit(ctx.stat(0)));
        } else if (ctx.DOAGAINWHILE() != null) {
            return ast.new MultipleStatNode((AST.StatNode) visit(ctx.stat(0)), ast.new WhileNode((AST.ExprNode) visit(ctx.expr()), (AST.StatNode) visit(ctx.stat(0))));
        } else if (ctx.WHILE() != null) {
            return ast.new WhileNode((AST.ExprNode) visit(ctx.expr()), (AST.StatNode) visit(ctx.stat(0)));
        } else if (ctx.IF() != null) {
            return ast.new IfNode((AST.ExprNode) visit(ctx.expr()), (AST.StatNode) visit(ctx.stat(0)), (AST.StatNode) visit(ctx.stat(1)));
        } else if (ctx.PRINTLN() != null) {
            return ast.new PrintlnNode((AST.ExprNode) visit(ctx.expr()));
        } else if (ctx.PRINT() != null) {
            return ast.new PrintNode((AST.ExprNode) visit(ctx.expr()));
        } else if (ctx.EXIT() != null) {
            return ast.new ExitNode((AST.ExprNode) visit(ctx.expr()));
        } else if (ctx.RETURN() != null) {
            return ast.new ReturnNode((AST.ExprNode) visit(ctx.expr()));
        } else if (ctx.FREE() != null) {
            return ast.new FreeNode((AST.ExprNode) visit(ctx.expr()));
        } else if (ctx.READ() != null) {
            return ast.new ReadNode(visit(ctx.assign_lhs()));
        } else if (ctx.SKIP() != null) {
            return ast.new SkipNode();
        } else if (ctx.assign_lhs() != null) {
            return ast.new AssignmentNode(visit(ctx.assign_lhs()), visit(ctx.assign_rhs()));
        } else if (ctx.type() != null) {
            AST.TypeNode typeNode = (AST.TypeNode) visit(ctx.type());
            AST.IdentNode identNode = (AST.IdentNode) visit(ctx.ident());
            AST.DeclarationNode declarationNode = ast.new DeclarationNode(typeNode, identNode, visit(ctx.assign_rhs()));
            return declarationNode;
        } else if (ctx.FOR() != null) {
            AST.TypeNode typeNode = (AST.TypeNode) visit(ctx.forcond().forass1().type());
            AST.IdentNode identNode = (AST.IdentNode) visit(ctx.forcond().forass1().ident());
            AST.DeclarationNode declarationNode = ast.new DeclarationNode(typeNode, identNode, visit(ctx.forcond().forass1().assign_rhs()));
            AST.AssignmentNode assignmentNode = ast.new AssignmentNode(visit(ctx.forcond().forass2().ident()), visit(ctx.forcond().forass2().assign_rhs()));
            AST.ForNode forNode = ast.new ForNode(declarationNode, visit(ctx.forcond().expr()), assignmentNode, visit(ctx.stat(0)));
            return forNode;
        } else if (ctx.BREAK() != null) {
            AST.BreakNode breakNode = ast.new BreakNode();
            return breakNode;
        } else if (ctx.CONTINUE() != null) {
            AST.ContinueNode continueNode = ast.new ContinueNode();
            return continueNode;
        }
        System.out.println("Error");
        return visitChildren(ctx);
    }

    /**
     * {@inheritDoc}
     *
     * @author YinJun
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitBool_liter(@NotNull WACCParser.Bool_literContext ctx) {

        return ast.new Bool_literNode(ctx.getText());
    }

    /**
     * {@inheritDoc}
     *
     * @author YinJun
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitParam_list(@NotNull WACCParser.Param_listContext ctx) {
        //TODO
        System.out.println("Not Implemented");
        return visitChildren(ctx);
    }

    /**
     * {@inheritDoc}
     *
     * @author WangJiaYing & Jimmy
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitDigit(@NotNull WACCParser.DigitContext ctx) {
        //TODO
        System.out.println("Error");
        return visitChildren(ctx);
    }

    /**
     * {@inheritDoc}
     *
     * @author WangJiaYing & Jimmy
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitArg_list(@NotNull WACCParser.Arg_listContext ctx) {

        //TODO
        System.out.println("Not Implemented");
        return visitChildren(ctx);
    }

    /**
     * {@inheritDoc}
     *
     * @author WangJiaYing & Jimmy
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitArray_elem(@NotNull WACCParser.Array_elemContext ctx) {

        List<AST.ExprNode> exprNodeList = new ArrayList<>();
        for (WACCParser.ExprContext exprContext : ctx.expr()) {
            exprNodeList.add((AST.ExprNode) visit(exprContext));
        }
        return ast.new Array_elemNode((AST.IdentNode) visit(ctx.ident()), exprNodeList);
    }

    /**
     * {@inheritDoc}
     *
     * @author WangJiaYing & Jimmy
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitPair_elem_type(@NotNull WACCParser.Pair_elem_typeContext ctx) {
        if (ctx.base_type() != null) {
            return visit(ctx.base_type());
        } else if (ctx.array_type() != null) {
            return visit(ctx.array_type());
        } else if (ctx.PAIR() != null) {
            return ast.new PairNode();
        }
        System.out.println("Error");
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @author WangJiaYing & Jimmy
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitChar_liter(@NotNull WACCParser.Char_literContext ctx) {

        return ast.new Char_literNode(ctx.CHAR_LITER().getText());
    }

    /**
     * {@inheritDoc}
     *
     * @author WangJiaYing & Jimmy
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitArray_liter(@NotNull WACCParser.Array_literContext ctx) {

        //TODO
        System.out.println("Not Implemented");
        return visitChildren(ctx);
    }


    /**
     * {@inheritDoc}
     *
     * @author WangJiaYing & Jimmy
     * <p>The default implementation returns the result of calling
     * {@link #visitChildren} on {@code ctx}.</p>
     */
    @Override
    public AST.ASTNode visitInt_liter(@NotNull WACCParser.Int_literContext ctx) {

        String sign = "+";
        String number = "";
        if (ctx.int_sign() != null) {
            sign = ctx.int_sign().getText();
        }
        if (ctx.INTEGER() != null) {
            number = ctx.INTEGER().getText();
        }
        return ast.new Int_literNode(sign, number);
    }
}