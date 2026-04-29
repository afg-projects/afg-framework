package io.github.afgprojects.framework.data.sql.util;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;

import java.util.List;

/**
 * 列名提取器 - 从 SQL AST 中提取所有列名
 */
public class ColumnFinder extends StatementVisitorAdapter<Void> {

    private final List<String> columns;

    public ColumnFinder(List<String> columns) {
        this.columns = columns;
    }

    @Override
    public <S> Void visit(Select select, S context) {
        // 使用 instanceof 检查类型，避免 ClassCastException
        if (select instanceof PlainSelect plainSelect) {
            processPlainSelect(plainSelect);
        } else if (select instanceof SetOperationList setOpList) {
            processSetOperationList(setOpList);
        }
        return null;
    }

    @Override
    public <S> Void visit(Update update, S context) {
        // 处理 SET 子句中的列
        if (update.getUpdateSets() != null) {
            for (UpdateSet updateSet : update.getUpdateSets()) {
                // 处理列名
                if (updateSet.getColumns() != null) {
                    for (Column column : updateSet.getColumns()) {
                        addColumn(column);
                    }
                }
                // 处理值表达式
                if (updateSet.getValues() != null) {
                    for (Expression expr : updateSet.getValues()) {
                        processExpression(expr);
                    }
                }
            }
        }
        // 处理 WHERE
        if (update.getWhere() != null) {
            processExpression(update.getWhere());
        }
        return null;
    }

