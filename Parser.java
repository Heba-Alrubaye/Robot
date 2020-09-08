import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.*;
import javax.swing.JFileChooser;

/**
 * The parser and interpreter. The top level parse function, a main method for
 * testing, and several utility methods are provided. You need to implement
 * parseProgram and all the rest of the parser.
 */
public class Parser {

	static HashMap<String, ASSGNNode> vars = new HashMap<String, ASSGNNode>();

	/**
	 * Top level parse method, called by the World
	 */
	static RobotProgramNode parseFile(File code) {
		Scanner scan = null;
		try {
			scan = new Scanner(code);

			// the only time tokens can be next to each other is
			// when one of them is one of (){},;
			scan.useDelimiter("\\s+|(?=[{}(),;])|(?<=[{}(),;])");

			RobotProgramNode n = parseProgram(scan); // You need to implement this!!!

			scan.close();
			return n;
		} catch (FileNotFoundException e) {
			System.out.println("Robot program source file not found");
		} catch (ParserFailureException e) {
			System.out.println("Parser error:");
			System.out.println(e.getMessage());
			scan.close();
		}
		return null;
	}

	/** For testing the parser without requiring the world */

	public static void main(String[] args) {
		if (args.length > 0) {
			for (String arg : args) {
				File f = new File(arg);
				if (f.exists()) {
					System.out.println("Parsing '" + f + "'");
					RobotProgramNode prog = parseFile(f);
					System.out.println("Parsing completed ");
					if (prog != null) {
						System.out.println("================\nProgram:");
						System.out.println(prog);
					}
					System.out.println("=================");
				} else {
					System.out.println("Can't find file '" + f + "'");
				}
			}
		} else {
			while (true) {
				JFileChooser chooser = new JFileChooser(".");// System.getProperty("user.dir"));
				int res = chooser.showOpenDialog(null);
				if (res != JFileChooser.APPROVE_OPTION) {
					break;
				}
				RobotProgramNode prog = parseFile(chooser.getSelectedFile());
				System.out.println("Parsing completed");
				if (prog != null) {
					System.out.println("Program: \n" + prog);
				}
				System.out.println("=================");
			}
		}
		System.out.println("Done");
	}

	// Useful Patterns

	static Pattern NUMPAT = Pattern.compile("-?\\d+"); // ("-?(0|[1-9][0-9]*)");
	static Pattern OPENPAREN = Pattern.compile("\\(");
	static Pattern CLOSEPAREN = Pattern.compile("\\)");
	static Pattern OPENBRACE = Pattern.compile("\\{");
	static Pattern CLOSEBRACE = Pattern.compile("\\}");

	/**
	 * PROG ::= STMT+
	 */
	// THE PARSER GOES HERE
	static RobotProgramNode parseProgram(Scanner s) {
		// If the file is empty then throw exception
		if (!s.hasNext()) {
			fail("Parse failed!", s);
		}

		ProgNode node = new ProgNode();

		while (s.hasNext()) {
			node.addNode(parseSTMT(s));
		}
		return node;
	}

	static RobotProgramNode parseSTMT(Scanner s) {
		//Act
		if (s.hasNext("move") || s.hasNext("turnL") || s.hasNext("turnR") || s.hasNext("takeFuel") || s.hasNext("wait")
				|| s.hasNext("turnAround") || s.hasNext("shieldOn") || s.hasNext("shieldOff")) {
			StmtNode node = new StmtNode(parseACT(s));
			if (s.hasNext(";")) {
				s.next(";");
				return node;
			}
			fail("';' Missing", s);
			//Loop
		} else if (s.hasNext("loop")) {
			StmtNode node = new StmtNode(parseLOOP(s));
			return node;
			//if
		} else if (s.hasNext("if")) {
			StmtNode node = new StmtNode(parseIf(s));
			return node;
			//while
		} else if (s.hasNext("while")) {
			StmtNode node = new StmtNode(parseWHILE(s));
			return node;
			//Assgn
		} else if (s.hasNext("\\$[A-Za-z][A-Za-z0-9]*")) {
			StmtNode node = new StmtNode(parseASSGN(s));
			if (s.hasNext(";")) {
				s.next(";");
				return node;
			}
			fail("';' Missing", s);
		}
		fail("Invalid statement", s);
		return null;
	}

