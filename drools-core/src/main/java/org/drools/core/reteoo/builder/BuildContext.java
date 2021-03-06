/*
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.core.reteoo.builder;

import org.drools.core.common.BaseNode;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.common.RuleBasePartitionId;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.impl.InternalKnowledgeBase;
import org.drools.core.reteoo.KieComponentFactory;
import org.drools.core.reteoo.LeftTupleSource;
import org.drools.core.reteoo.ObjectSource;
import org.drools.core.reteoo.ObjectTypeNode;
import org.drools.core.reteoo.ReteooBuilder;
import org.drools.core.rule.EntryPointId;
import org.drools.core.rule.GroupElement;
import org.drools.core.rule.Pattern;
import org.drools.core.rule.QueryImpl;
import org.drools.core.rule.RuleConditionElement;
import org.drools.core.spi.AlphaNodeFieldConstraint;
import org.drools.core.spi.BetaNodeFieldConstraint;
import org.drools.core.spi.RuleComponent;
import org.drools.core.time.TemporalDependencyMatrix;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

/**
 * A build context for Reteoo Builder
 */
public class BuildContext {

    // tuple source to attach next node to
    private LeftTupleSource                  tupleSource;
    // object source to attach next node to
    private ObjectSource                     objectSource;
    // object type cache to check for cross products
    private LinkedList<Pattern>              objectType;
    // offset of the pattern
    private int                              currentPatternOffset;
    // rule base to add rules to
    private InternalKnowledgeBase            kBase;
    // rule being added at this moment
    private RuleImpl                         rule;
    private GroupElement                     subRule;
    // the rule component being processed at the moment
    private Stack<RuleComponent>             ruleComponent;
    // working memories attached to the given rulebase
    private InternalWorkingMemory[]          workingMemories;
    // id generator
    private ReteooBuilder.IdGenerator        idGenerator;
    // a build stack to track nested elements
    private LinkedList<RuleConditionElement> buildstack;
    // beta constraints from the last pattern attached
    private List<BetaNodeFieldConstraint>    betaconstraints;
    // alpha constraints from the last pattern attached
    private List<AlphaNodeFieldConstraint>   alphaConstraints;
    // the current entry point
    private EntryPointId                       currentEntryPoint;
    private boolean                          tupleMemoryEnabled;
    private boolean                          objectTypeNodeMemoryEnabled;
    private boolean                          query;
    /**
     * Stores the list of nodes being added that require partitionIds
     */
    private List<BaseNode>                   nodes;
    /**
     * Stores the id of the partition this rule will be added to
     */
    private RuleBasePartitionId              partitionId;
    /**
     * the calculate temporal distance matrix
     */
    private TemporalDependencyMatrix         temporal;
    private ObjectTypeNode                   rootObjectTypeNode;
    private Pattern[]                        lastBuiltPatterns;
    // The reason why this is here is because forall can inject a
    //  "this == " + BASE_IDENTIFIER $__forallBaseIdentifier
    // Which we don't want to actually count in the case of forall node linking    
    private boolean                          emptyForAllBetaConstraints;
    private KieComponentFactory              componentFactory;
    private boolean                          attachPQN;

    public BuildContext(final InternalKnowledgeBase kBase,
                        final ReteooBuilder.IdGenerator idGenerator) {
        this.kBase = kBase;

        this.idGenerator = idGenerator;

        this.workingMemories = null;

        this.objectType = null;
        this.buildstack = null;

        this.tupleSource = null;
        this.objectSource = null;

        this.currentPatternOffset = 0;

        this.tupleMemoryEnabled = true;

        this.objectTypeNodeMemoryEnabled = true;

        this.currentEntryPoint = EntryPointId.DEFAULT;

        this.nodes = new LinkedList<BaseNode>();

        this.partitionId = null;

        this.ruleComponent = new Stack<RuleComponent>();

        this.attachPQN = true;

        this.componentFactory = kBase.getConfiguration().getComponentFactory();

        this.emptyForAllBetaConstraints = false;
    }

    public boolean isEmptyForAllBetaConstraints() {
        return emptyForAllBetaConstraints;
    }

    public void setEmptyForAllBetaConstraints(boolean emptyForAllBetaConstraints) {
        this.emptyForAllBetaConstraints = emptyForAllBetaConstraints;
    }

    /**
     * @return the currentPatternOffset
     */
    public int getCurrentPatternOffset() {
        return this.currentPatternOffset;
    }

