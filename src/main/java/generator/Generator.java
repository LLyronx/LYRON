package generator;

import exception.PLDLAnalysisException;
import exception.PLDLParsingException;
import lexer.Lexer;
import lexer.NFA;
import lexer.SimpleREApply;
import parser.AnalysisNode;
import parser.AnalysisTree;
import parser.CFG;
import parser.CFGProduction;
import symbol.AbstractSymbol;
import symbol.Symbol;
import symbol.SymbolPool;
import symbol.Terminator;
import translator.MovementCreator;

import java.util.*;

public class Generator implements MovementCreator {

    private CFG cfg = null;

    private Set<Character> emptyChars = new HashSet<>();

    private Map<CFGProduction, List<AnalysisTree>> beforeMovementsMap = new HashMap<>();
    private Map<CFGProduction, List<AnalysisTree>> afterMovementsMap = new HashMap<>();

    private Lexer lexer;

    public Generator() throws PLDLParsingException, PLDLAnalysisException {
        List<Map.Entry<String, NFA>> terminatorsNFA = new ArrayList<>();
        terminatorsNFA.add(new AbstractMap.SimpleEntry<>("$$", NFA.fastNFA("$$")));
        terminatorsNFA.add(new AbstractMap.SimpleEntry<>("$", NFA.fastNFA("$")));
        terminatorsNFA.add(new AbstractMap.SimpleEntry<>("(", NFA.fastNFA("(")));
        terminatorsNFA.add(new AbstractMap.SimpleEntry<>(")", NFA.fastNFA(")")));
        terminatorsNFA.add(new AbstractMap.SimpleEntry<>(",", NFA.fastNFA(",")));
        terminatorsNFA.add(new AbstractMap.SimpleEntry<>("print", NFA.fastNFA("print")));
        terminatorsNFA.add(new AbstractMap.SimpleEntry<>("gen", NFA.fastNFA("gen")));
        terminatorsNFA.add(new AbstractMap.SimpleEntry<>("_", NFA.fastNFA("_")));
        terminatorsNFA.add(new AbstractMap.SimpleEntry<>("val", new SimpleREApply("[a-zA-Z][a-zA-Z0-9]*").getNFA()));
        terminatorsNFA.add(new AbstractMap.SimpleEntry<>("num", new SimpleREApply("[1-9][0-9]*|0").getNFA()));

        lexer = new Lexer(terminatorsNFA, null);
        emptyChars.add(' ');
        emptyChars.add('\t');
        emptyChars.add('\n');
        emptyChars.add('\r');
        emptyChars.add('\f');
        setCFG();
    }

    public AnalysisTree getMovementTree(String str) throws PLDLAnalysisException, PLDLParsingException {
        List<Symbol> symbols = lexer.analysis(str, emptyChars);
        return cfg.getTable().getAnalysisTree(symbols);
    }


