package net.explorviz.code.analysis.visitor;

import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import net.explorviz.code.analysis.handler.FileDataHandler;

/**
 * Visitor filling a FileData object with metrics data. This Visitor is depending
 */
public class MetricsVisitor extends VoidVisitorAdapter<FileDataHandler> {

}
