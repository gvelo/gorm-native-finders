package groovy.org.grails.plugin.nativefinders.transform;

import groovyjarjarasm.asm.Opcodes;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.CompileUnit;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

/**
 * Process all the app classes and tranform only calls to domains classes.
 * 
 * @author Gabriel Velo
 * 
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class NativeFinderTransform extends ClassCodeVisitorSupport implements ASTTransformation, Opcodes {

	private static final String DOMAIN_DIR = "domain";
	private static final String GRAILS_APP_DIR = "grails-app";
	private static final String FIND_METHOD_NAME = "find";
	private static final String FINDALL_METHOD_NAME = "findAll";

	public final String AST_CACHE_FIELD_NAME = "gorm_native_finder_ast_cache";

	private SourceUnit sourceUnit;
	private CompileUnit compileUnit;

	private List<Expression> closureASTBuilderExpressions;

	@Override
	public void visit(ASTNode[] nodes, SourceUnit sourceUnit) {

		this.sourceUnit = sourceUnit;
		this.compileUnit = sourceUnit.getAST().getUnit();
		ModuleNode module = sourceUnit.getAST();

		for (ClassNode classNode : module.getClasses()) {

			closureASTBuilderExpressions = new ArrayList<Expression>();

			// Visit and extract closure AST.
			visitClass( classNode );

			// add the ast builded to the cache.
			if (!closureASTBuilderExpressions.isEmpty()) {
				addAST( classNode );
			}
		}

	}

	@Override
	public void visitMethodCallExpression(MethodCallExpression call) {

		if (shouldTransform( call )) {

			ArgumentListExpression args = (ArgumentListExpression) call.getArguments();
			ClosureExpression closureExpression = (ClosureExpression) args.getExpression( 0 );
			ClosureTransformer transformer = new ClosureTransformer();
			transformer.transformClosureExpression( closureExpression );
			closureASTBuilderExpressions.add( transformer.getClosureASTBuilderExpression() );
			transformFinderArguments( call, transformer.getRuntimeEvaluatedParameters() );

		} else {
			super.visitMethodCallExpression( call );
		}

	}

	protected boolean shouldTransform(MethodCallExpression call) {

		if (( call.getObjectExpression() instanceof ClassExpression ) && ( call.getMethod() instanceof ConstantExpression )) {

			ClassNode cn = ( (ClassExpression) call.getObjectExpression() ).getType();

			String methodName = call.getMethodAsString();

			if (isDomainClass( cn ) && ( FIND_METHOD_NAME.equals( methodName ) || FINDALL_METHOD_NAME.equals( methodName ) )) {

				if (!( call.getArguments() instanceof ArgumentListExpression )) {
					return false;
				}

				ArgumentListExpression args = (ArgumentListExpression) call.getArguments();

				if (args.getExpressions().isEmpty()) {
					return false;
				}

				if (!( args.getExpression( 0 ) instanceof ClosureExpression )) {
					return false;
				}

				return true;
			}

		}

		return false;

	}

	/**
	 * tranform the finder arguments from find( ClosureExpression ) to find ( astCache[ index ] , parameters )
	 * 
	 * @param call
	 * @param parameterList
	 */
	private void transformFinderArguments(MethodCallExpression call, List<Expression> parameterList) {

		int astIndex = closureASTBuilderExpressions.size() - 1;

		// this is find { astCache[ index ] , parameters }
		BinaryExpression arrayAcess = new BinaryExpression( new VariableExpression( AST_CACHE_FIELD_NAME ), Token.newSymbol( Types.LEFT_SQUARE_BRACKET, 0, 0 ), new ConstantExpression( astIndex ) );

		ListExpression queryparameters = new ListExpression( parameterList );
		ArgumentListExpression arguments = new ArgumentListExpression();
		arguments.addExpression( arrayAcess );
		arguments.addExpression( queryparameters );
		call.setArguments( arguments );

	}

	/**
	 * attach the ast cache field to the class.
	 * 
	 * @param classNode
	 */
	private void addAST(ClassNode classNode) {

		ModuleNode module = sourceUnit.getAST();

		module.addStarImport( "org.codehaus.groovy.ast." );
		module.addStarImport( "groovy.org.grails.plugin.nativefinders.transform." );

		ListExpression astCacheInitialValue = new ListExpression( closureASTBuilderExpressions );
		classNode.addField( AST_CACHE_FIELD_NAME, ACC_PUBLIC | ACC_STATIC, ClassHelper.DYNAMIC_TYPE, astCacheInitialValue );
		return;

	}

	@Override
	protected SourceUnit getSourceUnit() {
		return sourceUnit;
	}

	protected boolean isDomainClass(ClassNode classNode) {

		ClassNode cn = compileUnit.getClass( classNode.getName() );

		if (cn == null) {
			return false;
		}

		String sourcePath = cn.getModule().getContext().getName();

		File sourceFile = new File( sourcePath );
		File parent = sourceFile.getParentFile();
		while (parent != null) {
			File parentParent = parent.getParentFile();
			if (parent.getName().equals( DOMAIN_DIR ) && parentParent != null && parentParent.getName().equals( GRAILS_APP_DIR )) {
				return true;
			}
			parent = parentParent;
		}

		return false;
	}

}
