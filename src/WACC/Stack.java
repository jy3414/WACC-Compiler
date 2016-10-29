package WACC;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zl4214 on 01/12/15.
 */
public class Stack {

    /*
     * size is the total size of stack in the whole program generated.
     */
    private int size;

    public static final int MAX_STACK_SIZE = 1024;

    private List<StackElem> stack = new ArrayList<>();

    /*create a stackElem using ident and sizeOfType and add this elem to
     the list of stackElem
     */

    public void add(String ident, int sizeOfType) {
        StackElem stackElem = new StackElem(ident, sizeOfType);
        stack.add(stackElem);
        size += stackElem.getSize();
        stackElem.setOffset(size);
    }

    /*This method calculates the stack offset of this elem from the
    stack pointer
     */
    public int getStackElemOffset(String ident) {
        int offset = 0;
        for(StackElem stackElem : getStackElemList()) {
            if (ident.equals(stackElem.getIdent())) {
                offset = size - stackElem.getOffset();
                break;
            }
        }

        return offset;
    }


    public int getSize() {
        return size;
    }

    public void incSize(int sizeOfType) {
        size += sizeOfType;
    }

    public void decSize(int sizeOfType) {
        size -= sizeOfType;
    }

    public List<StackElem> getStackElemList() {
        return stack;
    }



    /*
     * The elem on the stack.
     */
    private class StackElem {

        private String ident;
        private int sizeOfType;
        private int offset;


        private StackElem(String ident, int sizeOfType) {
            this.ident = ident;
            this.sizeOfType = sizeOfType;
        }

        public int getSize() {
            return sizeOfType;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }

        public int getOffset() {
            return offset;
        }

        public String getIdent() {
            return ident;
        }
    }
}