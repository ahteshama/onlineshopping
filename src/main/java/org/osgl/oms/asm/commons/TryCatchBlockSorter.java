/**
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 * <p/>
 * Redistribution and use in srccode and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of srccode code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the className of the copyright holders nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.osgl.oms.asm.commons;

import org.osgl.oms.asm.MethodVisitor;
import org.osgl.oms.asm.Opcodes;
import org.osgl.oms.asm.tree.MethodNode;
import org.osgl.oms.asm.tree.TryCatchBlockNode;

import java.util.Collections;
import java.util.Comparator;

/**
 * A {@link org.osgl.oms.asm.MethodVisitor} adapter to sort the exception handlers. The handlers
 * are sorted in a method innermost-to-outermost. This allows the programmer to
 * add handlers without worrying about ordering them correctly with respect to
 * existing, in-code handlers.
 *
 * Behavior is only defined for properly-nested handlers. If any "try" blocks
 * overlap (something that isn't possible in Java code) then this may not do
 * what you want. In fact, this adapter just sorts by the length of the "try"
 * block, taking advantage of the fact that a given try block must be larger
 * than any block it contains).
 *
 * @author Adrian Sampson
 */
public class TryCatchBlockSorter extends MethodNode {

    public TryCatchBlockSorter(final MethodVisitor mv, final int access,
                               final String name, final String desc, final String signature,
                               final String[] exceptions) {
        this(Opcodes.ASM5, mv, access, name, desc, signature, exceptions);
    }

    protected TryCatchBlockSorter(final int api, final MethodVisitor mv,
                                  final int access, final String name, final String desc,
                                  final String signature, final String[] exceptions) {
        super(api, access, name, desc, signature, exceptions);
        this.mv = mv;
    }

    @Override
    public void visitEnd() {
        // Compares TryCatchBlockNodes by the length of their "try" block.
        Comparator<TryCatchBlockNode> comp = new Comparator<TryCatchBlockNode>() {

            public int compare(TryCatchBlockNode t1, TryCatchBlockNode t2) {
                int len1 = blockLength(t1);
                int len2 = blockLength(t2);
                return len1 - len2;
            }

            private int blockLength(TryCatchBlockNode block) {
                int startidx = instructions.indexOf(block.start);
                int endidx = instructions.indexOf(block.end);
                return endidx - startidx;
            }
        };
        Collections.sort(tryCatchBlocks, comp);
        // Updates the 'target' of each try catch block annotation.
        for (int i = 0; i < tryCatchBlocks.size(); ++i) {
            tryCatchBlocks.get(i).updateIndex(i);
        }
        if (mv != null) {
            accept(mv);
        }
    }
}