	static RobotProgramNode parseACT(Scanner s) {
		if (s.hasNext("move")) {
			s.next("move");
			if (s.hasNext(OPENPAREN)) {//stage2
				s.next(OPENPAREN);
				RobotProgramNode node = new MoveNode(parseEXP(s));
				if (s.hasNext(CLOSEPAREN)) {
					s.next(CLOSEPAREN);
					return node;
				}
				fail("No close parenthesis found after expNode", s);
			}
			return new MoveNode();
		}
		if (s.hasNext("turnL")) {
			s.next("turnL");
			return new TurnLNode();
		}
		if (s.hasNext("turnR")) {
			s.next("turnR");
			return new TurnRNode();
		}
		if (s.hasNext("turnAround")) {
			s.next("turnAround");
			return new TurnAroundNode();
		}
		
		if (s.hasNext("shieldOn")) {
			s.next("shieldOn");
			return new ShieldOnNode();
		}
		if (s.hasNext("shieldOff")) {
			s.next("shieldOff");
			return new ShieldOffNode();
		}
		if (s.hasNext("takeFuel")) {
			s.next("takeFuel");
			return new TakeFuelNode();
		}
		if (s.hasNext("wait")) {
			s.next("wait");

			if (s.hasNext(OPENPAREN)) {//stage2
				s.next(OPENPAREN);
				RobotProgramNode node = new WaitNode(parseEXP(s));

				if (s.hasNext(CLOSEPAREN)) {
					s.next(CLOSEPAREN);
					return node;
				}
				fail("No close parenthesis found after expNode", s);
			}

			return new WaitNode();
		}
		fail("Invalid Action", s);
		return null;
	}

	static RobotProgramNode parseLOOP(Scanner s) {
		if (s.hasNext("loop")) {
			s.next("loop");
			return new LoopNode(parseBLOCK(s));
		}
		fail("'loop' statement not found", s);
		return null;
	}

	static RobotProgramNode parseIf(Scanner s) {
		if (s.hasNext("if")) {
			s.next("if");

			if (s.hasNext(OPENPAREN)) {
				s.next(OPENPAREN);

				RobotProgramNode ifNode = new IfNode(parseCOND(s));

				if (s.hasNext(CLOSEPAREN)) {
					s.next(CLOSEPAREN);

					IfNode IfNode = (IfNode) ifNode;
					IfNode.setBlock((BlockNode) parseBLOCK(s));

					while (s.hasNext("elif")) {
						IfNode.addElif((IfNode) parseIf(s));
					}

					if (s.hasNext("else")) {
						s.next("else");
						IfNode.setElseBlock((BlockNode) parseBLOCK(s));
					}
					return ifNode;
				} else {
					fail("No close parenthesis found after conditional", s);
				}
			} else {
				fail("No open parenthesis found before conditional", s);
			}
		} else if (s.hasNext("elif")) {
			s.next("elif");

			if (s.hasNext(OPENPAREN)) {
				s.next(OPENPAREN);

				RobotProgramNode ifNode = new IfNode(parseCOND(s));

				if (s.hasNext(CLOSEPAREN)) {
					s.next(CLOSEPAREN);

					IfNode IfNode = (IfNode) ifNode;
					IfNode.setBlock((BlockNode) parseBLOCK(s));

					while (s.hasNext("elif")) {
						IfNode.addElif((IfNode) parseIf(s));
					}

					if (s.hasNext("else")) {
						s.next("else");
						IfNode.setElseBlock((BlockNode) parseBLOCK(s));
					}
					return ifNode;
				} else {
					fail("No close parenthesis found after conditional", s);
				}
			} else {
				fail("No open parenthesis found before conditional", s);
			}
		} else {
			fail("'if' statement not found ", s);
		}
		return null;
	}

	static RobotProgramNode parseWHILE(Scanner s) {
		if (s.hasNext("while")) {
			s.next("while");
			if (s.hasNext(OPENPAREN)) {
				s.next(OPENPAREN);

				RobotProgramNode whileNode = new WhileNode(parseCOND(s));

				if (s.hasNext(CLOSEPAREN)) {
					s.next(CLOSEPAREN);
					WhileNode WhileNode = (WhileNode) whileNode;
					WhileNode.setBlock((BlockNode) parseBLOCK(s));
					return whileNode;
				}
				fail("No close parenthesis found after conditioanl", s);
			}
			fail("No open parenthesis found before conditional", s);
		}
		fail("no 'while' found", s);

		return null;
	}

	static RobotProgramNode parseASSGN(Scanner s) {
		if (s.hasNext("\\$[A-Za-z][A-Za-z0-9]*")) {
			String variableName = s.next("\\$[A-Za-z][A-Za-z0-9]*");

			if (s.hasNext("=")) {
				s.next("=");
				EXPNode expNode = parseEXP(s);
				// Put assignment of variable in map, to check for declaration later
				ASSGNNode assn = new ASSGNNode(variableName, expNode);
				vars.put(variableName, assn);
				return assn;
			}
			fail("'=' not found after variable name", s);
		}
		fail("Invalid variable name", s);

		return null;
	}