    /**
     * @param currentPatternIndex the currentPatternOffset to set
     */
    public void setCurrentPatternOffset(final int currentPatternIndex) {
        this.currentPatternOffset = currentPatternIndex;
        this.syncObjectTypesWithPatternOffset();
    }

    public void syncObjectTypesWithPatternOffset() {
        if (this.objectType == null) {
            this.objectType = new LinkedList<Pattern>();
        }
        while (this.objectType.size() > this.currentPatternOffset) {
            this.objectType.removeLast();
        }
    }

    /**
     * @return the objectSource
     */
    public ObjectSource getObjectSource() {
        return this.objectSource;
    }

    /**
     * @param objectSource the objectSource to set
     */
    public void setObjectSource(final ObjectSource objectSource) {
        this.objectSource = objectSource;
    }

    /**
     * @return the objectType
     */
    public LinkedList<Pattern> getObjectType() {
        if (this.objectType == null) {
            this.objectType = new LinkedList<Pattern>();
        }
        return this.objectType;
    }

    /**
     * @param objectType the objectType to set
     */
    public void setObjectType(final LinkedList<Pattern> objectType) {
        if (this.objectType == null) {
            this.objectType = new LinkedList<Pattern>();
        }
        this.objectType = objectType;
    }

    /**
     * @return the tupleSource
     */
    public LeftTupleSource getTupleSource() {
        return this.tupleSource;
    }

    /**
     * @param tupleSource the tupleSource to set
     */
    public void setTupleSource(final LeftTupleSource tupleSource) {
        this.tupleSource = tupleSource;
    }

    public void incrementCurrentPatternOffset() {
        this.currentPatternOffset++;
    }

    public void decrementCurrentPatternOffset() {
        this.currentPatternOffset--;
        this.syncObjectTypesWithPatternOffset();
    }

    /**
     * Returns context rulebase
     *
     * @return
     */
    public InternalKnowledgeBase getKnowledgeBase() {
        return this.kBase;
    }

    /**
     * Return the array of working memories associated with the given
     * rulebase.
     *
     * @return
     */
    public InternalWorkingMemory[] getWorkingMemories() {
        if (this.workingMemories == null) {
            this.workingMemories = this.kBase.getWorkingMemories();
        }
        return this.workingMemories;
    }

    /**
     * Returns an Id for the next node
     *
     * @return
     */
    public int getNextId() {
        return this.idGenerator.getNextId();
    }

    /**
     * Method used to undo previous id assignment
     */
    public void releaseId(int id) {
        this.idGenerator.releaseId(id);
    }

    /**
     * Adds the rce to the build stack
     *
     * @param rce
     */
    public void push(final RuleConditionElement rce) {
        if (this.buildstack == null) {
            this.buildstack = new LinkedList<RuleConditionElement>();
        }
        this.buildstack.addLast(rce);
    }

    /**
     * Removes the top stack element
     *
     * @return
     */
    public RuleConditionElement pop() {
        if (this.buildstack == null) {
            this.buildstack = new LinkedList<RuleConditionElement>();
        }
        return this.buildstack.removeLast();
    }

    /**
     * Returns the top stack element without removing it
     *
     * @return
     */
    public RuleConditionElement peek() {
        if (this.buildstack == null) {
            this.buildstack = new LinkedList<RuleConditionElement>();
        }
        return this.buildstack.getLast();
    }

    /**
     * Returns a list iterator to iterate over the stacked elements
     *
     * @return
     */
    public ListIterator<RuleConditionElement> stackIterator() {
        if (this.buildstack == null) {
            this.buildstack = new LinkedList<RuleConditionElement>();
        }
        return this.buildstack.listIterator(this.buildstack.size());
    }

    /**
     * @return the betaconstraints
     */
    public List<BetaNodeFieldConstraint> getBetaconstraints() {
        return this.betaconstraints;
    }

    /**
     * @param betaconstraints the betaconstraints to set
     */
    public void setBetaconstraints(final List<BetaNodeFieldConstraint> betaconstraints) {
        this.betaconstraints = betaconstraints;
    }

    /**
     * @return
     */
    public List<AlphaNodeFieldConstraint> getAlphaConstraints() {
        return alphaConstraints;
    }

    public void setAlphaConstraints(List<AlphaNodeFieldConstraint> alphaConstraints) {
        this.alphaConstraints = alphaConstraints;
    }

