package WACC;
// import ANTLR's runtime libraries
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

// import antlr package (your code)
import antlr.*;

import java.io.PrintWriter;


public class Main {

    public static void main(String[] args) throws Exception {

        // create a CharStream that reads from standard input
        ANTLRInputStream input = new ANTLRFileStream(args[0]);

        // create a lexer that feeds off of input CharStream
        WACCLexer lexer = new WACCLexer(input);

        // create a buffer of tokens pulled from the lexer
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // create a parser that feeds off the tokens buffer
        WACCParser parser = new WACCParser(tokens);

        ParseTree tree = parser.program(); // begin parsing at program rule

        if(parser.getNumberOfSyntaxErrors() > 0) {
            System.out.println("#syntax_error#");
            System.exit(100);
        }

        // build and run my custom visitor
        TreeVisitor visitor = new TreeVisitor();

        AST.ASTNode astNode = visitor.visit(tree);

        astNode.check();
        System.out.println("Semantic check finished");

        String fileName = args[0].substring(args[0].lastIndexOf("/") + 1, args[0].length() - "WACC".length());

        AssemblyBuilder builder = new AssemblyBuilder(new StringBuilder(), new StringBuilder(),
                new StringBuilder(), new StringBuilder(), new StringBuilder());
        astNode.generate(builder);
        PrintWriter fileWriter = new PrintWriter(fileName + "s");

        fileWriter.println(builder.getHeader());
        fileWriter.println(builder.getFunction());
        fileWriter.println(builder.getMain());
        fileWriter.println(builder.getLabel());

        //test purpose only=======================
        PrintWriter testFileWriter = new PrintWriter("testFile.s");
        testFileWriter.println(builder.getHeader());
        testFileWriter.println(builder.getFunction());
        testFileWriter.println(builder.getMain());
        testFileWriter.println(builder.getLabel());
        testFileWriter.close();
        //===========================


        System.out.println(builder.getHeader());
        System.out.println();
        System.out.println(builder.getFunction());
        System.out.println();
        System.out.println(builder.getMain());
        System.out.println();
        System.out.println(builder.getLabel());

        fileWriter.close();
    }
}