	static RobotProgramNode parseBLOCK(Scanner s) {
		if (!s.hasNext(OPENBRACE)) {
			fail("No open brace found", s);
		}
		s.next(OPENBRACE);

		BlockNode node = new BlockNode();
		while (!s.hasNext(CLOSEBRACE) && isSTMT(s)) {
			node.addNode(parseSTMT(s));
		}

		if (node.getSize() == 0) {
			fail("No 'block' found inside loop", s);
		}

		if (!s.hasNext(CLOSEBRACE)) {
			fail("No close brace found", s);
		}
		s.next(CLOSEBRACE);

		return node;

	}

	static EXPNode parseEXP(Scanner s) {
		if (s.hasNext("-?[1-9][0-9]*|0")) {
			return new NumNode(s.nextInt());
		} else if (isSEN(s)) {
			return parseSEN(s);
		} else if (s.hasNext("add") || s.hasNext("sub") || s.hasNext("mul") || s.hasNext("div")) {
			return parseOP(s);
		} else if (s.hasNext("\\$[A-Za-z][A-Za-z0-9]*")) {
			String variableName = s.next("\\$[A-Za-z][A-Za-z0-9]*");

			// Check if declared in map - Stage 4
			if (vars.containsKey(variableName)) {
				return vars.get(variableName).getExpression();
			} else {
				fail("Variables must be declared before they are used in the program", s);
			}
		}

		fail("No valid EXP Node found", s);
		return null;
	}

	static SENNode parseSEN(Scanner s) {
		if (s.hasNext("fuelLeft")) {
			s.next("fuelLeft");
			SENNode node = new FuelLeftNode();
			return node;
		}

		if (s.hasNext("oppLR")) {
			s.next("oppLR");
			SENNode node = new OppLRNode();
			return node;
		}

		if (s.hasNext("oppFB")) {
			s.next("oppFB");
			SENNode node = new OppFBNode();
			return node;
		}

		if (s.hasNext("numBarrels")) {
			s.next("numBarrels");
			SENNode node = new NumBarrelsNode();
			return node;
		}

		if (s.hasNext("barrelLR")) {
			s.next("barrelLR");

			if (s.hasNext(OPENPAREN)) {
				s.next(OPENPAREN);

				EXPNode EXP = parseEXP(s);
				if (s.hasNext(CLOSEPAREN)) {
					s.next(CLOSEPAREN);
					return new BarrelLRNode(EXP);
				}
				fail("Missing close parenthesis after optional argument", s);
			}

			SENNode node = new BarrelLRNode();
			return node;
		}

		if (s.hasNext("barrelFB")) {
			s.next("barrelFB");

			if (s.hasNext(OPENPAREN)) {
				s.next(OPENPAREN);

				EXPNode EXP = parseEXP(s);
				if (s.hasNext(CLOSEPAREN)) {
					s.next(CLOSEPAREN);
					return new BarrelFBNode(EXP);
				}
				fail("Missing close parenthesis after optional argument", s);
			}

			SENNode node = new BarrelFBNode();
			return node;
		}
		if (s.hasNext("wallDist")) {
			s.next("wallDist");
			SENNode node = new WallDistNode();
			return node;
		}

		fail("Invalid SEN argument with"+s.next(), s);

		return null;
	}

	static EXPNode parseOP(Scanner s) {
		if (s.hasNext("add")) {
			s.next("add");
			if (s.hasNext(OPENPAREN)) {
				s.next(OPENPAREN);
				EXPNode EXP1 = parseEXP(s);
				if (s.hasNext(",")) {
					s.next(",");
					EXPNode EXP2 = parseEXP(s);
					if (s.hasNext(CLOSEPAREN)) {
						s.next(CLOSEPAREN);
						return new AddNode(EXP1, EXP2);
					}
					fail("Missing closing parenthesis after EXP", s);
				}
				fail("Missing ','", s);
			}
			fail("Missing open parenthesis", s);
		}

		if (s.hasNext("sub")) {
			s.next("sub");
			if (s.hasNext(OPENPAREN)) {
				s.next(OPENPAREN);
				EXPNode EXP1 = parseEXP(s);
				if (s.hasNext(",")) {
					s.next(",");
					EXPNode EXP2 = parseEXP(s);
					if (s.hasNext(CLOSEPAREN)) {
						s.next(CLOSEPAREN);
						return new SubNode(EXP1, EXP2);
					}
					fail("Missing closing parenthesis after EXP", s);
				}
				fail("Missing ','", s);
			}
			fail("Missing open parenthesis", s);
		}

		if (s.hasNext("mul")) {
			s.next("mul");
			if (s.hasNext(OPENPAREN)) {
				s.next(OPENPAREN);
				EXPNode EXP1 = parseEXP(s);
				if (s.hasNext(",")) {
					s.next(",");
					EXPNode EXP2 = parseEXP(s);
					if (s.hasNext(CLOSEPAREN)) {
						s.next(CLOSEPAREN);
						return new MulNode(EXP1, EXP2);
					}
					fail("Missing closing parenthesis after EXP", s);
				}
				fail("Missing ','", s);
			}
			fail("Missing open parenthesis", s);
		}

		if (s.hasNext("div")) {
			s.next("div");
			if (s.hasNext(OPENPAREN)) {
				s.next(OPENPAREN);
				EXPNode EXP1 = parseEXP(s);
				if (s.hasNext(",")) {
					s.next(",");
					EXPNode EXP2 = parseEXP(s);
					if (s.hasNext(CLOSEPAREN)) {
						s.next(CLOSEPAREN);
						return new DivNode(EXP1, EXP2);
					}
					fail("Missing closing parenthesis after EXP", s);
				}
				fail("Missing ','", s);
			}
			fail("Missing open parenthesis", s);
		}
		return null;
	}