    protected void setCFG() {
        Set<String> terminatorStrs = new HashSet<>(Arrays.asList("$$", "$", "(", ")", ",", "val", "num", "_", "print", "gen", "checkvar"));
        Set<String> unterminatorStrs = new HashSet<>(Arrays.asList("F", "G", "H", "Var", "E", "L", "L_"));
        SymbolPool pool = new SymbolPool();
        try {
            pool.initTerminatorString(terminatorStrs);
            pool.initUnterminatorString(unterminatorStrs);
            List<CFGProduction> res = new ArrayList<>(Arrays.asList(
                    new GenerateProduction(CFGProduction.getCFGProductionFromCFGString("G -> Var", pool)) {

                        @Override
                        public void doMovement(AnalysisNode movementTree, AnalysisNode analysisTree, ResultTuple4 resultCOMM) {
                            movementTree.getValue().setProperties(new HashMap<>());
                            for (String str : movementTree.getChildren().get(0).getValue().getProperties().keySet()) {
                                movementTree.getValue().getProperties().put(str, movementTree.getChildren().get(0).getValue().getProperties().get(str));
                            }
                        }
                    },
                    new GenerateProduction(CFGProduction.getCFGProductionFromCFGString("E -> print ( H )", pool)) {

                        /* For Debug */
                        @Override
                        public void doMovement(AnalysisNode movementTree, AnalysisNode analysisTree, ResultTuple4 resultCOMM) {
                            AnalysisNode rightTreeNode = (AnalysisNode) movementTree.getChildren().get(2).getValue().getProperties().get("rightTreeNode");
                            Symbol rightTreeNodeValue = rightTreeNode.getValue();
                            String name = (String) movementTree.getChildren().get(2).getValue().getProperties().get("name");
                            System.out.println(rightTreeNodeValue.getProperties().get(name));
                        }
                    },
                    new GenerateProduction(CFGProduction.getCFGProductionFromCFGString("E -> gen ( val , L_ , L_ , L_ )", pool)) {

                        @Override
                        public void doMovement(AnalysisNode movementTree, AnalysisNode analysisTree, ResultTuple4 resultCOMM) {
                            /*
                                0： gen
                                1: (
                                2: op
                                3: ,
                                4: L
                                5: ,
                                6: L
                                7: ,
                                8: L
                                9: )
                             */
                            String val1 = (String) movementTree.getChildren().get(2).getValue().getProperties().get("val");
                            String val2 = (String) movementTree.getChildren().get(4).getValue().getProperties().get("val");
                            String val3 = (String) movementTree.getChildren().get(6).getValue().getProperties().get("val");
                            String val4 = (String) movementTree.getChildren().get(8).getValue().getProperties().get("val");
                            resultCOMM.append(val1, val2, val3, val4);
                        }
                    },
                    new GenerateProduction(CFGProduction.getCFGProductionFromCFGString("H -> Var ( val )", pool)) {

                        @Override
                        public void doMovement(AnalysisNode movementTree, AnalysisNode analysisTree, ResultTuple4 resultCOMM) {
                            String name = (String) movementTree.getChildren().get(2).getValue().getProperties().get("val");
                            AnalysisNode rightTreeNode = (AnalysisNode) movementTree.getChildren().get(0).getValue().getProperties().get("rightTreeNode");
                            movementTree.getValue().addProperty("rightTreeNode", rightTreeNode);   //右树节点
                            movementTree.getValue().addProperty("name", name);  //索引名
                        }
                    },
                    new GenerateProduction(CFGProduction.getCFGProductionFromCFGString("Var -> $$", pool)) {

                        @Override
                        public void doMovement(AnalysisNode movementTree, AnalysisNode analysisTree, ResultTuple4 resultCOMM) {
                            //Var这个左树节点中存储的是右树节点
                            movementTree.getValue().addProperty("rightTreeNode", analysisTree);
                        }
                    },
                    new GenerateProduction(CFGProduction.getCFGProductionFromCFGString("Var -> $ num", pool)) {

                        @Override
                        public void doMovement(AnalysisNode movementTree, AnalysisNode analysisTree, ResultTuple4 resultCOMM) throws PLDLParsingException {
                            int num = Integer.parseInt((String) movementTree.getChildren().get(1).getValue().getProperties().get("val"));
                            --num;
                            if (num < 0 || num >= analysisTree.getChildren().size()) {
                                throw new PLDLParsingException("$后面的数字超出这条产生式右部元素的范围", null);
                            }
                            AnalysisNode rightTreeNode = analysisTree.getChildren().get(num);
                            movementTree.getValue().addProperty("rightTreeNode", rightTreeNode);
                        }
                    },
                    new GenerateProduction(CFGProduction.getCFGProductionFromCFGString("Var -> val", pool)) {

                        @Override
                        public void doMovement(AnalysisNode movementTree, AnalysisNode analysisTree, ResultTuple4 resultCOMM) {
                            String varname = (String) movementTree.getChildren().get(0).getValue().getProperties().get("val");
                            AnalysisNode rightTreeNode;
                            if (!analysisTree.getValue().getProperties().containsKey("var_" + varname)) {
                                rightTreeNode = new AnalysisNode(new Terminator(null));
                                rightTreeNode.getValue().addProperty("val", "var_" + varname);
                                analysisTree.getValue().getProperties().put("var_" + varname, rightTreeNode);
                            } else {
                                rightTreeNode = (AnalysisNode) analysisTree.getValue().getProperties().get("var_" + varname);
                            }
                            movementTree.getValue().addProperty("rightTreeNode", rightTreeNode);
                        }
                    },
                    new GenerateProduction(CFGProduction.getCFGProductionFromCFGString("L -> H", pool)) {

                        @Override
                        public void doMovement(AnalysisNode movementTree, AnalysisNode analysisTree, ResultTuple4 resultCOMM) {
                            movementTree.getValue().setProperties(new HashMap<>());
                            AnalysisNode HrightTreeNode = (AnalysisNode) movementTree.getChildren().get(0).getValue().getProperties().get("rightTreeNode");
                            String Hname = (String) movementTree.getChildren().get(0).getValue().getProperties().get("name");
                            movementTree.getValue().getProperties().put("val", HrightTreeNode.getValue().getProperties().get(Hname));
                        }
                    },
                    new GenerateProduction(CFGProduction.getCFGProductionFromCFGString("L_ -> L", pool)) {

                        @Override
                        public void doMovement(AnalysisNode movementTree, AnalysisNode analysisTree, ResultTuple4 resultCOMM) {
                            movementTree.getValue().addProperty("val", movementTree.getChildren().get(0).getValue().getProperties().get("val"));
                        }
                    },
                    new GenerateProduction(CFGProduction.getCFGProductionFromCFGString("L_ -> _", pool)) {

                        @Override
                        public void doMovement(AnalysisNode movementTree, AnalysisNode analysisTree, ResultTuple4 resultCOMM) {
                            movementTree.getValue().addProperty("val", "_");
                        }
                    },
                    new GenerateProduction(CFGProduction.getCFGProductionFromCFGString("L_ -> num", pool)) {

                        @Override
                        public void doMovement(AnalysisNode movementTree, AnalysisNode analysisTree, ResultTuple4 resultCOMM) {
                            movementTree.getValue().addProperty("val", movementTree.getChildren().get(0).getValue().getProperties().get("val"));
                        }
                    },
                    new GenerateProduction(CFGProduction.getCFGProductionFromCFGString("L_ -> val", pool)) {

                        @Override
                        public void doMovement(AnalysisNode movementTree, AnalysisNode analysisTree, ResultTuple4 resultCOMM) {
                            movementTree.getValue().addProperty("val", movementTree.getChildren().get(0).getValue().getProperties().get("val"));
                        }
                    }
            ));
            cfg = new CFG(pool, res, "E");
        } catch (PLDLParsingException e) {
            e.printStackTrace();
        }

    }

