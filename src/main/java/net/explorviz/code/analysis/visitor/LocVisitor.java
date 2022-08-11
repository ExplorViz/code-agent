package net.explorviz.code.analysis.visitor;


import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;

/**
 * Collects the lines of code minus the comments.
 */
public class LocVisitor extends GenericVisitorAdapter<Integer, Void> {

  @Override
  public Integer visit(final ClassOrInterfaceDeclaration n, final Void x) {
    int result = -1;

    if (n.getRange().isPresent()) {

      int linesOfComments = 0;

      for (final Comment commentNode : n.getAllContainedComments()) {
        if (commentNode.getRange().isPresent()) {
          linesOfComments += commentNode.getRange().get().getLineCount();
        }
      }

      result = n.getRange().get().getLineCount() - linesOfComments;

    }

    return result;
  }

}