	static CONDNode parseCOND(Scanner s) {
		//RELOP
		if (s.hasNext("lt")) {
			s.next("lt");

			if (s.hasNext(OPENPAREN)) {
				s.next(OPENPAREN);

				EXPNode e1 = parseEXP(s);

				if (s.hasNext(",")) {
					s.next(",");

					EXPNode e2 = parseEXP(s);

					if (s.hasNext(CLOSEPAREN)) {
						s.next(CLOSEPAREN);
						CONDNode node = new LessThanNode(e1, e2);
						return node;
					} else {
						fail("')' not found", s);
					}
				} else {
					fail("',' not found", s);
				}
			} else {
				fail("No '(' found", s);
			}
		}

		if (s.hasNext("gt")) {
			s.next("gt");

			if (s.hasNext(OPENPAREN)) {
				s.next(OPENPAREN);

				EXPNode e1 = parseEXP(s);

				if (s.hasNext(",")) {
					s.next(",");

					EXPNode e2 = parseEXP(s);
					if (s.hasNext(CLOSEPAREN)) {
						s.next(CLOSEPAREN);
						CONDNode node = new GreaterThanNode(e1, e2);
						return node;
					} else {
						fail("')' not found", s);
					}
				} else {
					fail("',' not found", s);
				}
			} else {
				fail("'(' not found ", s);
			}
		}

		if (s.hasNext("eq")) {
			s.next("eq");

			if (s.hasNext(OPENPAREN)) {
				s.next(OPENPAREN);

				EXPNode e1 = parseEXP(s);

				if (s.hasNext(",")) {
					s.next(",");

					EXPNode e2 = parseEXP(s);
					if (s.hasNext(CLOSEPAREN)) {
						s.next(CLOSEPAREN);
						CONDNode node = new EqualToNode(e1, e2);
						return node;
					} else {
						fail("')' not found", s);
					}
				} else {
					fail("',' not found", s);
				}
			} else {
				fail("'(' not found ", s);
			}
		}
//stage 2
		if (s.hasNext("and")) {
			s.next("and");

			if (s.hasNext(OPENPAREN)) {
				s.next(OPENPAREN);

				CONDNode n1 = new ConditionNode(parseCOND(s));

				if (s.hasNext(",")) {
					s.next(",");
					CONDNode n2 = new ConditionNode(parseCOND(s));

					if (s.hasNext(CLOSEPAREN)) {
						s.next(CLOSEPAREN);
						return new AndNode(n1, n2);
					}
					fail("close parenthesis not found", s);
				}
				fail("',' not found", s);
			}
			fail("'(' not found", s);
		}

		if (s.hasNext("or")) {
			s.next("or");

			if (s.hasNext(OPENPAREN)) {
				s.next(OPENPAREN);

				CONDNode n1 = new ConditionNode(parseCOND(s));

				if (s.hasNext(",")) {
					s.next(",");
					CONDNode n2 = new ConditionNode(parseCOND(s));

					if (s.hasNext(CLOSEPAREN)) {
						s.next(CLOSEPAREN);
						return new OrNode(n1, n2);
					}
					fail("close parenthesis not found", s);
				}
				fail("',' not found", s);
			}
			fail("'(' not found", s);
		}

		if (s.hasNext("not")) {
			s.next("not");

			if (s.hasNext(OPENPAREN)) {
				s.next(OPENPAREN);

				CONDNode n1 = new ConditionNode(parseCOND(s));

				if (s.hasNext(CLOSEPAREN)) {
					s.next(CLOSEPAREN);
					return new NotNode(n1);
				}
				fail("close parenthesis not found", s);
			}
			fail("'(' not found", s);
		}

		fail("Invalid condition EXP", s);
		return null;
	}