    public void doTreesMovements(AnalysisTree analysisTree, ResultTuple4 resultCOMM) throws PLDLParsingException, PLDLAnalysisException {
        rr_doTreesMovements(analysisTree.getRoot(), resultCOMM);
    }

    private void rr_doTreesMovements(AnalysisNode analysisNode, ResultTuple4 resultCOMM) throws PLDLParsingException, PLDLAnalysisException {
        doTreeMovement(beforeMovementsMap.get(analysisNode.getProduction()), analysisNode, resultCOMM);
        if (analysisNode.getChildren() != null) {
            for (AnalysisNode childNode : analysisNode.getChildren()) {
                if (childNode.getValue().getAbstractSymbol().getType() != AbstractSymbol.TERMINATOR) {
                    rr_doTreesMovements(childNode, resultCOMM);
                }
            }
        }
        doTreeMovement(afterMovementsMap.get(analysisNode.getProduction()), analysisNode, resultCOMM);
    }

    private void doTreeMovement(List<AnalysisTree> movementTrees, AnalysisNode analysisNode, ResultTuple4 resultCOMM) throws PLDLParsingException, PLDLAnalysisException {
        for (AnalysisTree movementTree : movementTrees) {
            rr_doTreeMovement(movementTree.getRoot(), analysisNode, resultCOMM);
        }
    }

    private void rr_doTreeMovement(AnalysisNode movementNode, AnalysisNode analysisNode, ResultTuple4 resultCOMM) throws PLDLParsingException, PLDLAnalysisException {
        if (movementNode.getChildren() != null) {
            for (AnalysisNode childNode : movementNode.getChildren()) {
                if (childNode.getValue().getAbstractSymbol().getType() != AbstractSymbol.TERMINATOR) {
                    rr_doTreeMovement(childNode, analysisNode, resultCOMM);
                }
            }
        }
        GenerateProduction GenerateProduction = (GenerateProduction) movementNode.getProduction();
        GenerateProduction.doMovement(movementNode, analysisNode, resultCOMM);
    }

    public void addToMovementsMap(CFGProduction production,
                                  List<AnalysisTree> beforeTrees,
                                  List<AnalysisTree> afterTrees) {
        beforeMovementsMap.put(production, beforeTrees);
        afterMovementsMap.put(production, afterTrees);
    }
}
