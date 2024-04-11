package net.explorviz.code.analysis.visitor;

import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import net.explorviz.code.analysis.handler.FileDataHandler;

/**
 * Dummy Visitor that does nothing.
 */
public class EmptyGenericVisitor extends GenericVisitorAdapter<Integer, FileDataHandler> {

}