	static boolean isSTMT(Scanner s) {
		if (s.hasNext("move") || s.hasNext("turnL") || s.hasNext("turnR") || s.hasNext("takeFuel") || s.hasNext("wait")
				|| s.hasNext("turnAround") || s.hasNext("shieldOn") || s.hasNext("shieldOff") || s.hasNext("loop")
				|| s.hasNext("if") || s.hasNext("while") || s.hasNext("\\$[A-Za-z][A-Za-z0-9]*")) {
			return true;
		}
		fail("invalid statement or no close brace found", s);
		return false;
	}

	static boolean isSEN(Scanner s) {
		if (s.hasNext("fuelLeft") || s.hasNext("oppLR") || s.hasNext("oppFB") || s.hasNext("numBarrels")
				|| s.hasNext("barrelLR") || s.hasNext("barrelFB") || s.hasNext("wallDist")) {
			return true;
		}
		return false;
	}

	// utility methods for the parser

	/**
	 * Report a failure in the parser.
	 */
	static void fail(String message, Scanner s) {
		String msg = message + "\n   @ ...";
		for (int i = 0; i < 5 && s.hasNext(); i++) {
			msg += " " + s.next();
		}
		throw new ParserFailureException(msg + "...");
	}

	/**
	 * Requires that the next token matches a pattern if it matches, it consumes and
	 * returns the token, if not, it throws an exception with an error message
	 */
	static String require(String p, String message, Scanner s) {
		if (s.hasNext(p)) {
			return s.next();
		}
		fail(message, s);
		return null;
	}

	static String require(Pattern p, String message, Scanner s) {
		if (s.hasNext(p)) {
			return s.next();
		}
		fail(message, s);
		return null;
	}

	/**
	 * Requires that the next token matches a pattern (which should only match a
	 * number) if it matches, it consumes and returns the token as an integer if
	 * not, it throws an exception with an error message
	 */
	static int requireInt(String p, String message, Scanner s) {
		if (s.hasNext(p) && s.hasNextInt()) {
			return s.nextInt();
		}
		fail(message, s);
		return -1;
	}

	static int requireInt(Pattern p, String message, Scanner s) {
		if (s.hasNext(p) && s.hasNextInt()) {
			return s.nextInt();
		}
		fail(message, s);
		return -1;
	}

	/**
	 * Checks whether the next token in the scanner matches the specified pattern,
	 * if so, consumes the token and return true. Otherwise returns false without
	 * consuming anything.
	 */
	static boolean checkFor(String p, Scanner s) {
		if (s.hasNext(p)) {
			s.next();
			return true;
		} else {
			return false;
		}
	}

	static boolean checkFor(Pattern p, Scanner s) {
		if (s.hasNext(p)) {
			s.next();
			return true;
		} else {
			return false;
		}
	}

}


// You could add the node classes here, as long as they are not declared public (or private)

//***********************************Classes*****************************************
//PROG
class ProgNode implements RobotProgramNode {
	private ArrayList<RobotProgramNode> STMTNodes;

	public ProgNode() {
		this.STMTNodes = new ArrayList<RobotProgramNode>();
	}

	public void execute(Robot robot) {
		for (int i = 0; i < this.STMTNodes.size(); i++) {
			this.STMTNodes.get(i).execute(robot);
		}
	}

	public void addNode(RobotProgramNode n) {
		this.STMTNodes.add(n);
	}

	public String toString() {
		String string = "";
		for (int i = 0; i < this.STMTNodes.size(); i++) {
			string += this.STMTNodes.get(i).toString() + "\n";
		}
		return string;
	}
}
//STMT
class StmtNode implements RobotProgramNode {

	RobotProgramNode n;

	public StmtNode(RobotProgramNode node) {
		this.n = node;
	}

	public String toString() {
		return this.n.toString();
	}

	public void execute(Robot robot) {
		n.execute(robot);
	}
}
//LOOP
class LoopNode implements RobotProgramNode {

	private RobotProgramNode n;

	public LoopNode(RobotProgramNode blockNode) {
		this.n = blockNode;
	}

	public void execute(Robot robot) {
		while (true) {
			this.n.execute(robot);
		}
	}

	public String toString() {
		System.out.println("called");
		return "loop " + n.toString() + "end loop";
	}
}

//BLOCK 
class BlockNode implements RobotProgramNode {

	private ArrayList<RobotProgramNode> STMT;

	public BlockNode() {
		this.STMT = new ArrayList<RobotProgramNode>();
	}

