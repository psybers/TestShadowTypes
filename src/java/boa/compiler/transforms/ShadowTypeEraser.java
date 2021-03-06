/*
 * Copyright 2017, Hridesh Rajan, Robert Dyer, Kaushik Nimmala
 *                 Iowa State University of Science and Technology
 *                 and Bowling Green State University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package boa.compiler.transforms;

import java.util.*;

import boa.compiler.ast.Factor;
import boa.compiler.ast.Selector;
import boa.compiler.ast.Term;
import boa.compiler.ast.Node;
import boa.compiler.ast.Component;
import boa.compiler.ast.Identifier;
import boa.compiler.ast.Conjunction;
import boa.compiler.ast.Call;
import boa.compiler.ast.expressions.*;
import boa.compiler.ast.statements.VarDeclStatement;
import boa.compiler.ast.statements.Statement;
import boa.compiler.ast.statements.VisitStatement;
import boa.compiler.ast.expressions.VisitorExpression;
import boa.compiler.ast.statements.Block;
import boa.compiler.ast.statements.SwitchStatement;
import boa.compiler.SymbolTable;
import boa.compiler.visitors.AbstractVisitorNoArg;
import boa.types.BoaShadowType;
import boa.types.BoaTuple;
import boa.types.proto.StatementProtoTuple;
import boa.types.proto.enums.StatementKindProtoMap;

import boa.compiler.transforms.ASTFactory;

/**
 * Converts a tree using shadow types into a tree without shadow types.
 *
 * Algorithm:-
 * 1) Find each instance of VisitorExpression, then for each:
 *   a) For each VisitStatement that is a ShadowType:
 *       i) Replace identifier to erase the shadow type
 *      ii) Find each use of identifier, replace subtree
 *     iii) Remove the VisitStatement from the VisitorExpression block statement and place it in a list
 *      iv) If VisitorExpression has no VisitStatement for the shadowed type, create an empty one - otherwise select it
 *       v) Insert into the shadowed type's VisitStatement a SwitchStatement sub tree
 *      vi) For all VisitStatement's in the list, create a CaseStatement in the above created SwitchStatement, with the case value being the shadowed type
 *     vii) If there is a wildcard and we created a visit in step iv, then add the wildcard's body as the default case in the SwitchStatement
 * @author rdyer
 * @author kaushin
 */
public class ShadowTypeEraser extends AbstractVisitorNoArg {
		
	@Override
	public void start(final Node n) {
		//FIRST STEP TO COLLECT AND TRANSFORM ALL VISITSTATEMENTS
		new VisitorReplace().start(n);

		//SECOND STEP TO TRANSFORM ALL THE SUB TREES
		new SubtreeEraser().start(n);

		
	}


	public class VisitorReplace  extends AbstractVisitorNoArg{

		private LinkedList<VisitStatement> visitStack = new LinkedList<VisitStatement>();
		private LinkedList<VisitStatement> shadowVisitStack = new LinkedList<VisitStatement>();
		private LinkedList<VisitorExpression> visitorExpStack = new LinkedList<VisitorExpression>();
		private HashMap<VisitStatement,String> shadowedMap = new HashMap<VisitStatement,String>();
        private HashMap<VisitStatement,String> statementShadowedVisit = new HashMap<VisitStatement,String>();
		private boolean shadowedTypePresent = false;

		@Override
		public void visit(final VisitStatement n) {
			visitStack.push(n);
			super.visit(n);
			visitStack.pop();
		}