    private void processPlainSelect(PlainSelect plainSelect) {
        // 处理 SELECT 列
        if (plainSelect.getSelectItems() != null) {
            for (SelectItem<?> item : plainSelect.getSelectItems()) {
                processSelectItem(item);
            }
        }
        // 处理 WHERE
        if (plainSelect.getWhere() != null) {
            processExpression(plainSelect.getWhere());
        }
        // 处理 GROUP BY
        if (plainSelect.getGroupBy() != null && plainSelect.getGroupBy().getGroupByExpressionList() != null) {
            for (Object obj : plainSelect.getGroupBy().getGroupByExpressionList()) {
                if (obj instanceof Expression expr) {
                    processExpression(expr);
                }
            }
        }
        // 处理 HAVING
        if (plainSelect.getHaving() != null) {
            processExpression(plainSelect.getHaving());
        }
        // 处理 ORDER BY
        if (plainSelect.getOrderByElements() != null) {
            for (OrderByElement order : plainSelect.getOrderByElements()) {
                processExpression(order.getExpression());
            }
        }
        // 处理 JOIN
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                // 处理所有 ON 表达式
                if (join.getOnExpressions() != null) {
                    for (Expression onExpr : join.getOnExpressions()) {
                        processExpression(onExpr);
                    }
                }
            }
        }
    }

    private void processSetOperationList(SetOperationList setOpList) {
        if (setOpList.getSelects() != null) {
            for (Select select : setOpList.getSelects()) {
                // 使用 instanceof 检查类型，避免 ClassCastException
                if (select instanceof PlainSelect plainSelect) {
                    processPlainSelect(plainSelect);
                } else if (select instanceof SetOperationList nestedSetOp) {
                    processSetOperationList(nestedSetOp);
                }
            }
        }
    }

    private void processSelectItem(SelectItem<?> item) {
        Expression expr = item.getExpression();
        if (expr != null) {
            processExpression(expr);
        }
    }

    private void processExpression(Expression expr) {
        if (expr instanceof Column column) {
            addColumn(column);
        } else if (expr instanceof Function function) {
            if (function.getParameters() != null) {
                for (Expression param : function.getParameters()) {
                    processExpression(param);
                }
            }
        } else if (expr instanceof AndExpression andExpr) {
            if (andExpr.getLeftExpression() != null) {
                processExpression(andExpr.getLeftExpression());
            }
            if (andExpr.getRightExpression() != null) {
                processExpression(andExpr.getRightExpression());
            }
        } else if (expr instanceof OrExpression orExpr) {
            if (orExpr.getLeftExpression() != null) {
                processExpression(orExpr.getLeftExpression());
            }
            if (orExpr.getRightExpression() != null) {
                processExpression(orExpr.getRightExpression());
            }
        } else if (expr instanceof net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList<?> parenthesedList) {
            // 处理括号表达式列表
            for (Object innerExpr : parenthesedList) {
                if (innerExpr instanceof Expression e) {
                    processExpression(e);
                }
            }
        } else if (expr instanceof net.sf.jsqlparser.expression.operators.relational.EqualsTo equalsTo) {
            processExpression(equalsTo.getLeftExpression());
            processExpression(equalsTo.getRightExpression());
        } else if (expr instanceof net.sf.jsqlparser.expression.operators.relational.NotEqualsTo notEqualsTo) {
            processExpression(notEqualsTo.getLeftExpression());
            processExpression(notEqualsTo.getRightExpression());
        } else if (expr instanceof net.sf.jsqlparser.expression.operators.relational.GreaterThan greaterThan) {
            processExpression(greaterThan.getLeftExpression());
            processExpression(greaterThan.getRightExpression());
        } else if (expr instanceof net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals greaterThanEquals) {
            processExpression(greaterThanEquals.getLeftExpression());
            processExpression(greaterThanEquals.getRightExpression());
        } else if (expr instanceof net.sf.jsqlparser.expression.operators.relational.MinorThan minorThan) {
            processExpression(minorThan.getLeftExpression());
            processExpression(minorThan.getRightExpression());
        } else if (expr instanceof net.sf.jsqlparser.expression.operators.relational.MinorThanEquals minorThanEquals) {
            processExpression(minorThanEquals.getLeftExpression());
            processExpression(minorThanEquals.getRightExpression());
        } else if (expr instanceof net.sf.jsqlparser.expression.operators.relational.InExpression inExpr) {
            if (inExpr.getLeftExpression() != null) {
                processExpression(inExpr.getLeftExpression());
            }
        } else if (expr instanceof net.sf.jsqlparser.expression.operators.relational.Between between) {
            processExpression(between.getLeftExpression());
            processExpression(between.getBetweenExpressionStart());
            processExpression(between.getBetweenExpressionEnd());
        } else if (expr instanceof net.sf.jsqlparser.expression.operators.relational.LikeExpression likeExpr) {
            processExpression(likeExpr.getLeftExpression());
            processExpression(likeExpr.getRightExpression());
        } else if (expr instanceof net.sf.jsqlparser.expression.operators.relational.IsNullExpression isNullExpr) {
            processExpression(isNullExpr.getLeftExpression());
        } else if (expr instanceof net.sf.jsqlparser.expression.operators.arithmetic.Addition addition) {
            processExpression(addition.getLeftExpression());
            processExpression(addition.getRightExpression());
        } else if (expr instanceof net.sf.jsqlparser.expression.operators.arithmetic.Subtraction subtraction) {
            processExpression(subtraction.getLeftExpression());
            processExpression(subtraction.getRightExpression());
        } else if (expr instanceof net.sf.jsqlparser.expression.operators.arithmetic.Multiplication multiplication) {
            processExpression(multiplication.getLeftExpression());
            processExpression(multiplication.getRightExpression());
        } else if (expr instanceof net.sf.jsqlparser.expression.operators.arithmetic.Division division) {
            processExpression(division.getLeftExpression());
            processExpression(division.getRightExpression());
        } else if (expr instanceof net.sf.jsqlparser.expression.CaseExpression caseExpr) {
            if (caseExpr.getSwitchExpression() != null) {
                processExpression(caseExpr.getSwitchExpression());
            }
            if (caseExpr.getWhenClauses() != null) {
                for (net.sf.jsqlparser.expression.WhenClause when : caseExpr.getWhenClauses()) {
                    processExpression(when.getWhenExpression());
                    processExpression(when.getThenExpression());
                }
            }
            if (caseExpr.getElseExpression() != null) {
                processExpression(caseExpr.getElseExpression());
            }
        }
    }

    private void addColumn(Column column) {
        String columnName = column.getColumnName();
        if (!columns.contains(columnName)) {
            columns.add(columnName);
        }
    }
}