	public void execute(Robot robot) {
		for (int i = 0; i < this.STMT.size(); i++) {
			this.STMT.get(i).execute(robot);
		}
	}

	public void addNode(RobotProgramNode node) {
		this.STMT.add(node);
	}

	public int getSize() {
		return this.STMT.size();
	}

	public String toString() {
		String str = "\n";

		for (RobotProgramNode r : this.STMT) {
			str += "\t" + r.toString() + "\n";
		}

		return str;
	}
}

//if
class IfNode implements RobotProgramNode {

	private CONDNode condition;
	private BlockNode block;
	private ArrayList<IfNode> elifList;
	private BlockNode elseBlock;

	public IfNode(CONDNode condNode) {
		this.condition = condNode;
	}

	public void setBlock(BlockNode block) {
		this.block = block;
	}

	public void setElseBlock(BlockNode elseBlock) {
		this.elseBlock = elseBlock;
	}

	public void addElif(IfNode node) {
		if (elifList == null) {
			this.elifList = new ArrayList<IfNode>();
		}

		this.elifList.add(node);
	}

	public void execute(Robot robot) {
		if (condition.evaluate(robot)) {
			block.execute(robot);
		} else {
			if (this.elifList != null) {
				for (IfNode n : this.elifList) {
					n.execute(robot);
					return;
				}
			}
			if (elseBlock != null) {
				elseBlock.execute(robot);
			}
		}
	}

	public String toString() {
		String str = "";

		str += "if (" + condition.toString() + ")" + this.block.toString() + "}";

		if (elifList != null) {
			for (IfNode n : this.elifList) {
				str += "elif" + n.toString();
			}
		}

		if (elseBlock != null) {
			str += "else " + this.elseBlock.toString() + "}";
		}

		return str;
	}
}
//while
class WhileNode implements RobotProgramNode {
	private CONDNode condition;
	private BlockNode block;

	public WhileNode(CONDNode condNode) {
		this.condition = condNode;
	}

	public void setBlock(BlockNode block) {
		this.block = block;
	}

	public void execute(Robot robot) {
		while (condition.evaluate(robot)) {
			block.execute(robot);
		}
	}

	public String toString() {
		return "while (" + condition.toString() + ")" + this.block.toString() + "end while";
	}
}

//**************************************** Action ACT *****************************************
// MOVE NODE
class MoveNode implements RobotProgramNode {
	private EXPNode expNode;
	private int count;

	public MoveNode() {

	}

	public MoveNode(EXPNode expNode) {
		this.expNode = expNode;
	}

	public void execute(Robot robot) {
		if (expNode == null) {
			robot.move();
		} else {
			this.count = expNode.evaluate(robot);
			for (int i = 0; i < count; i++) {
				robot.move();
			}
		}
	}

	public String toString() {
		if (expNode == null) {
			return "move";
		}
		return "move " + expNode.toString() + " number of times";
	}
}

// TURN LEFT NODE
class TurnLNode implements RobotProgramNode {

	
	public TurnLNode() {
	}

	public void execute(Robot robot) {
		robot.turnLeft();
	}

	public String toString() {
		return "turn left";
	}
}

// TURN RIGHT NODE
class TurnRNode implements RobotProgramNode {

	
	public TurnRNode() {
	}

	public void execute(Robot robot) {
		robot.turnRight();
	}

	public String toString() {
		return "turn right";
	}
}

// TURN AROUND NODE
class TurnAroundNode implements RobotProgramNode {

	public void execute(Robot robot) {
		robot.turnAround();
	}

	public String toString() {
		return "Turn Around";
	}
}

// SHIELD ON NODE
class ShieldOnNode implements RobotProgramNode {

	public void execute(Robot robot) {
		robot.setShield(true);
	}

	public String toString() {
		return "Shield On";
	}
}

// SHIELD OFF NODE
class ShieldOffNode implements RobotProgramNode {

	public void execute(Robot robot) {
		robot.setShield(false);
	}

	public String toString() {
		return "Shield Off";
	}
}

// TAKE FUEL NODE
class TakeFuelNode implements RobotProgramNode {

	public TakeFuelNode() {
	}

	public void execute(Robot robot) {
		robot.takeFuel();
	}

	public String toString() {
		return "take fuel";
	}
}

// WAIT NODE
class WaitNode implements RobotProgramNode {

	private EXPNode expNode;
	private int count;

	public WaitNode() {
	}

	public WaitNode(EXPNode exp) {
		this.expNode = exp;
	}

	public void execute(Robot robot) {
		if (this.expNode == null) {
			robot.idleWait();
		} else {
			this.count = this.expNode.evaluate(robot);
			for (int i = 0; i < count; i++) {
				robot.idleWait();
			}
		}
	}

