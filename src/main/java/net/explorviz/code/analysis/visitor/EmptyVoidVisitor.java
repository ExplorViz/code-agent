package net.explorviz.code.analysis.visitor;

import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import net.explorviz.code.analysis.handler.FileDataHandler;

// This visitor does nothing. It is intended to fill in missing Visitors.
public class EmptyVoidVisitor extends VoidVisitorAdapter<FileDataHandler> {
}
