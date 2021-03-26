/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.utfsm.jena.arq.sparql.mgt;

import org.apache.jena.sparql.algebra.op.*;
import org.utfsm.utils.BTNode;

public interface OpVisitorTDB
{
    // Op0
    public BTNode visit(OpBGP opBGP) ;
    public BTNode visit(OpQuadPattern quadPattern) ;
    public BTNode visit(OpQuadBlock quadBlock) ;
    public BTNode visit(OpTriple opTriple) ;
    public BTNode visit(OpQuad opQuad) ;
    public BTNode visit(OpPath opPath) ;
    public BTNode visit(OpFind opFind) ;
    public BTNode visit(OpTable opTable) ;
    public BTNode visit(OpNull opNull) ;
    
    //Op1
    public BTNode visit(OpProcedure opProc) ;
    public BTNode visit(OpPropFunc opPropFunc) ;
    public BTNode visit(OpFilter opFilter) ;
    public BTNode visit(OpGraph opGraph) ;
    public BTNode visit(OpService opService) ;
    public BTNode visit(OpDatasetNames dsNames) ;
    public BTNode visit(OpLabel opLabel) ;
    public BTNode visit(OpAssign opAssign) ;
    public BTNode visit(OpExtend opExtend) ;
    
    // Op2
    public BTNode visit(OpJoin opJoin) ;
    public BTNode visit(OpLeftJoin opLeftJoin) ;
    public BTNode visit(OpUnion opUnion) ;
    public BTNode visit(OpDiff opDiff) ;
    public BTNode visit(OpMinus opMinus) ;
    public BTNode visit(OpConditional opCondition) ;
    
    // OpN
    public BTNode visit(OpSequence opSequence) ;
    public BTNode visit(OpDisjunction opDisjunction) ;

    public default BTNode visit(OpExt opExt) {
        return  new BTNode("OpExt");
    }
    
    // OpModifier
    public BTNode visit(OpList opList) ;
    public BTNode visit(OpOrder opOrder) ;
    public BTNode visit(OpProject opProject) ;
    public BTNode visit(OpReduced opReduced) ;
    public BTNode visit(OpDistinct opDistinct) ;
    public BTNode visit(OpSlice opSlice) ;

    public BTNode visit(OpGroup opGroup) ;
    public BTNode visit(OpTopN opTop) ;
}