		@Override
		public void visit(final VisitorExpression n) {
			visitorExpStack.push(n);
			super.visit(n);
			visitorExpStack.pop();
			// Remove the shadow type visit statements from the VisitorExpression Block.
			if(!shadowVisitStack.isEmpty()){
				n.replaceVisit(shadowVisitStack);
			}

			Block afterTransformation = new Block();
			
            


            //TODO : Create a Visit Statement of the shadowed type and attach the block to it
            if(shadowedMap.containsValue("Statement") && !shadowedTypePresent ){
                //Creating Switch statement
                Factor f = new Factor(ASTFactory.createIdentifier("name", n.env));
                
                Selector selec = new Selector(ASTFactory.createIdentifier("kind", n.env));
                f.addOp(selec);
                f.env = n.env;
                Expression exp = ASTFactory.createFactorExpr(f);
                exp.type = new StatementKindProtoMap();

                SwitchStatement switchS = new SwitchStatement(exp);

                afterTransformation.addStatement(switchS);
                //creating new shadowed visit
                VisitStatement shadowedTypeVisit = new VisitStatement(false, new Component( ASTFactory.createIdentifier("node", n.env), ASTFactory.createIdentifier("Statement", n.env)), afterTransformation);
                shadowedTypeVisit.type =  new StatementProtoTuple();
                shadowedTypeVisit.env = n.env;
                n.getBody().addStatement(shadowedTypeVisit);

                


                //TODO : Convert all items in shadowVisitStack into relevent swtich case block and add them to a Block object of required type
                 for(VisitStatement visit : shadowVisitStack){
                    //TODO : transform
                    //TODO : For each shadowded type create  a visit statement
                    
                   
                    //ADD transformed to the switch object
                    //switchS.addCase()

                }
            }else if(shadowedMap.containsValue("Statement") && shadowedTypePresent) {
                //TODO : need to add check to see if swtich already exists
                //creating switch statement
                Factor f = new Factor(ASTFactory.createIdentifier(statementShadowedVisit.values().toArray(new String[0])[0], n.env));
               
                Selector selec = new Selector(ASTFactory.createIdentifier("kind", n.env));
                f.addOp(selec);
                f.env = n.env;
                Expression exp = ASTFactory.createFactorExpr(f);
                exp.type = new StatementKindProtoMap();

                SwitchStatement switchS = new SwitchStatement(exp);                

                //need to add to the body of already existing Statement visit
                Block b = statementShadowedVisit.keySet().toArray(new VisitStatement[0])[0].getBody();

                
                // b.addStatement(swtichS);

                 //TODO : Convert all items in shadowVisitStack into relevent swtich case block and add them to a Block object of required type
                for(VisitStatement visit : shadowVisitStack){
                     //TODO : transform
                    //TODO : For each shadowded type create  a visit statement
                   
                    //b.addStatement(visit);
                 
                 }
            }

			

			
			
		/* This is the block thats needs to be generated (if there was only 1 shadow type)

			VisitStatement
		        Component
		            Identifier
		            Identifier
		        Block
		            SwitchStatement
		                Expression
		                    Conjunction
		                        Comparison
		                            SimpleExpr
		                                Term
		                                    Factor
		                                        Identifier
		                                        Selector
		                                            Identifier
		                SwitchCase
		                    Expression
		                        Conjunction
		                            Comparison
		                                SimpleExpr
		                                    Term
		                                        Factor
		                                            Identifier
		                                            Selector
		                                                Identifier
			*/

			
			shadowVisitStack = new LinkedList<VisitStatement>();
			shadowedTypePresent = false;
		}


		@Override
		public void visit(final Component n) {
			super.visit(n);
			// TODO : add field to strack presence of shadowed type (eg. Statement)
			
			if(n.type.toString().equals("Statement")){// will need to extend to multiple types apart from Statement
				shadowedTypePresent = true;
                statementShadowedVisit.put(visitStack.peek(),n.getIdentifier().getToken());

			}

			if (n.type instanceof BoaShadowType) {
				//get parent visit statement
				VisitStatement parentVisit = visitStack.peek(); 
				shadowVisitStack.push(parentVisit);

				final BoaShadowType shadow = (BoaShadowType)n.type;
				shadowedMap.put(parentVisit,shadow.shadowedName());
			}
		}


	}

	public class SubtreeEraser extends AbstractVisitorNoArg{
		private LinkedList<Expression> expressionStack = new LinkedList<Expression>();
		private boolean flag = false;
		private List<Node> ops = null;

		// track nearest Expression node
		public void visit(final Expression n) {
			expressionStack.push(n);
			super.visit(n);
			expressionStack.pop();
		}

		@Override
		public void visit(final Factor n) {
			flag = false;
			ops = n.getOps();
			super.visit(n);
		}

		// replacing shadow type selectors
		@Override
		public void visit(final Selector n) {
			super.visit(n);

			final Factor fact = (Factor)n.getParent();

			if (!flag && fact.getOperand().type instanceof BoaShadowType) {
				// avoid replacing past the first selector
				flag = true;
				final Expression parentExp = expressionStack.peek();

				// get shadow type used
				final Identifier id = (Identifier)fact.getOperand();
				final BoaShadowType shadow = (BoaShadowType)fact.getOperand().type;

				// replace the selector
				final Expression replacement = (Expression)shadow.lookupCodegen(n.getId().getToken(), id.getToken(), parentExp.env);
				final ParenExpression paren = new ParenExpression(replacement);
				final Factor newFact = new Factor(paren);
				final Expression newExp = ASTFactory.createFactorExpr(newFact);

				if (ops != null)
					for (int i = 1; i < ops.size(); i++)
						newFact.addOp(ops.get(i));

				newFact.env = parentExp.env;
				paren.type = replacement.type;
				newExp.type = paren.type;

				parentExp.replaceExpression(parentExp, newExp);
			}
		}

		// removing shadow types in before/after visit
		@Override
		public void visit(final Component n) {
			super.visit(n);
			if (n.type instanceof BoaShadowType) {
				final BoaShadowType shadow = (BoaShadowType)n.type;

				// change the identifier
				final Identifier id = (Identifier)n.getType();
				id.setToken(shadow.shadowedName());

				// update types
				n.type = n.getType().type = shadow.shadowedType;
				n.env.set(n.getIdentifier().getToken(), n.type);
			}
		}

		// removing shadow types in variable declarations
		@Override
		public void visit(final VarDeclStatement n) {
			super.visit(n);

			if (n.hasType()) {
				if (n.type instanceof BoaShadowType) {
					final BoaShadowType shadow = (BoaShadowType)n.env.get(n.getType().toString());

					// change the identifier
					final Identifier id = (Identifier)n.getType();
					id.setToken(shadow.shadowedName());

					// update types
					n.type = shadow.shadowedType;
					n.env.setType(n.getId().getToken(), shadow.shadowedType);
				}
			}
		}
	}
}
