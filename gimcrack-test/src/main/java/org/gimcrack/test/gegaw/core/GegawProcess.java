package org.gimcrack.test.gegaw.core;

import java.util.LinkedHashSet;
import java.util.Set;

import org.gimcrack.test.gegaw.core.collections.GegawNodes;


public class GegawProcess {

    public GegawNodes nodes;

    public GegawProcess() { 
        this.nodes = new GegawNodes();
    }
    
    public void start(GegawContext context) {
        GegawNode startNode = nodes.getStartNode();
        context.initialize();
        traverse(startNode, context);
    }

    public GegawContext traverse(GegawNode node, GegawContext context) {
        StackEntry stack = new StackEntry(node, 0, null);
        int branch = 0;
        
        while (true) {
            // preorder
            if (branch == 0) {
                context.setVisited(node);
                context.execute(node);
            }
            
            // go through available branches
            GegawNode [] branches = nodes.getBranches(node);
            int numBranches = node.getNumBranches(branches, context);
            for (; branch < numBranches; branch++) {
                GegawNode branchNode = node.getBranch(branches, context, branch);
                if (context.isVisited(branchNode)) {
                    context.skipBranch(node, branch);
                } else {
                    stack = context.push(stack, node, branch + 1, branchNode, numBranches);
                    context.beforeBranch(node, branch);
                    node = branchNode;
                    branch = 0;
                    break;
                }
            }
            
            // when done with branches, proceed to next
            if (branch == numBranches) {
                context.postorder(node);
                node = stack.node;
                branch = stack.branch;
                stack = stack.pop();
                context.afterBranch(node, branch - 1);
                if (stack == null) {
                    return context;
                }
            }
        }
    }

    private int getNumBranches(GegawNode node) {
        return nodes.getBranches(node).length;
    }

}