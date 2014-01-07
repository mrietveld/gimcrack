package org.gimcrack.test.gegaw.core.collections;

import java.util.Arrays;

import org.gimcrack.test.gegaw.core.GegawNode;

public class GegawNodeHolder {

    public final GegawNode node;
    
    private GegawNodeHolder [] prev;
    private GegawNodeHolder [] next;
    
    public GegawNodeHolder(GegawNode node) { 
        this.node = node;
        this.prev = this.next = null;
    }
    
    public GegawNodeHolder(GegawNodeHolder prev, GegawNode node) { 
        this.node = node;
        this.prev = new GegawNodeHolder[1];
        this.prev[0] = prev;
        this.next = null;
    }
    
    public GegawNodeHolder(GegawNode node, GegawNodeHolder next) { 
        this.node = node;
        this.next = new GegawNodeHolder[1];
        this.next[0] = next;
        this.prev = null;
    }
    
    public GegawNodeHolder next(GegawNodeHolder next) { 
        // extend list of next
        int last = 0;
        if( this.next == null ) { 
            this.next = new GegawNodeHolder[1];
        } else { 
            GegawNodeHolder [] oldNext = this.next;
            this.next = Arrays.copyOf(oldNext, oldNext.length+1);
            last = oldNext.length;
        }
        
        // add node to new last next
        this.next[last] = next;
        
        // return new node holder
        return this.next[last];
    }

    public GegawNodeHolder prev(GegawNodeHolder prev) { 
        // extend list of prev
        int last = 0;
        if( this.prev == null ) { 
            this.prev = new GegawNodeHolder[1];
        } else { 
            GegawNodeHolder [] oldPrev = this.prev;
            this.prev = Arrays.copyOf(oldPrev, oldPrev.length+1);
            last = oldPrev.length;
        }
        // add node to new last prev
        this.prev[last] = prev;
        
        // return new node holder
        return this.prev[last];
    }
    
    public GegawNodeHolder [] getBranches() { 
        return next;
    }
    
}