	public String toString() {
		if (this.expNode == null) {
			return "wait";
		} else {
			return "wait 'expNode' number of times";
		}
	}
}

// ************************************** Condition COND *****************************************

class ConditionNode implements CONDNode {
	CONDNode n;

	public ConditionNode(CONDNode n) {
		this.n = n;
	}

	public boolean evaluate(Robot robot) {
		return n.evaluate(robot);
	}

	public String toString() {
		return n.toString();
	}
}

// AND NODE
class AndNode implements CONDNode {
	CONDNode n1;
	CONDNode n2;

	public AndNode(CONDNode n1, CONDNode n2) {
		this.n1 = n1;
		this.n2 = n2;
	}

	public boolean evaluate(Robot robot) {
		return (n1.evaluate(robot) && n2.evaluate(robot));
	}

	public String toString() {
		return n1.toString() + " AND " + n2.toString();
	}
}

// NOT NODE
class NotNode implements CONDNode {
	CONDNode n1;

	public NotNode(CONDNode n1) {
		this.n1 = n1;
	}

	public boolean evaluate(Robot robot) {
		return (!n1.evaluate(robot));
	}

	public String toString() {
		return "NOT " + n1.toString();
	}
}

// OR NODE
class OrNode implements CONDNode {
	CONDNode n1;
	CONDNode n2;

	public OrNode(CONDNode n1, CONDNode n2) {
		this.n1 = n1;
		this.n2 = n2;
	}

	public boolean evaluate(Robot robot) {
		return (n1.evaluate(robot) || n2.evaluate(robot));
	}

	public String toString() {
		return n1.toString() + " OR " + n2.toString();
	}
}

// GREATER THAN NODE
class GreaterThanNode implements CONDNode {
	private EXPNode e1;
	private EXPNode e2;

	public GreaterThanNode(EXPNode e1, EXPNode e2) {
		this.e1 = e1;
		this.e2 = e2;
	}

	public boolean evaluate(Robot robot) {
		if (e1.evaluate(robot) > e2.evaluate(robot)) {
			return true;
		}
		return false;
	}

	public String toString() {
		return "(" + e1.toString() + " > " + e2.toString() + ")";
	}
}

// EQUAL TO NODE
class EqualToNode implements CONDNode {

	private EXPNode e1;
	private EXPNode e2;

	public EqualToNode(EXPNode e1, EXPNode e2) {
		this.e1 = e1;
		this.e2 = e2;
	}

	public boolean evaluate(Robot robot) {
		if (e1.evaluate(robot) == e2.evaluate(robot)) {
			return true;
		}
		return false;
	}

	public String toString() {
		return "(" + e1.toString() + " == " + e2.toString() + ")";
	}
}

// LESS THAN NODE
class LessThanNode implements CONDNode {

	private EXPNode e1;
	private EXPNode e2;

	public LessThanNode(EXPNode e1, EXPNode e2) {
		this.e1 = e1;
		this.e2 = e2;
	}

	public boolean evaluate(Robot robot) {
		if (e1.evaluate(robot) < e2.evaluate(robot)) {
			return true;
		}
		return false;
	}

	public String toString() {
		return "(" + e1.toString() + " < " + e2.toString() + ")";
	}
}

// *******************************ASSGN ***************************************

class ASSGNNode implements RobotProgramNode {

	private String name;
	private EXPNode EXP;

	public ASSGNNode(String name, EXPNode EXP) {
		this.name = name;
		this.EXP = EXP;
	}
	
	public EXPNode getExpression() {
		return EXP;
	}

	public void setExpression(EXPNode EXP) {
		this.EXP = EXP;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void execute(Robot robot) {
		Parser.vars.put(this.name, this);
	}

	public String toString() {
		return name.toString() + " = " + EXP.toString();
	}
}


// *****************************************SEN ***********************************
// "fuelLeft" NODE
class FuelLeftNode implements SENNode, EXPNode {
	public int execute(Robot robot) {
		return robot.getFuel();
	}

	public int evaluate(Robot robot) {
		return execute(robot);
	}

	public String toString() {
		return "Fuel remaining";
	}
}

// "oppLR" NODE
class OppLRNode implements SENNode, EXPNode {

	public int execute(Robot robot) {
		return robot.getOpponentLR();
	}

	public int evaluate(Robot robot) {
		return execute(robot);
	}

	public String toString() {
		return "Opponents LR Position";

	}
}

// "oppFB" NODE
class OppFBNode implements SENNode, EXPNode {
	public int execute(Robot robot) {
		return robot.getOpponentFB();
	}

	public int evaluate(Robot robot) {
		return execute(robot);
	}