    public boolean isTupleMemoryEnabled() {
        return this.tupleMemoryEnabled;
    }

    public void setTupleMemoryEnabled(boolean hasLeftMemory) {
        this.tupleMemoryEnabled = hasLeftMemory;
    }

    public boolean isObjectTypeNodeMemoryEnabled() {
        return objectTypeNodeMemoryEnabled;
    }

    public void setObjectTypeNodeMemoryEnabled(boolean hasObjectTypeMemory) {
        this.objectTypeNodeMemoryEnabled = hasObjectTypeMemory;
    }

    public boolean isQuery() {
        return query;
    }

    /**
     * @return the currentEntryPoint
     */
    public EntryPointId getCurrentEntryPoint() {
        return currentEntryPoint;
    }

    /**
     * @param currentEntryPoint the currentEntryPoint to set
     */
    public void setCurrentEntryPoint(EntryPointId currentEntryPoint) {
        this.currentEntryPoint = currentEntryPoint;
    }

    /**
     * @return the nodes
     */
    public List<BaseNode> getNodes() {
        return nodes;
    }

    /**
     * @param nodes the nodes to set
     */
    public void setNodes(List<BaseNode> nodes) {
        this.nodes = nodes;
    }

    /**
     * @return the partitionId
     */
    public RuleBasePartitionId getPartitionId() {
        return partitionId;
    }

    /**
     * @param partitionId the partitionId to set
     */
    public void setPartitionId(RuleBasePartitionId partitionId) {
        this.partitionId = partitionId;
    }

    public boolean isStreamMode() {
        return this.temporal != null;
    }

    public TemporalDependencyMatrix getTemporalDistance() {
        return this.temporal;
    }

    public void setTemporalDistance(TemporalDependencyMatrix temporal) {
        this.temporal = temporal;
    }

    public LinkedList<RuleConditionElement> getBuildStack() {
        return this.buildstack;
    }

    public RuleImpl getRule() {
        return rule;
    }

    public void setRule(RuleImpl rule) {
        this.rule = rule;
        if (rule.isQuery()) {
            this.query = true;
        }
    }

    public GroupElement getSubRule() {
        return subRule;
    }

    public void setSubRule(GroupElement subRule) {
        this.subRule = subRule;
    }

    /**
     * Removes the top element from the rule component stack.
     * The rule component stack is used to add trackability to
     * the ReteOO nodes so that they can be linked to the rule
     * components that originated them.
     *
     * @return
     */
    public RuleComponent popRuleComponent() {
        return this.ruleComponent.pop();
    }

    /**
     * Peeks at the top element from the rule component stack.
     * The rule component stack is used to add trackability to
     * the ReteOO nodes so that they can be linked to the rule
     * components that originated them.
     *
     * @return
     */
    public RuleComponent peekRuleComponent() {
        return this.ruleComponent.isEmpty() ? null : this.ruleComponent.peek();
    }

    /**
     * Adds the ruleComponent to the top of the rule component stack.
     * The rule component stack is used to add trackability to
     * the ReteOO nodes so that they can be linked to the rule
     * components that originated them.
     *
     * @return
     */
    public void pushRuleComponent(RuleComponent ruleComponent) {
        this.ruleComponent.push(ruleComponent);
    }

    public ObjectTypeNode getRootObjectTypeNode() {
        return rootObjectTypeNode;
    }

    public void setRootObjectTypeNode(ObjectTypeNode source) {
        rootObjectTypeNode = source;
    }

    public Pattern[] getLastBuiltPatterns() {
        return lastBuiltPatterns;
    }

    public void setLastBuiltPattern(Pattern lastBuiltPattern) {
        if (this.lastBuiltPatterns == null) {
            this.lastBuiltPatterns = new Pattern[]{lastBuiltPattern, null};
        } else {
            this.lastBuiltPatterns[1] = this.lastBuiltPatterns[0];
            this.lastBuiltPatterns[0] = lastBuiltPattern;
        }
    }

    public boolean isAttachPQN() {
        return attachPQN;
    }

    public void setAttachPQN(final boolean attachPQN) {
        this.attachPQN = attachPQN;
    }

    public KieComponentFactory getComponentFactory() {
        return componentFactory;
    }

    public void setComponentFactory(KieComponentFactory componentFactory) {
        this.componentFactory = componentFactory;
    }

}
