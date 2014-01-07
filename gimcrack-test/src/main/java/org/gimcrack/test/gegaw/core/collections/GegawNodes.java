package org.gimcrack.test.gegaw.core.collections;

import java.util.*;

import org.gimcrack.test.gegaw.core.Gegaw;
import org.gimcrack.test.gegaw.core.GegawContext;
import org.gimcrack.test.gegaw.core.GegawNode;
import org.gimcrack.test.gegaw.core.InternalGegawContext;
import org.gimcrack.test.gegaw.core.nodes.InternalStartNode;

/**
 * This class is NOT thread-safe!
 */
public class GegawNodes {

    private int size = 0;

    private GegawNodeHolder current;
    private final GegawNodeHolder start;

    private transient Map<GegawNode, GegawNodeHolder> nodes = new HashMap<GegawNode, GegawNodeHolder>();

    public GegawNodes() {
        current = new GegawNodeHolder(new InternalStartNode());
        nodes.put(current.node, current);
        start = current;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean contains(Object o) {
        if (!(o instanceof GegawNode)) {
            return false;
        }
        return nodes.containsKey((GegawNode) o);
    }

    public void addBranch(GegawNode node) {
        GegawNodeHolder newBranchHolder = null;
        if (!nodes.keySet().contains(node)) {
            ++size;
            newBranchHolder = new GegawNodeHolder(current, node);
        } else {
            newBranchHolder = nodes.get(node);
        }
        current.next(newBranchHolder);
    }

    public void add(GegawNode node) {
        GegawNodeHolder nodeHolder = getOrCreateNodeHolder(node, true);
        GegawNodeHolder next = current.next(nodeHolder);
        current = next;
    }

    public void prepend(GegawNode node) {
        GegawNodeHolder nodeHolder = getOrCreateNodeHolder(node, false);
        current = current.prev(nodeHolder);
    }
    
    private GegawNodeHolder getOrCreateNodeHolder(GegawNode node, boolean nodeHolderIsNext) { 
        GegawNodeHolder nodeHolder = null;
        if (!nodes.keySet().contains(node)) {
            if( nodeHolderIsNext ) { 
                nodeHolder = new GegawNodeHolder(current, node);
            } else { 
                nodeHolder = new GegawNodeHolder(node, current);
            }
            nodes.put(node, nodeHolder);
            ++size;
        } else {
            nodeHolder = nodes.get(node);
        }
        return nodeHolder;
    }

    public void addAll(Collection<? extends GegawNode> c) {
        throw new UnsupportedOperationException(Gegaw.unsupported(c));
    }

    public void setCurrentTo(GegawNode node) {
        GegawNodeHolder nodeHolder = nodes.get(node);
        if (nodeHolder != null) {
            current = nodeHolder;
        } else {

        }
    }

    public GegawNode getStartNode() {
        return start.node;
    }

    public GegawNode[] getBranches(GegawNode node) {
        GegawNodeHolder nodeHolder = nodes.get(node);
        GegawNodeHolder[] branchHolders = nodeHolder.getBranches();
        int length = 0;
        if( branchHolders != null ) { 
            length = branchHolders.length;
        }
        GegawNode[] branchNodes = new GegawNode[length];
        for (int i = 0; i < branchNodes.length; ++i) {
            branchNodes[i] = branchHolders[i].node;
        }
        return branchNodes;
    }

}