	public String toString() {
		return "Opponents FB Position";
	}
}

// "barrelFB" NODE
class BarrelFBNode implements SENNode, EXPNode {

	private EXPNode EXP;

	public BarrelFBNode() {

	}

	public BarrelFBNode(EXPNode EXP) {
		this.EXP = EXP;
	}

	public int execute(Robot robot) {
		return robot.getClosestBarrelFB();
	}

	public int evaluate(Robot robot) {
		if (EXP == null) {
			return robot.getClosestBarrelFB();
		} else {
			return robot.getBarrelFB(this.EXP.evaluate(robot));
		}
	}

	public String toString() {
		if (EXP == null) {
			return "FB distance to closest barrel";
		} else {
			return "FB distance to barrel no. 'argument'";
		}
	}
}

// "barrelLR" NODE
class BarrelLRNode implements SENNode, EXPNode {

	private EXPNode EXP;

	public BarrelLRNode() {

	}

	public BarrelLRNode(EXPNode EXP) {
		this.EXP = EXP;
	}

	public int execute(Robot robot) {
		if (EXP == null) {
			return robot.getClosestBarrelLR();
		} else {
			return robot.getBarrelLR(this.EXP.evaluate(robot));
		}
	}

	public int evaluate(Robot robot) {
		return execute(robot);
	}

	public String toString() {
		if (this.EXP == null) {
			return "LR distance to closest barrel";
		} else {
			return "LR distance to barrel no. 'argument'";
		}
	}

}

// "numBarrels" NODE
class NumBarrelsNode implements SENNode, EXPNode {
	public int execute(Robot robot) {
		return robot.numBarrels();
	}

	public int evaluate(Robot robot) {
		return execute(robot);
	}

	public String toString() {
		return "No. of Barrels Currently in the word";
	}
}

// "wallDist" NODE
class WallDistNode implements SENNode, EXPNode {
	public int execute(Robot robot) {
		return robot.getDistanceToWall();
	}

	public int evaluate(Robot robot) {
		return execute(robot);
	}

	public String toString() {
		return "Distance to wall";
	}
}

// *******************************OP NODES *************************************
//stage 2
// "+" add NODE
class AddNode implements EXPNode {

	private EXPNode e1;
	private EXPNode e2;

	public AddNode(EXPNode e1, EXPNode e2) {
		this.e1 = e1;
		this.e2 = e2;
	}

	public int evaluate(Robot robot) {
		return e1.evaluate(robot) + e2.evaluate(robot);
	}

	public String toString() {
		return "(" + e1.toString() + " PLUS " + e2.toString() + ")";
	}
}

// "-" sub NODE
class SubNode implements EXPNode {

	private EXPNode e1;
	private EXPNode e2;

	public SubNode(EXPNode e1, EXPNode e2) {
		this.e1 = e1;
		this.e2 = e2;
	}

	public int evaluate(Robot robot) {
		return e1.evaluate(robot) - e2.evaluate(robot);
	}

	public String toString() {
		return "(" + e1.toString() + " MINUS " + e2.toString() + ")";
	}
}

// "*" mul NODE
class MulNode implements EXPNode {
	private EXPNode e1;
	private EXPNode e2;

	public MulNode(EXPNode e1, EXPNode e2) {
		this.e1 = e1;
		this.e2 = e2;
	}

	public int evaluate(Robot robot) {
		return (e1.evaluate(robot) * e2.evaluate(robot));
	}

	public String toString() {
		return "(" + e1.toString() + " TIMES " + e2.toString() + ")";
	}
}

// "/" div NODE
class DivNode implements EXPNode {
	private EXPNode e1;
	private EXPNode e2;

	public DivNode(EXPNode e1, EXPNode e2) {
		this.e1 = e1;
		this.e2 = e2;
	}

	public int evaluate(Robot robot) {
		return (e1.evaluate(robot) / e2.evaluate(robot));
	}

	public String toString() {
		return "(" + e1.toString() + " DIVIDED BY " + e2.toString() + ")";
	}
}

// NUM NODE
class NumNode implements EXPNode {
	private int number;

	public NumNode(int num) {
		this.number = num;
	}

	public int evaluate() {
		return this.number;
	}

	public int evaluate(Robot robot) {
		return number;
	}

	public String toString() {
		return this.number + "";
	}
}

// VAR NODE
class VarNode implements EXPNode {
	private String name;

	public VarNode(String name) {
		this.name = name;
	}

	public int evaluate(Robot robot) {
		// Get the ASSGNNode from map using name, get EXPNode from ASSGNNode, evaluate EXPNode
		return Parser.vars.get(this.name).getExpression().evaluate(robot);
	}

	public String toString() {
		return this.name;
	}
}
